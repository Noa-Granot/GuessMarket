package guessmarket.engine.model;

/**
 * What happened when an event was closed.
 *
 * remainingBalance is what the event's account still holds after the winners
 * have been paid. Per the lecturer's clarification of 9/08, this money is NOT
 * swept anywhere: the account keeps its final balance so that command 3 shows
 * how the event ended up.
 */
public record CloseOutcome(String winningOptionName,
                           double grossPayout,
                           double commission,
                           double netPayout,
                           double remainingBalance) {
}
