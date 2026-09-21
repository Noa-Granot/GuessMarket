package guessmarket.engine.api;

/**
 * One line of a person's account: what happened, how much moved, and what the
 * balance was afterwards.
 *
 * @param change positive for money in, negative for money out
 */
public record MovementDto(int serial, String reason, double change, double balanceAfter) {
}
