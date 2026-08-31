package guessmarket.engine.model;

import guessmarket.engine.pricing.LmsrMarket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single tradable event. Holds its options, its trading method, its account
 * and its trade history.
 *
 * An event starts not started and can only be traded once its market maker has
 * opened it. Option indexes here start at 0; the UI converts to and from the
 * numbers shown on screen, which start at 1.
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 2L;

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

    /** Newest first, as the exercise requires. */
    public List<Transaction> getHistoryNewestFirst() {
        List<Transaction> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
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
            double[] none = new double[options.size()];
            return none;
        }
        return market.prices(quantities());
    }

    /** The subsidy an LMSR event needs before trading can start. */
    public double requiredSubsidy() {
        return market == null ? 0.0 : market.initialSubsidy(options.size());
    }

    /** What a purchase would cost, without performing it. */
    public double quote(int optionIndex, long quantity) {
        requireLmsr();
        validateOptionIndex(optionIndex);
        return market.costOfBuying(quantities(), optionIndex, quantity);
    }

    public Transaction buy(int optionIndex, long quantity) {
        requireLmsr();
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event " + id + " is not open for trading");
        }
        validateOptionIndex(optionIndex);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Share quantity must be a positive whole number");
        }

        double shareCost = market.costOfBuying(quantities(), optionIndex, quantity);
        double commission = (commissionType == CommissionType.ON_PURCHASE)
                ? shareCost * commissionPercent / 100.0
                : 0.0;

        options.get(optionIndex).addShares(quantity);
        account.deposit(shareCost + commission);
        commissionCollected += commission;

        Transaction transaction = new Transaction(
                history.size() + 1,
                options.get(optionIndex).getName(),
                quantity,
                shareCost,
                commission);
        history.add(transaction);
        return transaction;
    }

    /** Marks the event open. Moving the money is the caller's job. */
    public void markActive() {
        if (status != EventStatus.NOT_STARTED) {
            throw new IllegalStateException("Event " + id + " has already been started");
        }
        status = EventStatus.ACTIVE;
    }

    /**
     * Closes an LMSR event. Winners are paid one dollar per share; a commission
     * of type on-close is taken off that payout first. Whatever remains in the
     * account is reported so the caller can return it to the market maker.
     */
    public CloseOutcome close(int winningOptionIndex) {
        requireLmsr();
        if (status == EventStatus.CLOSED) {
            throw new IllegalStateException("Event " + id + " is already closed");
        }
        validateOptionIndex(winningOptionIndex);

        EventOption winner = options.get(winningOptionIndex);
        double gross = winner.getSharesBought() * LMSR_PAYOUT_PER_SHARE;
        double commission = (commissionType == CommissionType.ON_CLOSE)
                ? gross * commissionPercent / 100.0
                : 0.0;
        double net = gross - commission;

        commissionCollected += commission;
        account.withdraw(net);

        status = EventStatus.CLOSED;
        winningOptionName = winner.getName();

        return new CloseOutcome(winner.getName(), gross, commission, net, account.getBalance());
    }

    private void requireLmsr() {
        if (market == null) {
            throw new IllegalStateException(
                    "Event " + id + " is an order book event and does not support this operation yet");
        }
    }

    private void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("No such option in event " + id);
        }
    }
}
