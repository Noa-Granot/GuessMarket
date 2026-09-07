package guessmarket.engine.api;

import java.util.List;

/** What happened when an order was submitted. */
public record OrderReceipt(String userName,
                           String sideDisplay,
                           String optionName,
                           long quantitySubmitted,
                           long quantityResting,
                           List<TradeDto> trades,
                           double balanceAfter,
                           EventStateDto stateAfter) {

    public long quantityFilled() {
        return quantitySubmitted - quantityResting;
    }
}
