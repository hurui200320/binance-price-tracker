package info.skyblond.umpani;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Env {
    private static final Logger logger = LoggerFactory.getLogger(Env.class);

    public static final boolean DEBUG_MODE =
            Boolean.parseBoolean(readFromEnv("DEBUG_MODE", "false"));

    public static final String PROXY_TYPE = readFromEnv("PROXY_TYPE", "");
    public static final String PROXY_HOST = readFromEnv("PROXY_HOST", "");
    public static final String PROXY_PORT = readFromEnv("PROXY_PORT", "");

    public static final String BINANCE_BASE_URL = readFromEnv("BINANCE_API_URL", "https://api.binance.com");
    public static final String TARGET_CRYPTO_SYMBOL = readFromEnv("TARGET_CRYPTO", "USDT");
    public static final String DYNAMODB_TABLE_NAME = readFromEnv("DYNAMODB_TABLE_NAME");
    public static final String CRYPTO_COMMA_LIST = readFromEnv("CRYPTOS");

    public static Proxy getProxy() {
        if (PROXY_TYPE.isBlank() || PROXY_HOST.isBlank() || PROXY_PORT.isBlank()) {
            return null;
        }
        try {
            return new Proxy(
                    Proxy.Type.valueOf(PROXY_TYPE),
                    new InetSocketAddress(PROXY_HOST, Integer.parseInt(PROXY_PORT.trim()))
            );
        } catch (Throwable t) {
            logger.error("Error when parsing proxy settings", t);
            return null;
        }
    }

    public static String getPriceApiUrl() {
        String url;
        if (BINANCE_BASE_URL.endsWith("/")) {
            // skip the last '/'
            url = BINANCE_BASE_URL.substring(0, BINANCE_BASE_URL.length() - 1);
        } else {
            url = BINANCE_BASE_URL;
        }
        // get real time price
        return url + "/api/v3/ticker/price";
    }

    public static List<String> getCryptoSymbolList() {
        return Arrays.stream(CRYPTO_COMMA_LIST.split(","))
                .map(it -> it.trim().toUpperCase())
                .collect(Collectors.toUnmodifiableList());
    }

    public static OkHttpClient getHttpClient() {
        var builder = new OkHttpClient.Builder();
        Proxy proxy = getProxy();
        if (proxy != null) {
            builder = builder.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 1080)));
        }
        return builder.build();
    }

    public static DynamoDbClient getDynamoDbClient() {
        var builder = DynamoDbClient.builder();
        if (DEBUG_MODE) {
            // manually set the region
            builder = builder.region(Region.US_EAST_1);
        }
        return builder.build();
    }

    private static String readFromEnv(String key) {
        String result = readFromEnv(key, null);
        if (result == null) {
            throw new RuntimeException("Env key is required but not found: " + key);
        }
        return result;
    }

    private static String readFromEnv(String key, String defaultValue) {
        String result = System.getenv(key);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }
}
