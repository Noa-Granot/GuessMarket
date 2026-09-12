package guessmarket.engine.model;

import java.io.Serializable;

/**
 * BONUS: one reading of an account balance, so the change over time can be drawn.
 * The step counts how many times this account has changed.
 */
public record BalancePoint(int step, double balance) implements Serializable {

    private static final long serialVersionUID = 1L;
}
