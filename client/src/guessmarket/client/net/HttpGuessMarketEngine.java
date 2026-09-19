package guessmarket.client.net;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import guessmarket.engine.api.CloseReceipt;
import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.EventDto;
import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.NewEventSpec;
import guessmarket.engine.api.OpenReceipt;
import guessmarket.engine.api.OrderReceipt;
import guessmarket.engine.api.PurchaseReceipt;
import guessmarket.engine.api.QuoteDto;
import guessmarket.engine.api.UserDto;
import guessmarket.engine.api.UserStateDto;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The engine, as seen from a client: every call is an HTTP request.
 *
 * This is the whole point of the interface the project has carried since
 * exercise 1. The screens ask a GuessMarketEngine for events and get events.
 * In exercise 2 that engine was the real one, in the same process. Here it is
 * this class, and the screens cannot tell the difference.
 *
 * Three things are worth reading closely:
 *
 * 1. Caching. The screens poll about once a second. Each poll sends the
 *    version of the copy it is holding; the server answers 204 when that is
 *    still current and this class returns the copy it already had. So the
 *    screens make an engine call per second while the network mostly carries
 *    empty answers.
 *
 * 2. Identity. No call sends a user name. The server takes it from the
 *    session, so the name arguments the interface requires are ignored on the
 *    way out. They are kept because the interface is shared with exercise 2.
 *
 * 3. What is missing. Saving to disk and restoring belong to a single machine
 *    and have no meaning against a shared server, so they refuse rather than
 *    pretend.
 */
public class HttpGuessMarketEngine implements GuessMarketEngine {

    private static final Type EVENT_LIST = new TypeToken<List<EventDto>>() { }.getType();
    private static final Type USER_LIST = new TypeToken<List<UserDto>>() { }.getType();

    private final ServerConnection server;
    private final Gson gson = new Gson();

    /** The last answer for each thing polled, and the version it arrived with. */
    private Cached<List<EventDto>> events = Cached.empty();
    private Cached<List<UserDto>> users = Cached.empty();
    private Cached<UserStateDto> me = Cached.empty();
    private final Map<Integer, Cached<EventStateDto>> eventStates = new LinkedHashMap<>();

    public HttpGuessMarketEngine() {
        this(new ServerConnection());
    }

    public HttpGuessMarketEngine(ServerConnection server) {
        this.server = server;
    }

    // ---------- login, which the interface calls registering ----------

    /**
     * Logs in. The server refuses a name already in use, and that refusal
     * arrives as an EngineException carrying the sentence to show.
     */
    @Override
    public void registerUser(String userName) {
        server.post("/login", Map.of("username", userName));
        forgetEverything();
    }

    /** Ends the session. The user stays in the market, as the exercise asks. */
    public void logout() {
        server.post("/logout", Map.of());
        forgetEverything();
    }

    /** The market version this client last saw. Useful when watching a poll. */
    public int version() {
        return server.version();
    }

    /** Who the server thinks this client is, or an empty string. */
    public String whoAmI() {
        return server.get("/whoami", Map.of());
    }

    @Override
    public boolean isNameTaken(String userName) {
        if (userName == null || userName.isBlank()) {
            return false;
        }
        for (UserDto user : listUsers()) {
            if (user.name().equalsIgnoreCase(userName.trim())) {
                return true;
            }
        }
        return false;
    }

    // ---------- uploading ----------

    /**
     * Sends the file at that path to the server. The path is the client's own
     * disk; nothing about it is sent, only the bytes.
     */
    @Override
    public int loadFile(String path) {
        if (path == null || path.isBlank()) {
            throw new EngineException("No file was chosen.");
        }
        return countIn(server.postFile("/upload", "file", new File(path.trim())));
    }

    @Override
    public int uploadFile(InputStream content, String uploaderName) {
        return countIn(server.postMultipart("/upload", "file", "events.xml",
                ServerConnection.readAll(content)));
    }

    /** Sends a chosen file and returns the sentence the server answered with. */
    public String upload(File file) {
        String answer = server.postFile("/upload", "file", file);
        forgetEverything();
        return answer;
    }

    /** The server answers "3 events added to the market."; the screens want the 3. */
    private int countIn(String answer) {
        forgetEverything();
        String[] words = answer == null ? new String[0] : answer.trim().split("\\s+");
        try {
            return words.length == 0 ? 0 : Integer.parseInt(words[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---------- the things the screens poll ----------

    @Override
    public List<EventDto> listEvents() {
        String body = server.getIfChanged("/events", Map.of(), events.version);
        if (body == null) {
            return events.value;
        }
        events = new Cached<>(gson.fromJson(body, EVENT_LIST), server.version());
        return events.value;
    }

    @Override
    public List<UserDto> listUsers() {
        String body = server.getIfChanged("/users", Map.of(), users.version);
        if (body == null) {
            return users.value;
        }
        users = new Cached<>(gson.fromJson(body, USER_LIST), server.version());
        return users.value;
    }

    @Override
    public EventStateDto eventState(int eventId) {
        Cached<EventStateDto> held = eventStates.get(eventId);
        Integer since = held == null ? null : held.version;

        String body = server.getIfChanged("/event", Map.of("id", eventId), since);
        if (body == null) {
            return held.value;
        }
        Cached<EventStateDto> fresh =
                new Cached<>(gson.fromJson(body, EventStateDto.class), server.version());
        eventStates.put(eventId, fresh);
        return fresh.value;
    }

    /**
     * The account screen. The name is ignored: the server describes whoever
     * the session belongs to, and a client may only ask about itself.
     */
    @Override
    public UserStateDto userState(String userName) {
        String body = server.getIfChanged("/user-state", Map.of(), me.version);
        if (body == null) {
            return me.value;
        }
        me = new Cached<>(gson.fromJson(body, UserStateDto.class), server.version());
        return me.value;
    }

    @Override
    public boolean isLoaded() {
        List<EventDto> current = listEvents();
        return current != null && !current.isEmpty();
    }

    /** The server starts itself. Nothing to do from here. */
    @Override
    public void ensureStarted() {
    }

    // ---------- the things that change the market ----------

    @Override
    public double addFunds(String userName, double amount) {
        String body = server.post("/funds", Map.of("amount", amount));
        forgetEverything();
        return Double.parseDouble(body.trim());
    }

    @Override
    public QuoteDto quote(int eventId, int optionIndex, long quantity) {
        String body = server.get("/quote", ordered(
                "id", eventId, "option", optionIndex, "quantity", quantity));
        return gson.fromJson(body, QuoteDto.class);
    }

    @Override
    public OpenReceipt openEvent(int eventId, String userName) {
        String body = server.post("/open", Map.of("id", eventId));
        forgetEverything();
        return gson.fromJson(body, OpenReceipt.class);
    }

    @Override
    public PurchaseReceipt buy(int eventId, String userName, int optionIndex, long quantity) {
        String body = server.post("/buy", ordered(
                "id", eventId, "option", optionIndex, "quantity", quantity));
        forgetEverything();
        return gson.fromJson(body, PurchaseReceipt.class);
    }

    @Override
    public OrderReceipt placeOrder(int eventId, String userName, int optionIndex,
                                   String side, long quantity, double price) {
        String body = server.post("/order", ordered(
                "id", eventId, "option", optionIndex, "side", side,
                "quantity", quantity, "price", price));
        forgetEverything();
        return gson.fromJson(body, OrderReceipt.class);
    }

    @Override
    public CloseReceipt closeEvent(int eventId, String userName, int winningOptionIndex) {
        String body = server.post("/close", ordered("id", eventId, "option", winningOptionIndex));
        forgetEverything();
        return gson.fromJson(body, CloseReceipt.class);
    }

    // ---------- what a client cannot do ----------

    @Override
    public int createEvent(String userName, NewEventSpec spec) {
        throw new EngineException("Creating an event from the client is not part of exercise 3.");
    }

    @Override
    public void saveState(String pathWithoutExtension) {
        throw new EngineException("The server does not save its state. "
                + "The exercise asks for everything to be lost when it stops.");
    }

    @Override
    public int loadState(String pathWithoutExtension) {
        throw new EngineException("The server does not save its state, so there is nothing to restore.");
    }

    // ---------- small things ----------

    /**
     * After anything that changes the market, the copies held here are stale
     * and asking with their version would be answered 204 by a server that has
     * already moved on. Dropping them costs one full answer and keeps the
     * screens honest.
     */
    private void forgetEverything() {
        events = Cached.empty();
        users = Cached.empty();
        me = Cached.empty();
        eventStates.clear();
    }

    /** Map.of does not keep the order, and a readable URL is worth the lines. */
    private Map<String, Object> ordered(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            map.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return map;
    }

    /** One answer and the market version it arrived with. */
    private record Cached<T>(T value, Integer version) {
        static <T> Cached<T> empty() {
            return new Cached<>(null, null);
        }
    }
}
