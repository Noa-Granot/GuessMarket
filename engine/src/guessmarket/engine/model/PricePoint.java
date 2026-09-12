package guessmarket.engine.model;

import java.io.Serializable;

/**
 * BONUS: one reading of an option's price, so the change over time can be drawn.
 * The step is a simple counter of how many times this event's prices have moved.
 */
public record PricePoint(int step, double price) implements Serializable {

    private static final long serialVersionUID = 1L;
}
