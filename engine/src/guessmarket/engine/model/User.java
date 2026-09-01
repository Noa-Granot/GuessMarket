package guessmarket.engine.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A person acting in the system. Every user has a unique name and their own
 * account, and may be the market maker of any number of events.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Account account;
    private final Set<Integer> marketMakerFor = new LinkedHashSet<>();

    public User(String name, double initialCash) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A user must have a name");
        }
        this.name = name.trim();
        this.account = new Account(initialCash);
    }

    public String getName() {
        return name;
    }

    public Account getAccount() {
        return account;
    }

    public void addMarketMakerEvent(int eventId) {
        marketMakerFor.add(eventId);
    }

    public Set<Integer> getMarketMakerFor() {
        return Collections.unmodifiableSet(marketMakerFor);
    }

    public boolean isMarketMakerOf(int eventId) {
        return marketMakerFor.contains(eventId);
    }

    public boolean isMarketMaker() {
        return !marketMakerFor.isEmpty();
    }
}
