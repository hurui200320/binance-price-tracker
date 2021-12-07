package info.skyblond.umpani;

import com.google.gson.annotations.SerializedName;

public class BinancePriceResponse {
    @SerializedName("symbol")
    private String symbol;

    @SerializedName("price")
    private String price;

    public String getSymbol() {
        return this.symbol;
    }

    public String getPrice() {
        return this.price;
    }
}
