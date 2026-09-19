package guessmarket.server.utils;

/** Names used in more than one place, gathered so they cannot drift apart. */
public final class Constants {

    /** The parameter the login screen sends. */
    public static final String USERNAME = "username";

    /** The attribute the session remembers the logged in name under. */
    public static final String USERNAME_IN_SESSION = "username";

    public static final String AMOUNT = "amount";

    /** The multipart part the client puts the chosen file in. */
    public static final String FILE_PART = "file";

    /** Which event a request is about. */
    public static final String EVENT_ID = "id";

    /** Which option of that event, counted from 0 as the engine counts them. */
    public static final String OPTION_INDEX = "option";

    public static final String QUANTITY = "quantity";
    public static final String PRICE = "price";

    /** "Buy" or "Sell". */
    public static final String SIDE = "side";

    /**
     * The version a polling client already has. The server answers 204 and an
     * empty body when that is still the current one.
     */
    public static final String SINCE = "since";

    /** The header every answer carries the current market version in. */
    public static final String VERSION_HEADER = "X-Market-Version";

    public static final String JSON = "application/json";
    public static final String TEXT = "text/plain;charset=UTF-8";

    private Constants() {
    }
}
