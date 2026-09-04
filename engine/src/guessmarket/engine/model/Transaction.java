package guessmarket.engine.model;

import java.io.Serializable;

/** An immutable record of one purchase. */
public record Transaction(int serial,
                          String userName,
                          String optionName,
                          long quantity,
                          double shareCost,
                          double commission) implements Serializable {

    private static final long serialVersionUID = 2L;

    public double total() {
        return shareCost + commission;
    }
}
