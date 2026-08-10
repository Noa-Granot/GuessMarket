package guessmarket.engine.api;

/** What a purchase cost, plus the resulting state of the event. */
public record PurchaseReceipt(String optionName,
                              long quantity,
                              double shareCost,
                              double commission,
                              EventStateDto stateAfter) {

    public double total() {
        return shareCost + commission;
    }
}
