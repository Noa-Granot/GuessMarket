package guessmarket.engine.orderbook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What happened when an order was submitted: the trades it caused, how much of
 * it is still resting, and the money each person owes or is owed.
 *
 * The event works out the movements but does not perform them, because it does
 * not know about user accounts. The engine applies them.
 */
public class MatchResult {

    private final List<Trade> trades = new ArrayList<>();

    /** Positive means the user receives money, negative means they pay. */
    private final Map<String, Double> cashDelta = new LinkedHashMap<>();

    /** Commission owed to the market maker, already excluded from cashDelta payments. */
    private double commissionToMarketMaker = 0.0;

    /** Money that goes into the event account, which is what minting produces. */
    private double intoEventAccount = 0.0;

    private long restingQuantity = 0;

    public void addTrade(Trade trade) {
        trades.add(trade);
    }

    public void pay(String userName, double amount) {
        cashDelta.merge(userName, -amount, Double::sum);
    }

    public void receive(String userName, double amount) {
        cashDelta.merge(userName, amount, Double::sum);
    }

    public void addCommission(double amount) {
        commissionToMarketMaker += amount;
    }

    public void addToEventAccount(double amount) {
        intoEventAccount += amount;
    }

    public void setRestingQuantity(long restingQuantity) {
        this.restingQuantity = restingQuantity;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public Map<String, Double> getCashDelta() {
        return cashDelta;
    }

    public double getCommissionToMarketMaker() {
        return commissionToMarketMaker;
    }

    public double getIntoEventAccount() {
        return intoEventAccount;
    }

    public long getRestingQuantity() {
        return restingQuantity;
    }

    public boolean tradedAnything() {
        return !trades.isEmpty();
    }
}
