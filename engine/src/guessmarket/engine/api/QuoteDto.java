package guessmarket.engine.api;

/** What a purchase would cost, before it is made. */
public record QuoteDto(double shareCost, double commission) {

    public double total() {
        return shareCost + commission;
    }
}
