package guessmarket.engine.api;

/** What opening an event cost its market maker. */
public record OpenReceipt(String marketMakerName,
                          double amountPaid,
                          double marketMakerBalanceAfter,
                          EventStateDto stateAfter) {
}
