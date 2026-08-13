package guessmarket.engine.api;

/** How an event settled, plus its final state. */
public record CloseReceipt(String winningOptionName,
                           double grossPayout,
                           double commission,
                           double netPayout,
                           double remainingInEventAccount,
                           EventStateDto stateAfter) {
}
