package guessmarket.engine.api;

import java.util.List;

/** Summary of one user, for the users screen. */
public record UserDto(String name,
                      double balance,
                      boolean isMarketMaker,
                      List<Integer> marketMakerForEventIds) {
}
