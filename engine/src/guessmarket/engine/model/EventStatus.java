package guessmarket.engine.model;

/** Whether an event can be traded. Events start not started until their MM opens them. */
public enum EventStatus {
    NOT_STARTED("Not started"),
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
