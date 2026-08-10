package guessmarket.engine.model;

/**
 * A balance that money moves in and out of. Used twice in exercise 1: once per
 * event (the event's own "contract" account) and once for the events manager,
 * who funds the LMSR subsidies.
 *
 * Design note: no overdraft guard here. In exercise 1 the manager account is
 * expected to go negative while subsidies are outstanding, and the single user
 * has unlimited funds. Exercise 2 introduces per-user balances that must not go
 * negative, which is where a guard belongs.
 */
public class Account {

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
        balance -= amount;
    }

    /** Empties the account and returns whatever was in it. */
    public double drain() {
        double remaining = balance;
        balance = 0.0;
        return remaining;
    }
}
