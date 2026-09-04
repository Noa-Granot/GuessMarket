package guessmarket.engine.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The result of closing an event. The event account is emptied: the winners are
 * paid according to their holdings, any closing commission goes to the market
 * maker, and whatever is left over goes to him as well.
 */
public record CloseOutcome(String winningOptionName,
                           long winningShares,
                           double grossPayout,
                           double commission,
                           double netPaidToWinners,
                           double returnedToMarketMaker,
                           Map<String, Double> payoutsByUser) {

    public CloseOutcome {
        payoutsByUser = new LinkedHashMap<>(payoutsByUser);
    }
}
