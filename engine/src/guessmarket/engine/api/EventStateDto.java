package guessmarket.engine.api;

import java.util.List;

/** Full trading state of one event, for the "event state" command. */
public record EventStateDto(int id,
                            String name,
                            List<OptionStateDto> options,
                            double accountBalance,
                            double commissionCollected,
                            List<TransactionDto> historyNewestFirst,
                            String statusDisplay,
                            String winningOptionName) {

    public boolean isClosed() {
        return winningOptionName != null;
    }
}
