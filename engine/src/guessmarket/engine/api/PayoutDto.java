package guessmarket.engine.api;

/** What one user received when an event closed. */
public record PayoutDto(String userName, double amount) {
}
