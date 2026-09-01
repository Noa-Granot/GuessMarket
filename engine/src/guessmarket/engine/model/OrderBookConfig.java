package guessmarket.engine.model;

import java.io.Serializable;

/**
 * The settings of an order book event, taken from the file.
 *
 * initialShares is the stock the market maker creates when opening the event,
 * d is the value a winning share pays out, and minting allows two matching
 * orders whose prices together exceed d to create new shares.
 */
public record OrderBookConfig(int initialShares, int d, boolean allowMint) implements Serializable {

    private static final long serialVersionUID = 1L;

    /** No order may be priced at d or above. */
    public double highestAllowedPrice() {
        return d - 0.01;
    }
}
