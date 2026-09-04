package guessmarket.engine.model;

/** Thrown when an account is asked for more money than it holds. */
public class InsufficientFundsException extends RuntimeException {

    private final double balance;
    private final double required;

    public InsufficientFundsException(double balance, double required) {
        super(String.format("the balance is %.2f but %.2f is needed", balance, required));
        this.balance = balance;
        this.required = required;
    }

    public double getBalance() {
        return balance;
    }

    public double getRequired() {
        return required;
    }
}
