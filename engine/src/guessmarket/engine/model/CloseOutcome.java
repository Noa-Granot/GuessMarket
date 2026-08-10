package guessmarket.engine.model;

/**
 * What happened when an event was closed. Returned by Event.close so the caller
 * can move the leftover money without Event needing to know about MarketSystem.
 */
public record CloseOutcome(String winningOptionName,
                           double grossPayout,
                           double commission,
                           double netPayout,
                           double leftoverForManager) {
}
