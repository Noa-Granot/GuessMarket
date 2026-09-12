package guessmarket.engine.api;

import java.util.List;

/** BONUS: one named line on a graph. */
public record SeriesDto(String name, List<PointDto> points) {
}
