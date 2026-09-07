package guessmarket.engine.api;

import guessmarket.engine.model.CloseOutcome;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.EventOption;
import guessmarket.engine.model.EventStatus;
import guessmarket.engine.model.EventType;
import guessmarket.engine.model.InsufficientFundsException;
import guessmarket.engine.model.MarketSystem;
import guessmarket.engine.model.Transaction;
import guessmarket.engine.model.OrderBookConfig;
import guessmarket.engine.model.User;
import guessmarket.engine.orderbook.MatchResult;
import guessmarket.engine.orderbook.Order;
import guessmarket.engine.orderbook.OrderBook;
import guessmarket.engine.orderbook.OrderSide;
import guessmarket.engine.orderbook.Trade;
import guessmarket.engine.persistence.SystemStateStore;
import guessmarket.engine.xml.XmlLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds at most one loaded MarketSystem, and converts domain objects into DTOs
 * and domain exceptions into EngineException on the way out.
 *
 * Money that crosses between a user and an event is moved here, because an
 * event does not know about users and a user does not know about events.
 * Nothing here prints, and nothing here returns a domain object.
 */
public class GuessMarketEngineImpl implements GuessMarketEngine {

    private final XmlLoader loader = new XmlLoader();
    private final SystemStateStore stateStore = new SystemStateStore();

    private MarketSystem system = null;

    @Override
    public boolean isLoaded() {
        return system != null;
    }

    /**
     * If the loader throws, the assignment below is never reached and the
     * system loaded before stays in place.
     */
    @Override
    public int loadFile(String path) {
        MarketSystem candidate = loader.load(path);
        this.system = candidate;
        return candidate.getEvents().size();
    }

    @Override
    public List<EventDto> listEvents() {
        MarketSystem loaded = requireLoaded();
        List<EventDto> result = new ArrayList<>();
        for (Event event : loaded.getEvents()) {
            result.add(toDto(event));
        }
        return result;
    }

    @Override
    public List<UserDto> listUsers() {
        MarketSystem loaded = requireLoaded();
        List<UserDto> result = new ArrayList<>();
        for (User user : loaded.getUsers()) {
            result.add(new UserDto(
                    user.getName(),
                    user.getAccount().getBalance(),
                    user.isMarketMaker(),
                    new ArrayList<>(user.getMarketMakerFor())));
        }
        return result;
    }

    @Override
    public EventStateDto eventState(int eventId) {
        return toStateDto(findEvent(eventId));
    }

    @Override
    public UserStateDto userState(String userName) {
        MarketSystem loaded = requireLoaded();
        User user = findUser(userName);

        List<EventRoleDto> roles = new ArrayList<>();
        for (Event event : loaded.getEvents()) {
            boolean isMaker = event.isMarketMaker(userName);
            List<HoldingDto> held = holdingsOf(event, userName);
            if (!isMaker && held.isEmpty()) {
                continue;
            }
            roles.add(new EventRoleDto(
                    event.getId(),
                    event.getName(),
                    event.getStatus().getDisplay(),
                    isMaker,
                    held));
        }

        return new UserStateDto(
                user.getName(),
                user.getAccount().getBalance(),
                user.isMarketMaker(),
                roles);
    }

    private List<HoldingDto> holdingsOf(Event event, String userName) {
        List<HoldingDto> result = new ArrayList<>();
        List<EventOption> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            long shares = event.sharesHeldBy(userName, i);
            if (shares > 0) {
                result.add(new HoldingDto(userName, options.get(i).getName(), shares));
            }
        }
        return result;
    }

    @Override
    public QuoteDto quote(int eventId, int optionIndex, long quantity) {
        Event event = findEvent(eventId);
        try {
            double shareCost = event.quoteShares(optionIndex, quantity);
            return new QuoteDto(shareCost, event.commissionOnPurchase(shareCost));
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }
    }

    @Override
    public OpenReceipt openEvent(int eventId, String userName) {
        Event event = findEvent(eventId);
        User user = findUser(userName);

        if (!event.isMarketMaker(userName)) {
            throw new EngineException("Only " + describeMaker(event)
                    + " can open \"" + event.getName() + "\".");
        }
        if (event.getStatus() != EventStatus.NOT_STARTED) {
            throw new EngineException("\"" + event.getName() + "\" is already "
                    + event.getStatus().getDisplay().toLowerCase() + ".");
        }

        double cost = event.openingCost();
        if (!user.getAccount().canAfford(cost)) {
            throw new EngineException(String.format(
                    "%s needs %.2f to open \"%s\" but only has %.2f.",
                    userName, cost, event.getName(), user.getAccount().getBalance()));
        }

        try {
            user.getAccount().withdraw(cost);
            event.open(cost);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }

        return new OpenReceipt(userName, cost, user.getAccount().getBalance(), toStateDto(event));
    }

    @Override
    public PurchaseReceipt buy(int eventId, String userName, int optionIndex, long quantity) {
        Event event = findEvent(eventId);
        User user = findUser(userName);

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new EngineException("\"" + event.getName() + "\" is not open for trading. It is "
                    + event.getStatus().getDisplay().toLowerCase() + ".");
        }
        if (event.getType() != EventType.LMSR) {
            throw new EngineException("\"" + event.getName()
                    + "\" is an order book event, so shares are traded by placing orders.");
        }

        double shareCost;
        double commission;
        try {
            shareCost = event.quoteShares(optionIndex, quantity);
            commission = event.commissionOnPurchase(shareCost);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }

        double total = shareCost + commission;
        if (!user.getAccount().canAfford(total)) {
            throw new EngineException(String.format(
                    "%s needs %.2f for this purchase but only has %.2f.",
                    userName, total, user.getAccount().getBalance()));
        }

        Transaction transaction;
        try {
            user.getAccount().withdraw(total);
            transaction = event.buy(userName, optionIndex, quantity);
        } catch (InsufficientFundsException e) {
            throw new EngineException(userName + " does not have enough money: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }

        return new PurchaseReceipt(
                userName,
                transaction.optionName(),
                transaction.quantity(),
                transaction.shareCost(),
                transaction.commission(),
                user.getAccount().getBalance(),
                toStateDto(event));
    }


    @Override
    public OrderReceipt placeOrder(int eventId, String userName, int optionIndex,
                                   String side, long quantity, double price) {
        Event event = findEvent(eventId);
        User user = findUser(userName);

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new EngineException("\"" + event.getName() + "\" is not open for trading. It is "
                    + event.getStatus().getDisplay().toLowerCase() + ".");
        }
        if (event.getType() != EventType.ORDER_BOOK) {
            throw new EngineException("\"" + event.getName()
                    + "\" is an LMSR event, so shares are bought directly rather than by order.");
        }

        OrderSide orderSide = OrderSide.BUY.getDisplay().equalsIgnoreCase(side)
                ? OrderSide.BUY : OrderSide.SELL;

        checkTheyCanCoverIt(event, user, optionIndex, orderSide, quantity, price);

        MatchResult result;
        try {
            result = event.submitOrder(userName, optionIndex, orderSide, quantity, price);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }
        applyMoney(event, result);

        List<TradeDto> trades = new ArrayList<>();
        for (Trade trade : result.getTrades()) {
            trades.add(toDto(trade));
        }

        return new OrderReceipt(
                userName,
                orderSide.getDisplay(),
                event.getOptions().get(optionIndex).getName(),
                quantity,
                result.getRestingQuantity(),
                trades,
                user.getAccount().getBalance(),
                toStateDto(event));
    }

    /**
     * An order must be covered before it is accepted, counting what the user
     * already has tied up in orders that are still resting. That way a match
     * can never fail halfway through for want of money or shares.
     */
    private void checkTheyCanCoverIt(Event event, User user, int optionIndex,
                                     OrderSide side, long quantity, double price) {
        if (side == OrderSide.BUY) {
            double needed = quantity * price + event.commissionOnPurchase(quantity * price);
            double committed = event.cashCommittedBy(user.getName());
            double available = user.getAccount().getBalance() - committed;
            if (needed > available + 0.000001) {
                throw new EngineException(String.format(
                        "%s needs %.2f for this order but only has %.2f available%s.",
                        user.getName(), needed, Math.max(0, available),
                        committed > 0 ? String.format(" (%.2f is tied up in orders already placed)", committed) : ""));
            }
            return;
        }

        long held = event.sharesHeldBy(user.getName(), optionIndex);
        long committed = event.sharesCommittedBy(user.getName(), optionIndex);
        long available = held - committed;
        if (quantity > available) {
            throw new EngineException(String.format(
                    "%s wants to sell %d shares of %s but only has %d available%s.",
                    user.getName(), quantity,
                    event.getOptions().get(optionIndex).getName(), Math.max(0, available),
                    committed > 0 ? " (" + committed + " are already offered for sale)" : ""));
        }
    }

    /** Applies everything the match worked out, in one place. */
    private void applyMoney(Event event, MatchResult result) {
        for (Map.Entry<String, Double> entry : result.getCashDelta().entrySet()) {
            double amount = entry.getValue();
            User user = findUser(entry.getKey());
            if (amount < 0) {
                user.getAccount().withdraw(-amount);
            } else if (amount > 0) {
                user.getAccount().deposit(amount);
            }
        }
        if (result.getIntoEventAccount() > 0) {
            event.getAccount().deposit(result.getIntoEventAccount());
        }
        if (result.getCommissionToMarketMaker() > 0) {
            findUser(event.getMarketMakerName()).getAccount()
                    .deposit(result.getCommissionToMarketMaker());
            event.addCommissionCollected(result.getCommissionToMarketMaker());
        }
    }

    private List<OrderDto> toOrderDtos(List<Order> orders) {
        List<OrderDto> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(new OrderDto(
                    order.getSerial(),
                    order.getUserName(),
                    order.getSide().getDisplay(),
                    order.getRemaining(),
                    order.getQuantity(),
                    order.getPrice()));
        }
        return result;
    }

    private TradeDto toDto(Trade trade) {
        return new TradeDto(
                trade.serial(),
                trade.kind() == Trade.Kind.MINT ? "Mint" : "Match",
                trade.buyerName(),
                trade.sellerName(),
                trade.optionName(),
                trade.quantity(),
                trade.price(),
                trade.commission());
    }

    @Override
    public CloseReceipt closeEvent(int eventId, String userName, int winningOptionIndex) {
        Event event = findEvent(eventId);
        findUser(userName);

        if (!event.isMarketMaker(userName)) {
            throw new EngineException("Only " + describeMaker(event)
                    + " can close \"" + event.getName() + "\".");
        }

        CloseOutcome outcome;
        try {
            outcome = event.close(winningOptionIndex);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }

        // The winners are paid, then the commission and the leftover go to the MM.
        List<PayoutDto> payouts = new ArrayList<>();
        for (Map.Entry<String, Double> entry : outcome.payoutsByUser().entrySet()) {
            findUser(entry.getKey()).getAccount().deposit(entry.getValue());
            payouts.add(new PayoutDto(entry.getKey(), entry.getValue()));
        }
        findUser(event.getMarketMakerName()).getAccount().deposit(outcome.returnedToMarketMaker());

        return new CloseReceipt(
                outcome.winningOptionName(),
                outcome.winningShares(),
                outcome.grossPayout(),
                outcome.commission(),
                outcome.netPaidToWinners(),
                outcome.returnedToMarketMaker(),
                payouts,
                toStateDto(event));
    }

    @Override
    public void saveState(String pathWithoutExtension) {
        stateStore.save(pathWithoutExtension, requireLoaded());
    }

    /** As in loadFile, the assignment happens only if the restore succeeded. */
    @Override
    public int loadState(String pathWithoutExtension) {
        MarketSystem restored = stateStore.load(pathWithoutExtension);
        this.system = restored;
        return restored.getEvents().size();
    }

    private String describeMaker(Event event) {
        String maker = event.getMarketMakerName();
        return maker == null ? "the market maker" : maker;
    }

    private MarketSystem requireLoaded() {
        if (system == null) {
            throw new EngineException("No file is loaded. Load a file first.");
        }
        return system;
    }

    private Event findEvent(int eventId) {
        try {
            return requireLoaded().getEvent(eventId);
        } catch (EngineException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }
    }

    private User findUser(String userName) {
        try {
            return requireLoaded().getUser(userName);
        } catch (EngineException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }
    }

    private EventDto toDto(Event event) {
        List<String> optionNames = new ArrayList<>();
        for (EventOption option : event.getOptions()) {
            optionNames.add(option.getName());
        }
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPercent(),
                event.getCommissionType().getDisplay(),
                optionNames,
                event.getStatus().getDisplay(),
                event.getType().getDisplay(),
                event.getMarketMakerName(),
                event.getAccount().getBalance());
    }

    private EventStateDto toStateDto(Event event) {
        double[] prices = event.currentPrices();
        List<EventOption> options = event.getOptions();

        List<OptionStateDto> optionStates = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            optionStates.add(new OptionStateDto(
                    options.get(i).getName(),
                    prices[i],
                    options.get(i).getSharesBought()));
        }

        List<TransactionDto> history = new ArrayList<>();
        for (Transaction transaction : event.getHistoryNewestFirst()) {
            history.add(new TransactionDto(
                    transaction.serial(),
                    transaction.userName(),
                    transaction.optionName(),
                    transaction.quantity(),
                    transaction.shareCost(),
                    transaction.commission()));
        }

        List<HoldingDto> participations = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : event.getHoldings().entrySet()) {
            for (int i = 0; i < options.size(); i++) {
                if (entry.getValue()[i] > 0) {
                    participations.add(new HoldingDto(
                            entry.getKey(), options.get(i).getName(), entry.getValue()[i]));
                }
            }
        }

        List<BookDto> books = new ArrayList<>();
        for (int i = 0; i < event.getBooks().size(); i++) {
            OrderBook book = event.getBooks().get(i);
            books.add(new BookDto(
                    book.getOptionName(),
                    book.getLast(),
                    book.getBestBid(),
                    book.getBestAsk(),
                    book.getMid(),
                    book.getSpread(),
                    toOrderDtos(book.getBids()),
                    toOrderDtos(book.getAsks()),
                    options.get(i).getSharesBought()));
        }

        List<TradeDto> trades = new ArrayList<>();
        for (Trade trade : event.getTradesNewestFirst()) {
            trades.add(toDto(trade));
        }

        OrderBookConfig config = event.getOrderBookConfig();

        return new EventStateDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getType().getDisplay(),
                event.getStatus().getDisplay(),
                event.getMarketMakerName(),
                event.getCommissionPercent(),
                event.getCommissionType().getDisplay(),
                optionStates,
                event.getAccount().getBalance(),
                event.getCommissionCollected(),
                history,
                participations,
                event.getWinningOptionName(),
                event.openingCost(),
                books,
                trades,
                config == null ? null : (double) config.d(),
                config != null && config.allowMint());
    }
}
