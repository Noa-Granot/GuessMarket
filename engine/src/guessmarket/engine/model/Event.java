package guessmarket.engine.model;

import java.io.Serializable;

import guessmarket.engine.pricing.LmsrMarket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single tradable event. Owns its options, its pricing model, its own money
 * account and its trade history, and performs its own buy and close logic.
 *
 * All option indexes on this class are 0-based. Converting to and from the
 * 1-based numbers the exercise requires on screen is the UI's job.
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Every winning share pays out one dollar. */
    public static final double PAYOUT_PER_SHARE = 1.0;

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final List<EventOption> options;
    private final LmsrMarket market;
    private final Account account = new Account();
    private final List<Transaction> history = new ArrayList<>();

    private EventStatus status = EventStatus.ACTIVE;
    private String winningOptionName = null;
    private double commissionCollected = 0.0;

    public Event(int id,
                 String name,
                 String description,
                 int commissionPercent,
                 CommissionType commissionType,
                 List<String> optionNames,
                 int liquidityB) {

        if (commissionPercent < 0 || commissionPercent > 90) {
            throw new IllegalArgumentException(
                    "Commission must be between 0 and 90, got " + commissionPercent
                            + " (event " + id + ")");
        }
        if (optionNames == null || optionNames.size() != 2) {
            throw new IllegalArgumentException(
                    "Exercise 1 events must have exactly 2 options (event " + id + ")");
        }

        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.market = new LmsrMarket(liquidityB);

        List<EventOption> built = new ArrayList<>();
        for (String optionName : optionNames) {
            built.add(new EventOption(optionName));
        }
        this.options = built;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercent() {
        return commissionPercent;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }

    public double getCommissionCollected() {
        return commissionCollected;
    }

    public Account getAccount() {
        return account;
    }

    public LmsrMarket getMarket() {
        return market;
    }

    public List<EventOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    /** Newest first, as the exercise requires. */
    public List<Transaction> getHistoryNewestFirst() {
        List<Transaction> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
    }

    public int getOptionCount() {
        return options.size();
    }

    private long[] quantities() {
        long[] q = new long[options.size()];
        for (int i = 0; i < q.length; i++) {
            q[i] = options.get(i).getSharesBought();
        }
        return q;
    }

    public double[] currentPrices() {
        return market.prices(quantities());
    }

    /** The subsidy this event needs before trading can start. */
    public double requiredSubsidy() {
        return market.initialSubsidy(options.size());
    }

    /** What a purchase would cost, without performing it. */
    public double quote(int optionIndex, long quantity) {
        validateOptionIndex(optionIndex);
        return market.costOfBuying(quantities(), optionIndex, quantity);
    }

    public Transaction buy(int optionIndex, long quantity) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event " + id + " is closed and cannot be traded");
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

    /**
     * Decides the event. Winners are paid one dollar per share they hold; if the
     * commission is charged on close, it is taken off that payout first.
     *
     * The account is deliberately NOT emptied afterwards. Whatever remains is
     * the market maker's standing position in this event, and command 3 is
     * expected to show it.
     */
    public CloseOutcome close(int winningOptionIndex) {
        if (status == EventStatus.CLOSED) {
            throw new IllegalStateException("Event " + id + " is already closed");
        }
        validateOptionIndex(winningOptionIndex);

        EventOption winner = options.get(winningOptionIndex);
        double gross = winner.getSharesBought() * PAYOUT_PER_SHARE;
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

    private void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("No such option in event " + id);
        }
    }
}
