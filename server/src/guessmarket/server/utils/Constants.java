package guessmarket.server.utils;

/** Names used in more than one place, gathered so they cannot drift apart. */
public final class Constants {

    /** The parameter the login screen sends. */
    public static final String USERNAME = "username";

    /** The attribute the session remembers the logged in name under. */
    public static final String USERNAME_IN_SESSION = "username";

    public static final String AMOUNT = "amount";

    public static final String JSON = "application/json";
    public static final String TEXT = "text/plain;charset=UTF-8";

    private Constants() {
    }
}
