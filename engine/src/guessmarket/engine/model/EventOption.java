package guessmarket.engine.model;

/**
 * One possible outcome of an event, e.g. "YES".
 *
 * Deliberately dumb: an option knows its name and how many of its shares have
 * been bought so far, but NOT its own price. Under LMSR a price is a function
 * of every option's quantity together, so pricing belongs to LmsrMarket.
 */
public class EventOption {

    private final String name;
    private long sharesBought;

    public EventOption(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Option name cannot be empty");
        }
        this.name = name.trim();
        this.sharesBought = 0;
    }

    public String getName() {
        return name;
    }

    public long getSharesBought() {
        return sharesBought;
    }

    void addShares(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add a negative share amount");
        }
        sharesBought += amount;
    }
}
