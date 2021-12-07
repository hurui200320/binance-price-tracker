package info.skyblond.umpani;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class PriceFetcher {
    private final Logger logger = LoggerFactory.getLogger(PriceFetcher.class);
    private final Gson gson = new GsonBuilder().create();

    private final OkHttpClient httpClient;
    private final DynamoDbClient dynamoDbClient;
    private final String dynamoTableName;
    private final String targetSymbol;

    public PriceFetcher(OkHttpClient httpClient, DynamoDbClient dynamoDbClient, String dynamoTableName, String targetSymbol) {
        this.httpClient = httpClient;
        this.dynamoDbClient = dynamoDbClient;
        this.dynamoTableName = dynamoTableName;
        this.targetSymbol = targetSymbol;
    }

    private Map<String, String> getPriceMap() throws IOException {
        Map<String, String> result = new HashMap<>();
        Response resp = this.httpClient.newCall(new Request.Builder()
                .url(Env.getPriceApiUrl())
                .build()).execute();
        ResponseBody body = resp.body();
        if (resp.isSuccessful() && body != null) {
            BinancePriceResponse[] prices = this.gson.fromJson(body.string(), BinancePriceResponse[].class);
            for (BinancePriceResponse price : prices) {
                result.put(price.getSymbol(), price.getPrice());
            }
        }
        return result;
    }

    private List<String> resolvePath(LinkedList<String> path, String toSymbol, Map<String, String> priceMap) {
        String currentSymbol = path.getLast();
        if (priceMap.containsKey(currentSymbol + toSymbol)) {
            path.add(toSymbol);
            return path;
        }
        int currentPathLength = path.size();
        var option = priceMap.keySet().stream()
                // related pair
                .filter(it -> it.startsWith(currentSymbol))
                // get the symbol
                .map(it -> it.substring(currentSymbol.length()))
                // not a loop
                .filter(it -> !path.contains(it))
                .map(it -> {
                    var tempPath = new LinkedList<>(path.subList(0, currentPathLength));
                    tempPath.add(it);
                    return resolvePath(tempPath, toSymbol, priceMap);
                })
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(List::size));
        if (option.isEmpty()) {
            // not found
            return null;
        }
        return option.get();
    }

    private LinkedList<String> linkedListOfString(String str) {
        var result = new LinkedList<String>();
        result.add(str);
        return result;
    }

    private static Map<String, AttributeValue> transferToItem(Map<String, BigDecimal> resultMap) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis() / 1000)).build());
        resultMap.keySet()
                .stream()
                .sorted()
                .forEach(k -> result.put(k, AttributeValue.builder().n(resultMap.get(k).toPlainString()).build()));
        return result;
    }

    private void savePriceInfo(Map<String, BigDecimal> resultMap) {
        this.dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(dynamoTableName)
                .item(transferToItem(resultMap))
                .build());
    }

    public void record(List<String> symbols) throws IOException {
        Map<String, String> priceMap = getPriceMap();
        Map<String, BigDecimal> resultMap = new HashMap<>();

        for (String currentSymbol : symbols) {
            logger.info("Resolving token: {}", currentSymbol);
            List<String> path = this.resolvePath(linkedListOfString(currentSymbol),
                    targetSymbol, priceMap);
            logger.info("Resolved path: {}", path);
            if (path == null) {
                logger.warn("Path not found for {}", currentSymbol);
                continue;
            }
            String symbol = Objects.requireNonNull(path.remove(0));
            BigDecimal currentPrice = BigDecimal.ONE;
            while (!path.isEmpty()) {
                String pairPrice = priceMap.get(symbol + path.get(0));
                logger.info("{}: {}", symbol + path.get(0), pairPrice);
                currentPrice = currentPrice.multiply(new BigDecimal(pairPrice));
                symbol = path.remove(0);
            }
            logger.info("{} to {}: {}", currentSymbol, targetSymbol, currentPrice.toPlainString());
            resultMap.put(currentSymbol, currentPrice);
        }
        this.savePriceInfo(resultMap);
    }
}
