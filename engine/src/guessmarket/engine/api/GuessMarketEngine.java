package guessmarket.engine.api;

import java.util.List;

/**
 * Everything the user interface can ask the engine to do, and the only part of
 * the engine it may know about. The engine is passive: it answers requests,
 * does not know who is calling, and never prints.
 *
 * Option numbers here start at 0; the caller converts to the numbers shown on
 * screen, which start at 1. Users are identified by name.
 */
public interface GuessMarketEngine {

    /** True once a valid file has been loaded. */
    boolean isLoaded();

    /**
     * Exercise 3. Makes sure there is a system to work with even before any
     * file has been uploaded, because people log in first.
     */
    void ensureStarted();

    /**
     * Exercise 3. Adds a user with the given name. Names are unique and are
     * compared without case.
     *
     * @throws EngineException if the name is already taken or is empty
     */
    void registerUser(String userName);

    /** Exercise 3. True if someone is already using that name. */
    boolean isNameTaken(String userName);

    /** Exercise 3. Puts money into a user's own account. */
    double addFunds(String userName, double amount);

    /**
     * Reads the file at the given path and replaces whatever is loaded. If the
     * file is not valid the current system is left alone and a LoadException is
     * thrown carrying every problem found.
     *
     * @return how many events were loaded
     */
    int loadFile(String path);

    List<EventDto> listEvents();

    List<UserDto> listUsers();

    EventStateDto eventState(int eventId);

    UserStateDto userState(String userName);

    /** What buying those shares would cost, in shares and in commission. */
    QuoteDto quote(int eventId, int optionIndex, long quantity);

    /**
     * Opens an event for trading. Only its market maker may do this, and only
     * if he can afford the opening cost.
     */
    OpenReceipt openEvent(int eventId, String userName);

    /** Buys shares of an LMSR event on behalf of a user. */
    PurchaseReceipt buy(int eventId, String userName, int optionIndex, long quantity);

    /**
     * Submits an order to an order book event. Any matching happens straight
     * away and whatever is left rests in the book.
     *
     * @param side "Buy" or "Sell"
     */
    OrderReceipt placeOrder(int eventId, String userName, int optionIndex,
                            String side, long quantity, double price);

    /** Closes an event and decides it. Only its market maker may do this. */
    CloseReceipt closeEvent(int eventId, String userName, int winningOptionIndex);

    /**
     * Bonus. Creates a new event, with the given user as its market maker. The
     * event starts not started, like any other.
     *
     * @return the new event's number
     */
    int createEvent(String userName, NewEventSpec spec);

    /**
     * Bonus. Writes the whole system, trade history included, to a file.
     *
     * @param pathWithoutExtension path and file name with no extension; the
     *                             engine adds its own
     */
    void saveState(String pathWithoutExtension);

    /**
     * Bonus. Restores a system written earlier by saveState.
     *
     * @return how many events were restored
     */
    int loadState(String pathWithoutExtension);
}
