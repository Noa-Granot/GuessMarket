package guessmarket.engine.api;

/** One row of an event's trade history. */
public record TransactionDto(int serial,
                             String optionName,
                             long quantity,
                             double shareCost,
                             double commission) {
}
