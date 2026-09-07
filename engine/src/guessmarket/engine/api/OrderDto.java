package guessmarket.engine.api;

/** One order resting in a book. */
public record OrderDto(int serial,
                       String userName,
                       String sideDisplay,
                       long remaining,
                       long quantity,
                       double price) {
}
