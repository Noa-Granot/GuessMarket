package guessmarket.engine.api;

import guessmarket.engine.model.CloseOutcome;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.EventOption;
import guessmarket.engine.model.MarketSystem;
import guessmarket.engine.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * The engine. Holds at most one loaded MarketSystem at a time, translates
 * domain objects into DTOs on the way out, and translates domain exceptions
 * into EngineException on the way out.
 *
 * Nothing here prints. Nothing here returns a mutable domain object.
 */
public class GuessMarketEngineImpl implements GuessMarketEngine {

    private MarketSystem system = null;

    @Override
    public boolean isLoaded() {
        return system != null;
    }

    @Override
    public void loadDemoData() {
        MarketSystem candidate = DemoData.build();
        candidate.paySubsidies();
        this.system = candidate;
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
    public List<EventDto> listActiveEvents() {
        MarketSystem loaded = requireLoaded();
        List<EventDto> result = new ArrayList<>();
        for (Event event : loaded.getEvents()) {
            if (event.getStatus() == guessmarket.engine.model.EventStatus.ACTIVE) {
                result.add(toDto(event));
            }
        }
        return result;
    }

    @Override
    public EventStateDto eventState(int eventId) {
        return toStateDto(findEvent(eventId));
    }

    @Override
    public double quote(int eventId, int optionIndex, long quantity) {
        Event event = findEvent(eventId);
        try {
            return event.quote(optionIndex, quantity);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }
    }

    @Override
    public PurchaseReceipt buy(int eventId, int optionIndex, long quantity) {
        Event event = findEvent(eventId);
        Transaction transaction;
        try {
            transaction = event.buy(optionIndex, quantity);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }
        return new PurchaseReceipt(
                transaction.optionName(),
                transaction.quantity(),
                transaction.shareCost(),
                transaction.commission(),
                toStateDto(event));
    }

    @Override
    public CloseReceipt close(int eventId, int winningOptionIndex) {
        MarketSystem loaded = requireLoaded();
        Event event = findEvent(eventId);
        CloseOutcome outcome;
        try {
            outcome = event.close(winningOptionIndex);
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(), e);
        }
        loaded.getManagerAccount().deposit(outcome.leftoverForManager());
        return new CloseReceipt(
                outcome.winningOptionName(),
                outcome.grossPayout(),
                outcome.commission(),
                outcome.netPayout(),
                outcome.leftoverForManager(),
                toStateDto(event));
    }

    @Override
    public double managerBalance() {
        return requireLoaded().getManagerAccount().getBalance();
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
                event.getStatus().getDisplay());
    }

    private EventStateDto toStateDto(Event event) {
        double[] prices = event.currentPrices();
        List<OptionStateDto> optionStates = new ArrayList<>();
        List<EventOption> options = event.getOptions();
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
                    transaction.optionName(),
                    transaction.quantity(),
                    transaction.shareCost(),
                    transaction.commission()));
        }

        return new EventStateDto(
                event.getId(),
                event.getName(),
                optionStates,
                event.getAccount().getBalance(),
                event.getCommissionCollected(),
                history,
                event.getStatus().getDisplay(),
                event.getWinningOptionName());
    }
}
