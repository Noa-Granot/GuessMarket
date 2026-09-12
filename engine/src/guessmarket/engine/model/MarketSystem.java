package guessmarket.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything currently loaded: the events and the users, both in file order.
 * A file load replaces this whole object, which is how a faulty file cannot
 * damage a system that is already loaded.
 */
public class MarketSystem implements Serializable {

    private static final long serialVersionUID = 2L;

    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();
    private final Map<String, User> usersByName = new LinkedHashMap<>();

    public void addEvent(Event event) {
        if (eventsById.containsKey(event.getId())) {
            throw new IllegalArgumentException("Duplicate event id: " + event.getId());
        }
        eventsById.put(event.getId(), event);
    }

    public void addUser(User user) {
        if (usersByName.containsKey(user.getName())) {
            throw new IllegalArgumentException("Duplicate user name: " + user.getName());
        }
        usersByName.put(user.getName(), user);
    }

    public List<Event> getEvents() {
        return new ArrayList<>(eventsById.values());
    }

    public List<User> getUsers() {
        return new ArrayList<>(usersByName.values());
    }

    public Event getEvent(int id) {
        Event event = eventsById.get(id);
        if (event == null) {
            throw new IllegalArgumentException("No event with id " + id + " is loaded");
        }
        return event;
    }

    public User getUser(String name) {
        User user = usersByName.get(name);
        if (user == null) {
            throw new IllegalArgumentException("No user named " + name + " is loaded");
        }
        return user;
    }

    public boolean hasEvent(int id) {
        return eventsById.containsKey(id);
    }

    /** BONUS: the smallest number no event is using. */
    public int nextFreeEventId() {
        int candidate = 1;
        while (eventsById.containsKey(candidate)) {
            candidate++;
        }
        return candidate;
    }

    public boolean isEmpty() {
        return eventsById.isEmpty();
    }
}
