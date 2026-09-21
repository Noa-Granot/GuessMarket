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

    /**
     * Every change to this balance and why, oldest first. The graph above
     * needs only the numbers; the account screen needs the reasons.
     */
    private final List<Movement> movements = new ArrayList<>();

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
        deposit(amount, "Money in");
    }

    public void deposit(double amount, String reason) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot deposit a negative amount");
        }
        balance += amount;
        record(amount, reason);
    }

    public void withdraw(double amount) {
        withdraw(amount, "Money out");
    }

    public void withdraw(double amount, String reason) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot withdraw a negative amount");
        }
        if (!canAfford(amount)) {
            throw new InsufficientFundsException(balance, amount);
        }
        balance -= amount;
        record(-amount, reason);
    }

    /** Empties the account and returns what was in it. */
    public double drain() {
        return drain("Account emptied");
    }

    public double drain(String reason) {
        double remaining = balance;
        balance = 0.0;
        record(-remaining, reason);
        return remaining;
    }

    private void record(double change, String reason) {
        history.add(new BalancePoint(history.size(), balance));
        movements.add(new Movement(movements.size() + 1, reason, change, balance));
    }

    /** BONUS: the balance after every change, oldest first. */
    public List<BalancePoint> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /** Every change and why, oldest first. */
    public List<Movement> getMovements() {
        return Collections.unmodifiableList(movements);
    }
}
