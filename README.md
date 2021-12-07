# binance-price-tracker

Fetch crypto prices from binance and save to AWS DynamoDB.

## How it works

It fetches the real prices of all available crypto pairs, and try to resolve a possible path from the given symbol to
target symbol to calculate the price. For example, a give symbol is `GAS`, and I want to get the price in `USDT`,
however there is no `GAS/USDT` pair on binance, so it will resolve this following path: `GAS -> BTC -> USDT`. Then the
price is calculated by `GASBTC * BTCUSDT`.

There are some limitations. First, the trading pair is not so clear in the name. Binance didn't use any spacing
character, so `GAS/BTC` is actually `GASBTC`, which the program cannot distinguish tokens clearly, thus it cannot
calculate the reverse price automatically. So saying you need some path like `... -> BTC -> GAS -> ...`, since the
program don't know `BTC/GAS`, it won't take that path. If this is the only path, then the program will fail to fetch the
price on this symbol.

The second limitation is the path searching algorithm, I used a simple DFS, which might not that efficient when dealing
with huge amount of symbols, but it's enough for my demand, so I won't bother to implement a more advanced algorithm.

## Env settings

All settings are set by environment variables. This is useful for docker containers.

### `DEBUG_MODE`

The default value is `false`. When set to `true`:

+ AWS region is set to US_EAST_1 (I'm using that region for testing)

### `PROXY_TYPE`

The default value is an empty string. The value can be following (case-sensitive):

+ `DIRECT`: Represents a direct connection.
+ `HTTP`: Represents proxy for high level protocols such as HTTP or FTP.
+ `SOCKS`: Represents a SOCKS (V4 or V5) proxy.

This has to be combined with `PROXY_HOST` and `PROXY_PROT`. If any of them are blank, the proxy won't be set.

### `PROXY_HOST`

The default value is an empty string. The value should be your proxy server's hostname.

This has to be combined with `PROXY_TYPE` and `PROXY_PROT`. If any of them are blank, the proxy won't be set.

### `PROXY_PORT`

The default value is an empty string. The value should be your proxy server's port.

This has to be combined with `PROXY_TYPE` and `PROXY_HOST`. If any of them are blank, the proxy won't be set.

### `BINANCE_API_URL`

The default value is `https://api.binance.com`. This is the basic url for binance api. You can use those alternative
endpoints:

+ `https://api1.binance.com`
+ `https://api2.binance.com`
+ `https://api3.binance.com`

### `TARGET_CRYPTO`

The default value is `USDT`, this decides the price representation token.

### `DYNAMODB_TABLE_NAME`

This is required. This is the name of the AWS DynamoDB table, the hash key of this table must be `timestamp` (number).

### `CRYPTOS`

This is required. This is the list of crypto symbols you want to fetch, seperated by comma.

For example: `FLM,NEO,GAS,ETH,BTC`.
