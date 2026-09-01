package guessmarket.engine.model;

/** Which trading method an event uses. */
public enum EventType {
    LMSR("LMSR"),
    ORDER_BOOK("Order Book");

    private final String display;

    EventType(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
