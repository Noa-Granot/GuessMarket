package guessmarket.engine.api;

import java.util.List;

/** One line of a user's participation list: an event they run or hold shares in. */
public record EventRoleDto(int eventId,
                           String eventName,
                           String statusDisplay,
                           boolean isMarketMaker,
                           List<HoldingDto> holdings) {
}
