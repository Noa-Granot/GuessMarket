package guessmarket.engine.api;

import java.util.List;

/** Summary of one event, for the "show events" command. */
public record EventDto(int id,
                       String name,
                       String description,
                       int commissionPercent,
                       String commissionTypeDisplay,
                       List<String> optionNames,
                       String statusDisplay) {
}
