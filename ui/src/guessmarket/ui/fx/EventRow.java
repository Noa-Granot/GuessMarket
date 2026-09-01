package guessmarket.ui.fx;

/** One line of the events table. A view model, so the table never holds an engine object. */
public class EventRow {

    private final int id;
    private final String name;
    private final String status;
    private final String type;
    private final String commission;
    private final String marketMaker;
    private final double accountBalance;

    public EventRow(int id, String name, String status, String type,
                    String commission, String marketMaker, double accountBalance) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.type = type;
        this.commission = commission;
        this.marketMaker = marketMaker;
        this.accountBalance = accountBalance;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getType() { return type; }
    public String getCommission() { return commission; }
    public String getMarketMaker() { return marketMaker; }
    public double getAccountBalance() { return accountBalance; }
}
