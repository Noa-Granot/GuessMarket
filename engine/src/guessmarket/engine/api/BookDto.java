package guessmarket.engine.api;

import java.util.List;

/**
 * One option's order book, with the numbers the screen shows.
 * Any of the five statistics may be null when there is nothing to compute it
 * from, which is normal in a market with few orders.
 */
public record BookDto(String optionName,
                      Double last,
                      Double bestBid,
                      Double bestAsk,
                      Double mid,
                      Double spread,
                      List<OrderDto> bids,
                      List<OrderDto> asks,
                      long sharesInIssue) {
}
