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

    /** Closes an event and decides it. Only its market maker may do this. */
    CloseReceipt closeEvent(int eventId, String userName, int winningOptionIndex);

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
