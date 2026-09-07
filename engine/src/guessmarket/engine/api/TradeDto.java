package guessmarket.engine.api;

/** One completed trade. A mint has no seller, so sellerName is null. */
public record TradeDto(int serial,
                       String kindDisplay,
                       String buyerName,
                       String sellerName,
                       String optionName,
                       long quantity,
                       double price,
                       double commission) {

    public double value() {
        return quantity * price;
    }
}
