package guessmarket.engine.api;

import guessmarket.engine.model.CloseOutcome;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.EventOption;
import guessmarket.engine.model.EventStatus;
import guessmarket.engine.model.EventType;
import guessmarket.engine.model.InsufficientFundsException;
import guessmarket.engine.model.MarketSystem;
import guessmarket.engine.model.Transaction;
import guessmarket.engine.model.User;
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
                event.openingCost());
    }
}
