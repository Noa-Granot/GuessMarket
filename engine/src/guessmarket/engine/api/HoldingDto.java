package guessmarket.engine.api;

/** How many shares of one option a user holds in one event. */
public record HoldingDto(String userName, String optionName, long shares) {
}
