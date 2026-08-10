package guessmarket.engine.model;

/**
 * Lifecycle of an event. Exercise 1 only has two states: an event is ACTIVE
 * from the moment it is loaded, and becomes CLOSED once it is decided.
 * Exercise 2 adds a NOT_STARTED state before ACTIVE.
 */
public enum EventStatus {
    ACTIVE("Active"),
    CLOSED("Closed");

    private final String display;

    EventStatus(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
