package guessmarket.client.fx;

/** One line of the users table. A view model, so the table never holds a DTO. */
public class UserRow {

    private final String name;
    private final double balance;
    private final String role;
    private final boolean you;

    public UserRow(String name, double balance, String role, boolean you) {
        this.name = name;
        this.balance = balance;
        this.role = role;
        this.you = you;
    }

    public String getName() { return you ? name + "  (you)" : name; }
    public double getBalance() { return balance; }
    public String getRole() { return role; }
    public boolean isYou() { return you; }
}
