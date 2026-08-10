package guessmarket.engine.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything currently loaded in the system: the events, in file order, plus the
 * events manager's account.
 *
 * This whole object is what a file load replaces. The loader builds a fresh
 * MarketSystem, validates it completely, and only then does the engine swap it
 * in -- which is how "a broken file must not overwrite the good one already
 * loaded" is enforced, without any rollback logic.
 */
public class MarketSystem {

    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();
    private final Account managerAccount = new Account();

    public void addEvent(Event event) {
        if (eventsById.containsKey(event.getId())) {
            throw new IllegalArgumentException("Duplicate event id: " + event.getId());
        }
        eventsById.put(event.getId(), event);
    }

    /**
     * Moves the opening subsidy of every event from the manager to that event's
     * account. Called once, immediately after a file is loaded successfully.
     */
    public void paySubsidies() {
        for (Event event : eventsById.values()) {
            double subsidy = event.requiredSubsidy();
            managerAccount.withdraw(subsidy);
            event.getAccount().deposit(subsidy);
        }
    }

    public List<Event> getEvents() {
        return new ArrayList<>(eventsById.values());
    }

    public Event getEvent(int id) {
        Event event = eventsById.get(id);
        if (event == null) {
            throw new IllegalArgumentException("No event with id " + id + " is loaded");
        }
        return event;
    }

    public boolean isEmpty() {
        return eventsById.isEmpty();
    }

    public Account getManagerAccount() {
        return managerAccount;
    }
}
