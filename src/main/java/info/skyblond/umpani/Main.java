package info.skyblond.umpani;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger("Application");

    public static void main(String[] args) {
        try {
            if (Env.DEBUG_MODE) {
                logger.info("DEBUG mode enabled");
            }
            long start = System.currentTimeMillis();

            OkHttpClient httpClient = Env.getHttpClient();
            DynamoDbClient dynamoDbClient = Env.getDynamoDbClient();
            String dynamoTableName = Env.DYNAMODB_TABLE_NAME;
            String targetSymbol = Env.TARGET_CRYPTO_SYMBOL;

            PriceFetcher fetcher = new PriceFetcher(httpClient, dynamoDbClient,
                    dynamoTableName, targetSymbol);
            List<String> symbols = Env.getCryptoSymbolList();
            logger.info("Start recording prices...");
            fetcher.record(symbols);

            logger.info("Done, shutdown resources...");
            dynamoDbClient.close();

            long end = System.currentTimeMillis();
            logger.info("Exit. Consumed {} ms", end - start);
        } catch (Throwable t) {
            logger.error("Uncaught throwable", t);
            throw new RuntimeException(t);
        }
    }
}
