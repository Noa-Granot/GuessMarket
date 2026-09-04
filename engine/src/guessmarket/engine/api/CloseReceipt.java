package guessmarket.engine.api;

import java.util.List;

/** How an event settled, plus its final state. */
public record CloseReceipt(String winningOptionName,
                           long winningShares,
                           double grossPayout,
                           double commission,
                           double netPaidToWinners,
                           double returnedToMarketMaker,
                           List<PayoutDto> payouts,
                           EventStateDto stateAfter) {
}
