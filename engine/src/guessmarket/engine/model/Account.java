package guessmarket.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /** BONUS: every balance this account has had, for the graph. */
    private final List<BalancePoint> history = new ArrayList<>();

    public Account() {
        this(0.0);
    }

    public Account(double initialBalance) {
        this.balance = initialBalance;
        history.add(new BalancePoint(0, initialBalance));
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
        record();
    }

    public void withdraw(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot withdraw a negative amount");
        }
        if (!canAfford(amount)) {
            throw new InsufficientFundsException(balance, amount);
        }
        balance -= amount;
        record();
    }

    /** Empties the account and returns what was in it. */
    public double drain() {
        double remaining = balance;
        balance = 0.0;
        record();
        return remaining;
    }

    private void record() {
        history.add(new BalancePoint(history.size(), balance));
    }

    /** BONUS: the balance after every change, oldest first. */
    public List<BalancePoint> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
