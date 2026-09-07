package guessmarket.engine.orderbook;

/** Which way an order goes. */
public enum OrderSide {
    BUY("Buy"),
    SELL("Sell");

    private final String display;

    OrderSide(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
