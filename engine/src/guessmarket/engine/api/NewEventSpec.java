package guessmarket.engine.api;

import java.util.List;

/**
 * BONUS: what the user filled in to create an event. The user who submits it
 * becomes its market maker.
 *
 * For an LMSR event only liquidityB matters; for an order book event only the
 * three order book fields do.
 */
public record NewEventSpec(String name,
                           String description,
                           int commissionPercent,
                           String commissionType,
                           List<String> optionNames,
                           boolean orderBook,
                           int liquidityB,
                           int initialShares,
                           int basePrice,
                           boolean allowMint) {
}
