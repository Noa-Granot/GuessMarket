package guessmarket.ui.fx;

import java.util.List;

/** One line of the users table. */
public class UserRow {

    private final String name;
    private final double balance;
    private final String role;
    private final List<Integer> marketMakerFor;

    public UserRow(String name, double balance, String role, List<Integer> marketMakerFor) {
        this.name = name;
        this.balance = balance;
        this.role = role;
        this.marketMakerFor = marketMakerFor;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public String getRole() { return role; }
    public List<Integer> getMarketMakerFor() { return marketMakerFor; }
}
