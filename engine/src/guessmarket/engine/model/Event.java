package guessmarket.engine.model;

import guessmarket.engine.pricing.LmsrMarket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A single tradable event. Holds its options, its trading method, its account,
 * its trade history and who owns which shares.
 *
 * An event starts not started and can only be traded once its market maker has
 * opened it. Option indexes here start at 0; the UI converts to and from the
 * numbers shown on screen, which start at 1.
 *
 * This class moves money into and out of its own account only. Moving money to
 * or from a user is the caller's job, because an event does not know about
 * users.
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 3L;

    /** Every winning share of an LMSR event pays out one dollar. */
    public static final double LMSR_PAYOUT_PER_SHARE = 1.0;

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final EventType type;
    private final List<EventOption> options;

    /** Set for LMSR events, null for order book events. */
    private final LmsrMarket market;

    /** Set for order book events, null for LMSR events. */
    private final OrderBookConfig orderBookConfig;

    private final Account account = new Account();
    private final List<Transaction> history = new ArrayList<>();

    /** user name to shares held, one entry per option index. */
    private final Map<String, long[]> holdings = new LinkedHashMap<>();

    private String marketMakerName;
    private EventStatus status = EventStatus.NOT_STARTED;
    private String winningOptionName = null;
    private double commissionCollected = 0.0;

    private Event(int id, String name, String description,
                  int commissionPercent, CommissionType commissionType,
                  EventType type, List<String> optionNames,
                  LmsrMarket market, OrderBookConfig orderBookConfig) {

        if (commissionPercent < 0 || commissionPercent > 90) {
            throw new IllegalArgumentException(
                    "Commission must be between 0 and 90, got " + commissionPercent
                            + " (event " + id + ")");
        }
        if (optionNames == null || optionNames.size() != 2) {
            throw new IllegalArgumentException(
                    "Every event must have exactly 2 options (event " + id + ")");
        }

        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.type = type;
        this.market = market;
        this.orderBookConfig = orderBookConfig;

        List<EventOption> built = new ArrayList<>();
        for (String optionName : optionNames) {
            built.add(new EventOption(optionName));
        }
        this.options = built;
    }

    public static Event lmsr(int id, String name, String description,
                             int commissionPercent, CommissionType commissionType,
                             List<String> optionNames, int liquidityB) {
        return new Event(id, name, description, commissionPercent, commissionType,
                EventType.LMSR, optionNames, new LmsrMarket(liquidityB), null);
    }

    public static Event orderBook(int id, String name, String description,
                                  int commissionPercent, CommissionType commissionType,
                                  List<String> optionNames, OrderBookConfig config) {
        return new Event(id, name, description, commissionPercent, commissionType,
                EventType.ORDER_BOOK, optionNames, null, config);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommissionPercent() { return commissionPercent; }
    public CommissionType getCommissionType() { return commissionType; }
    public EventType getType() { return type; }
    public EventStatus getStatus() { return status; }
    public String getWinningOptionName() { return winningOptionName; }
    public double getCommissionCollected() { return commissionCollected; }
    public Account getAccount() { return account; }
    public String getMarketMakerName() { return marketMakerName; }

    public void setMarketMakerName(String marketMakerName) {
        this.marketMakerName = marketMakerName;
    }

    /** Null on an order book event. */
    public LmsrMarket getMarket() { return market; }

    /** Null on an LMSR event. */
    public OrderBookConfig getOrderBookConfig() { return orderBookConfig; }

    public List<EventOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public int getOptionCount() {
        return options.size();
    }

    public boolean isMarketMaker(String userName) {
        return marketMakerName != null && marketMakerName.equals(userName);
    }

    /** Newest first, as the exercise requires. */
    public List<Transaction> getHistoryNewestFirst() {
        List<Transaction> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
    }

    public Map<String, long[]> getHoldings() {
        Map<String, long[]> copy = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> entry : holdings.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    public long sharesHeldBy(String userName, int optionIndex) {
        long[] owned = holdings.get(userName);
        return owned == null ? 0 : owned[optionIndex];
    }

    private void addHolding(String userName, int optionIndex, long quantity) {
        holdings.computeIfAbsent(userName, k -> new long[options.size()])[optionIndex] += quantity;
    }

    private long[] quantities() {
        long[] q = new long[options.size()];
        for (int i = 0; i < q.length; i++) {
            q[i] = options.get(i).getSharesBought();
        }
        return q;
    }

    /** Only meaningful on an LMSR event. */
    public double[] currentPrices() {
        if (market == null) {
            return new double[options.size()];
        }
        return market.prices(quantities());
    }

    /** The subsidy an LMSR event needs before trading can start. */
    public double requiredSubsidy() {
        return market == null ? 0.0 : market.initialSubsidy(options.size());
    }

    /** What the market maker must pay to open this event. */
    public double openingCost() {
        if (type == EventType.LMSR) {
            return requiredSubsidy();
        }
        return orderBookConfig.initialShares() * (double) orderBookConfig.d();
    }

    /** What a purchase would cost in shares alone, without performing it. */
    public double quoteShares(int optionIndex, long quantity) {
        requireLmsr();
        validateOptionIndex(optionIndex);
        return market.costOfBuying(quantities(), optionIndex, quantity);
    }

    /** The commission that would be charged on a purchase of that cost. */
    public double commissionOnPurchase(double shareCost) {
        return commissionType == CommissionType.ON_PURCHASE
                ? shareCost * commissionPercent / 100.0
                : 0.0;
    }

    /**
     * Opens the event for trading. The money the market maker pays is moved by
     * the caller; this only deposits it and records the opening stock.
     */
    public void open(double amountPaidByMarketMaker) {
        if (status != EventStatus.NOT_STARTED) {
            throw new IllegalStateException("Event " + id + " has already been started");
        }
        account.deposit(amountPaidByMarketMaker);

        if (type == EventType.ORDER_BOOK) {
            // The initial stock belongs to the market maker and he may sell it.
            long initial = orderBookConfig.initialShares();
            for (int i = 0; i < options.size(); i++) {
                options.get(i).addShares(initial);
                addHolding(marketMakerName, i, initial);
            }
        }
        status = EventStatus.ACTIVE;
    }

    /**
     * Buys shares for a user. The money is taken from the user by the caller;
     * this records the shares and deposits the payment into the event account.
     */
    public Transaction buy(String userName, int optionIndex, long quantity) {
        requireLmsr();
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event " + id + " is not open for trading");
        }
        validateOptionIndex(optionIndex);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Share quantity must be a positive whole number");
        }

        double shareCost = market.costOfBuying(quantities(), optionIndex, quantity);
        double commission = commissionOnPurchase(shareCost);

        options.get(optionIndex).addShares(quantity);
        addHolding(userName, optionIndex, quantity);
        account.deposit(shareCost + commission);
        commissionCollected += commission;

        Transaction transaction = new Transaction(
                history.size() + 1,
                userName,
                options.get(optionIndex).getName(),
                quantity,
                shareCost,
                commission);
        history.add(transaction);
        return transaction;
    }

    /**
     * Closes the event. The winners are paid from the event account according to
     * their holdings, a closing commission is set aside for the market maker,
     * and whatever remains goes to him too. The account ends empty.
     */
    public CloseOutcome close(int winningOptionIndex) {
        if (status == EventStatus.CLOSED) {
            throw new IllegalStateException("Event " + id + " is already closed");
        }
        if (status == EventStatus.NOT_STARTED) {
            throw new IllegalStateException("Event " + id + " has not been opened yet");
        }
        validateOptionIndex(winningOptionIndex);

        EventOption winner = options.get(winningOptionIndex);
        double payoutPerShare = (type == EventType.LMSR)
                ? LMSR_PAYOUT_PER_SHARE
                : orderBookConfig.d();

        long winningShares = winner.getSharesBought();
        double gross = winningShares * payoutPerShare;
        double commission = (commissionType == CommissionType.ON_CLOSE)
                ? gross * commissionPercent / 100.0
                : 0.0;
        double netPerShare = payoutPerShare * (1.0 - commissionPercent / 100.0 *
                (commissionType == CommissionType.ON_CLOSE ? 1 : 0));

        Map<String, Double> payouts = new LinkedHashMap<>();
        double paidOut = 0.0;
        for (Map.Entry<String, long[]> entry : holdings.entrySet()) {
            long held = entry.getValue()[winningOptionIndex];
            if (held <= 0) {
                continue;
            }
            double amount = held * netPerShare;
            payouts.put(entry.getKey(), amount);
            paidOut += amount;
        }

        commissionCollected += commission;
        account.withdraw(paidOut);
        double leftover = account.drain();

        status = EventStatus.CLOSED;
        winningOptionName = winner.getName();

        return new CloseOutcome(winner.getName(), winningShares, gross, commission,
                paidOut, leftover, payouts);
    }

    private void requireLmsr() {
        if (market == null) {
            throw new IllegalStateException(
                    "Event " + id + " is an order book event, so shares are traded through the book");
        }
    }

    private void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("No such option in event " + id);
        }
    }
}
