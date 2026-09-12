package guessmarket.engine.model;

import guessmarket.engine.orderbook.MatchResult;
import guessmarket.engine.orderbook.Order;
import guessmarket.engine.orderbook.OrderBook;
import guessmarket.engine.orderbook.OrderSide;
import guessmarket.engine.orderbook.Trade;
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

    private static final long serialVersionUID = 4L;

    /** Prices are compared with a tolerance so 0.1 + 0.2 behaves. */
    private static final double PRICE_EPSILON = 0.0000001;

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

    /** One book per option, on order book events only. */
    private final List<OrderBook> books = new ArrayList<>();
    private final List<Trade> trades = new ArrayList<>();
    private int nextOrderSerial = 1;
    private int nextTradeSerial = 1;

    /** BONUS: the price of each option after every change, for the graph. */
    private final List<List<PricePoint>> priceHistory = new ArrayList<>();
    private int priceStep = 0;

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

        if (type == EventType.ORDER_BOOK) {
            for (EventOption option : built) {
                books.add(new OrderBook(option.getName()));
            }
        }
        for (int i = 0; i < built.size(); i++) {
            priceHistory.add(new ArrayList<>());
        }
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

    /** Order book commission goes straight to the market maker; this only records it. */
    public void addCommissionCollected(double amount) {
        commissionCollected += amount;
    }

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
        recordPrices();
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
        recordPrices();
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


    // ---------- order book ----------

    public List<OrderBook> getBooks() {
        return Collections.unmodifiableList(books);
    }

    public List<Trade> getTradesNewestFirst() {
        List<Trade> reversed = new ArrayList<>(trades);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
    }

    /** Money this user already has tied up in resting buy orders. */
    public double cashCommittedBy(String userName) {
        double total = 0;
        for (OrderBook book : books) {
            total += book.cashCommittedBy(userName);
        }
        return total;
    }

    /** Shares of one option this user already has tied up in resting sell orders. */
    public long sharesCommittedBy(String userName, int optionIndex) {
        return books.get(optionIndex).sharesCommittedBy(userName);
    }

    /**
     * Submits an order and works out everything that follows from it: the
     * matches against resting orders, then a mint if one is possible, then
     * whatever is left rests in the book.
     *
     * No money is moved here. The result describes who owes what, and the
     * caller applies it, because an event does not know about accounts.
     */
    public MatchResult submitOrder(String userName, int optionIndex,
                                   OrderSide side, long quantity, double price) {
        requireOrderBook();
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event " + id + " is not open for trading");
        }
        validateOptionIndex(optionIndex);
        if (quantity <= 0) {
            throw new IllegalArgumentException("The number of shares must be a whole number above zero");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("The price must be above zero");
        }
        double highest = orderBookConfig.highestAllowedPrice();
        if (price > highest + PRICE_EPSILON) {
            throw new IllegalArgumentException(String.format(
                    "The price cannot be above %.2f, which is d minus 0.01", highest));
        }

        Order order = new Order(nextOrderSerial++, userName, side, optionIndex, price, quantity);
        MatchResult result = new MatchResult();

        matchAgainstBook(order, result);
        if (side == OrderSide.BUY && !order.isFilled() && orderBookConfig.allowMint()) {
            mintAgainstOppositeOption(order, result);
        }

        for (OrderBook book : books) {
            book.removeFilled();
        }
        if (!order.isFilled()) {
            books.get(optionIndex).add(order);
        }
        if (result.tradedAnything()) {
            recordPrices();
        }
        result.setRestingQuantity(order.getRemaining());
        return result;
    }

    /**
     * Fills the new order against the other side of the same option's book,
     * best price first. One order may consume several resting ones.
     */
    private void matchAgainstBook(Order order, MatchResult result) {
        OrderBook book = books.get(order.getOptionIndex());
        String optionName = options.get(order.getOptionIndex()).getName();

        List<Order> candidates = new ArrayList<>(
                order.getSide() == OrderSide.BUY ? book.getAsks() : book.getBids());

        for (Order resting : candidates) {
            if (order.isFilled()) {
                break;
            }
            if (resting.getUserName().equals(order.getUserName())) {
                continue; // nobody trades with themselves
            }
            boolean priceMeets = (order.getSide() == OrderSide.BUY)
                    ? resting.getPrice() <= order.getPrice() + PRICE_EPSILON
                    : resting.getPrice() >= order.getPrice() - PRICE_EPSILON;
            if (!priceMeets) {
                break; // the book is sorted, so nothing further can match either
            }

            long filled = Math.min(order.getRemaining(), resting.getRemaining());
            // The order that was waiting sets the price, as an exchange does.
            double tradePrice = resting.getPrice();
            double value = filled * tradePrice;

            String buyer = order.getSide() == OrderSide.BUY ? order.getUserName() : resting.getUserName();
            String seller = order.getSide() == OrderSide.BUY ? resting.getUserName() : order.getUserName();

            moveShares(seller, buyer, order.getOptionIndex(), filled);
            result.pay(buyer, value);
            result.receive(seller, value);

            double commission = commissionOnPurchase(value);
            if (commission > 0) {
                result.pay(buyer, commission);
                result.addCommission(commission);
            }

            order.reduceBy(filled);
            resting.reduceBy(filled);
            book.recordTrade(tradePrice);
            trades.add(new Trade(nextTradeSerial++, Trade.Kind.MATCH,
                    buyer, seller, optionName, filled, tradePrice, commission));
            result.addTrade(trades.get(trades.size() - 1));
        }
    }

    /**
     * Creates new shares when two buyers want opposite options and their prices
     * together cover the base value.
     *
     * The order that was already waiting pays its own price; the one that has
     * just arrived pays whatever completes the base value, so the event account
     * always receives exactly d for each new pair.
     */
    private void mintAgainstOppositeOption(Order order, MatchResult result) {
        int otherIndex = 1 - order.getOptionIndex();
        OrderBook otherBook = books.get(otherIndex);
        double d = orderBookConfig.d();

        String myOption = options.get(order.getOptionIndex()).getName();
        String otherOption = options.get(otherIndex).getName();

        for (Order resting : new ArrayList<>(otherBook.getBids())) {
            if (order.isFilled()) {
                break;
            }
            if (resting.getPrice() + order.getPrice() < d - PRICE_EPSILON) {
                break; // bids are sorted high first, so nothing further reaches d
            }

            long minted = Math.min(order.getRemaining(), resting.getRemaining());
            double restingPrice = resting.getPrice();
            double newPrice = d - restingPrice;

            addHolding(order.getUserName(), order.getOptionIndex(), minted);
            addHolding(resting.getUserName(), otherIndex, minted);
            options.get(order.getOptionIndex()).addShares(minted);
            options.get(otherIndex).addShares(minted);

            double newValue = minted * newPrice;
            double restingValue = minted * restingPrice;
            result.pay(order.getUserName(), newValue);
            result.pay(resting.getUserName(), restingValue);
            result.addToEventAccount(newValue + restingValue);

            double newCommission = commissionOnPurchase(newValue);
            double restingCommission = commissionOnPurchase(restingValue);
            if (newCommission > 0) {
                result.pay(order.getUserName(), newCommission);
                result.addCommission(newCommission);
            }
            if (restingCommission > 0) {
                result.pay(resting.getUserName(), restingCommission);
                result.addCommission(restingCommission);
            }

            order.reduceBy(minted);
            resting.reduceBy(minted);
            books.get(order.getOptionIndex()).recordTrade(newPrice);
            otherBook.recordTrade(restingPrice);

            trades.add(new Trade(nextTradeSerial++, Trade.Kind.MINT,
                    order.getUserName(), null, myOption, minted, newPrice, newCommission));
            result.addTrade(trades.get(trades.size() - 1));
            trades.add(new Trade(nextTradeSerial++, Trade.Kind.MINT,
                    resting.getUserName(), null, otherOption, minted, restingPrice, restingCommission));
            result.addTrade(trades.get(trades.size() - 1));
        }
    }

    private void moveShares(String from, String to, int optionIndex, long quantity) {
        long[] owned = holdings.get(from);
        if (owned == null || owned[optionIndex] < quantity) {
            throw new IllegalStateException(from + " does not hold enough shares to sell");
        }
        owned[optionIndex] -= quantity;
        addHolding(to, optionIndex, quantity);
    }

    /**
     * BONUS: takes a reading of every option's price. For an LMSR event that is
     * the formula's price; for an order book it is the last traded price, which
     * is the only price that actually happened.
     */
    private void recordPrices() {
        priceStep++;
        if (type == EventType.LMSR) {
            double[] prices = currentPrices();
            for (int i = 0; i < prices.length; i++) {
                priceHistory.get(i).add(new PricePoint(priceStep, prices[i]));
            }
            return;
        }
        for (int i = 0; i < books.size(); i++) {
            Double last = books.get(i).getLast();
            if (last != null) {
                priceHistory.get(i).add(new PricePoint(priceStep, last));
            }
        }
    }

    /** BONUS: the price readings for one option, oldest first. */
    public List<PricePoint> getPriceHistory(int optionIndex) {
        validateOptionIndex(optionIndex);
        return Collections.unmodifiableList(priceHistory.get(optionIndex));
    }

    private void requireOrderBook() {
        if (orderBookConfig == null) {
            throw new IllegalStateException(
                    "Event " + id + " is an LMSR event, so shares are bought directly");
        }
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
