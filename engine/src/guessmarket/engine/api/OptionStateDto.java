package guessmarket.engine.api;

/** Current value and share count of one option. */
public record OptionStateDto(String name, double price, long sharesBought) {
}
