package guessmarket.engine.orderbook;

import java.io.Serializable;

/**
 * One completed piece of business.
 *
 * A MATCH moves shares from a seller to a buyer. A MINT creates new shares for
 * two buyers on opposite options, and the money goes to the event account
 * instead of to a seller, so sellerName is null.
 */
public record Trade(int serial,
                    Kind kind,
                    String buyerName,
                    String sellerName,
                    String optionName,
                    long quantity,
                    double price,
                    double commission) implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Kind { MATCH, MINT }

    public double value() {
        return quantity * price;
    }
}
