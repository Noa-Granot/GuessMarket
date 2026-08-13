package guessmarket.engine.model;

import java.io.Serializable;

/**
 * An immutable record of one purchase. The commission field is populated even
 * though exercise 1 does not display it per row, because exercise 2 does.
 */
public record Transaction(int serial,
                          String optionName,
                          long quantity,
                          double shareCost,
                          double commission) implements Serializable {

    private static final long serialVersionUID = 1L;

    public double total() {
        return shareCost + commission;
    }
}
