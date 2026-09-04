package guessmarket.engine.api;

/** What a purchase cost, plus the resulting state of the event. */
public record PurchaseReceipt(String userName,
                              String optionName,
                              long quantity,
                              double shareCost,
                              double commission,
                              double buyerBalanceAfter,
                              EventStateDto stateAfter) {

    public double total() {
        return shareCost + commission;
    }
}
