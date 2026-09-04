package guessmarket.engine.model;

import java.io.Serializable;

/**
 * A balance that money moves in and out of. One account per event, plus one
 * per user. A withdrawal that would take the balance below zero is refused,
 * because no user may go into a negative balance.
 */
public class Account implements Serializable {

    private static final long serialVersionUID = 2L;

    /** Money is compared with a small tolerance so rounding cannot block a payout. */
    private static final double EPSILON = 0.000001;

    private double balance;

    public Account() {
        this(0.0);
    }

    public Account(double initialBalance) {
        this.balance = initialBalance;
    }

    public double getBalance() {
        return balance;
    }

    public boolean canAfford(double amount) {
        return amount <= balance + EPSILON;
    }

    public void deposit(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot deposit a negative amount");
        }
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot withdraw a negative amount");
        }
        if (!canAfford(amount)) {
            throw new InsufficientFundsException(balance, amount);
        }
        balance -= amount;
    }

    /** Empties the account and returns what was in it. */
    public double drain() {
        double remaining = balance;
        balance = 0.0;
        return remaining;
    }
}
