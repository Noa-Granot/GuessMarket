package guessmarket.engine.api;

import java.util.List;

/** Everything the users screen shows about one user. */
public record UserStateDto(String name,
                           double balance,
                           boolean isMarketMaker,
                           List<EventRoleDto> events,
                           SeriesDto balanceHistory) {
}
