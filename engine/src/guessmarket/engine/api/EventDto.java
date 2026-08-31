package guessmarket.engine.api;

import java.util.List;

/** Summary of one event, for the events screen. */
public record EventDto(int id,
                       String name,
                       String description,
                       int commissionPercent,
                       String commissionTypeDisplay,
                       List<String> optionNames,
                       String statusDisplay,
                       String typeDisplay,
                       String marketMakerName,
                       double accountBalance) {
}
