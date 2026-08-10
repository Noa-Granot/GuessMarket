package guessmarket.engine.api;

import java.util.List;

/**
 * Everything the outside world can ask the engine to do. The console UI in
 * exercise 1, the JavaFX controllers in exercise 2 and the servlets in exercise
 * 3 all talk to this interface and nothing else.
 *
 * The engine is passive: it answers questions and performs commands, and it has
 * no idea who is calling. It never prints anything.
 *
 * Option numbers here are 0-based. The 1-based numbering the exercise requires
 * on screen is the caller's responsibility.
 */
public interface GuessMarketEngine {

    /** True once a valid file (or the demo data) has been loaded. */
    boolean isLoaded();

    /** Temporary stand-in for loadFile, used until the XML loader exists. */
    void loadDemoData();

    List<EventDto> listEvents();

    /** Only events that can still be traded. */
    List<EventDto> listActiveEvents();

    EventStateDto eventState(int eventId);

    /** What a purchase would cost, without performing it. */
    double quote(int eventId, int optionIndex, long quantity);

    PurchaseReceipt buy(int eventId, int optionIndex, long quantity);

    CloseReceipt close(int eventId, int winningOptionIndex);

    double managerBalance();
}
