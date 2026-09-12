package guessmarket.engine.api;

import java.util.List;

/** Full trading state of one event, for the details panel. */
public record EventStateDto(int id,
                            String name,
                            String description,
                            String typeDisplay,
                            String statusDisplay,
                            String marketMakerName,
                            int commissionPercent,
                            String commissionTypeDisplay,
                            List<OptionStateDto> options,
                            double accountBalance,
                            double commissionCollected,
                            List<TransactionDto> historyNewestFirst,
                            List<HoldingDto> participations,
                            String winningOptionName,
                            double openingCost,
                            List<BookDto> books,
                            List<TradeDto> tradesNewestFirst,
                            Double basePrice,
                            boolean allowMint,
                            List<SeriesDto> priceHistory) {

    public boolean isClosed() {
        return winningOptionName != null;
    }
}
