package guessmarket.engine.xml.ex3;

import guessmarket.engine.api.LoadException;
import guessmarket.engine.model.CommissionType;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.OrderBookConfig;
import guessmarket.engine.xml.ex3.generated.Comision;
import guessmarket.engine.xml.ex3.generated.GMEvent;
import guessmarket.engine.xml.ex3.generated.GMLMSR;
import guessmarket.engine.xml.ex3.generated.GMMethod;
import guessmarket.engine.xml.ex3.generated.GMOptions;
import guessmarket.engine.xml.ex3.generated.GMOrderBook;
import guessmarket.engine.xml.ex3.generated.GuessMarket;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads an exercise 3 file and returns the events it describes.
 *
 * Three things separate this from the exercise 1 and 2 loader:
 *
 *   - it reads a stream, never a path. The exercise says the uploaded file must
 *     not be written to the server's disk, and a loader that cannot open a file
 *     cannot accidentally do so.
 *   - it returns events instead of a whole MarketSystem, because exercise 3
 *     adds to the market rather than replacing it.
 *   - the file has no users and no event ids. The uploader is the market maker
 *     of every event in the file, and the caller supplies the first id.
 *
 * As before, nothing is returned unless every event is valid, so a file with
 * one bad event adds none of its events. Validation collects every problem
 * before throwing, so one upload reports every mistake.
 */
public class Ex3XmlLoader {

    /** The exercise asks for events with more than two options, so this is a floor. */
    private static final int MIN_OPTION_COUNT = 2;
    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;

    /**
     * @param in                 the uploaded content. The caller closes it.
     * @param uploaderName       becomes the market maker of every event here.
     * @param firstEventId       the id given to the first event; the rest follow it.
     * @param existingEventNames names already used in the market, compared without case.
     * @return the events, in file order, market maker already set.
     */
    public List<Event> load(InputStream in, String uploaderName, int firstEventId,
                            Collection<String> existingEventNames) {

        if (uploaderName == null || uploaderName.isBlank()) {
            throw new LoadException("Only a logged in user can upload a file.");
        }

        GuessMarket root = unmarshal(in);
        List<GMEvent> rawEvents = extractEvents(root);

        List<String> problems = new ArrayList<>();
        validateEvents(rawEvents, existingEventNames, problems);

        if (!problems.isEmpty()) {
            throw new LoadException(
                    "The file was read but its contents are not valid, so nothing was added.",
                    problems);
        }

        return build(rawEvents, uploaderName.trim(), firstEventId);
    }

    // ---------- step 1: parse it ----------

    private GuessMarket unmarshal(InputStream in) {
        if (in == null) {
            throw new LoadException("No file content arrived at the server.");
        }
        try {
            JAXBContext context = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object result = unmarshaller.unmarshal(in);
            if (!(result instanceof GuessMarket guessMarket)) {
                throw new LoadException("The file is XML, but it is not a Guess Market file. "
                        + "The outermost element should be Guess-Market.");
            }
            return guessMarket;
        } catch (JAXBException e) {
            throw new LoadException("The file could not be read as XML. "
                    + describeCause(e)
                    + " Check that the file is not damaged and that every tag is closed.", e);
        }
    }

    private String describeCause(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return (message == null || message.isBlank()) ? "" : "Details: " + message.trim();
    }

    private List<GMEvent> extractEvents(GuessMarket root) {
        if (root.getGMEvents() == null || root.getGMEvents().getGMEvent() == null
                || root.getGMEvents().getGMEvent().isEmpty()) {
            throw new LoadException("The file does not define any events. "
                    + "An exercise 3 file needs at least one GM-event inside GM-events.");
        }
        return root.getGMEvents().getGMEvent();
    }

    // ---------- step 2: is the content sane ----------

    private void validateEvents(List<GMEvent> rawEvents, Collection<String> existingEventNames,
                                List<String> problems) {

        Set<String> alreadyInMarket = lowercased(existingEventNames);
        Set<String> seenInFile = new HashSet<>();
        Set<String> reportedDuplicates = new HashSet<>();

        for (int i = 0; i < rawEvents.size(); i++) {
            GMEvent raw = rawEvents.get(i);
            String where = describeEvent(raw, i);

            if (isBlank(raw.getName())) {
                problems.add(where + ": the event has no name. The name attribute cannot be empty.");
            } else {
                String key = raw.getName().trim().toLowerCase();
                if (alreadyInMarket.contains(key)) {
                    problems.add(where + ": an event named \"" + raw.getName().trim()
                            + "\" is already in the market. Event names have to be unique.");
                } else if (!seenInFile.add(key) && reportedDuplicates.add(key)) {
                    problems.add(where + ": the name \"" + raw.getName().trim()
                            + "\" is used by more than one event in this file.");
                }
            }

            if (isBlank(raw.getDescription())) {
                problems.add(where + ": the event has no description.");
            }

            validateComision(raw.getComision(), where, problems);
            int optionCount = validateOptions(raw.getGMOptions(), where, problems);
            validateMethod(raw.getGMMethod(), optionCount, where, problems);
        }
    }

    private void validateComision(Comision comision, String where, List<String> problems) {
        if (comision == null) {
            problems.add(where + ": the commission element is missing.");
            return;
        }

        int percent = comision.getValue();
        if (percent < MIN_COMMISSION || percent > MAX_COMMISSION) {
            problems.add(where + ": the commission is " + percent
                    + ", but it has to be between " + MIN_COMMISSION + " and " + MAX_COMMISSION + ".");
        }

        String type = comision.getType();
        if (isBlank(type)) {
            problems.add(where + ": the commission has no type. It must be either on-purchase or on-close.");
            return;
        }
        if (!isKnownCommissionType(type)) {
            problems.add(where + ": the commission type is \"" + type.trim()
                    + "\", but it must be either on-purchase or on-close.");
        }
    }

    private boolean isKnownCommissionType(String type) {
        try {
            CommissionType.fromXml(type);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** @return how many options the event declares, so the method check can use it. */
    private int validateOptions(GMOptions options, String where, List<String> problems) {
        if (options == null || options.getGMOption() == null || options.getGMOption().isEmpty()) {
            problems.add(where + ": the event has no options. It needs at least "
                    + MIN_OPTION_COUNT + ".");
            return 0;
        }

        List<String> names = options.getGMOption();
        if (names.size() < MIN_OPTION_COUNT) {
            problems.add(where + ": the event has " + names.size() + " option"
                    + (names.size() == 1 ? "" : "s") + ", but every event needs at least "
                    + MIN_OPTION_COUNT + ".");
        }

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < names.size(); i++) {
            if (isBlank(names.get(i))) {
                problems.add(where + ": option number " + (i + 1) + " has no name.");
            } else if (!seen.add(names.get(i).trim().toLowerCase())) {
                problems.add(where + ": the option \"" + names.get(i).trim()
                        + "\" appears more than once. Every option needs its own name.");
            }
        }
        return names.size();
    }

    private void validateMethod(GMMethod method, int optionCount, String where, List<String> problems) {
        if (method == null) {
            problems.add(where + ": the trading method is missing. "
                    + "An event must define either GM-LMSR or GM-order-book.");
            return;
        }

        GMLMSR lmsr = method.getGMLMSR();
        GMOrderBook orderBook = method.getGMOrderBook();

        if (lmsr == null && orderBook == null) {
            problems.add(where + ": the trading method is empty. "
                    + "An event must define either GM-LMSR or GM-order-book.");
            return;
        }
        if (lmsr != null && orderBook != null) {
            problems.add(where + ": the event defines both GM-LMSR and GM-order-book. "
                    + "It must have exactly one trading method.");
            return;
        }

        if (lmsr != null) {
            if (lmsr.getB() <= 0) {
                problems.add(where + ": the liquidity value b is " + lmsr.getB()
                        + ", but it has to be greater than zero.");
            }
            return;
        }

        if (orderBook.getD() <= 0) {
            problems.add(where + ": the payout value d is " + orderBook.getD()
                    + ", but it has to be greater than zero.");
        }
        // The exercise document allows an initial stock of zero, so only a
        // negative count is a mistake. A missing attribute is still a mistake.
        if (!orderBook.hasInitialShares()) {
            problems.add(where + ": the order book has no initial share count.");
        } else if (orderBook.getInitialShares() < 0) {
            problems.add(where + ": the initial share count is " + orderBook.getInitialShares()
                    + ", but it cannot be negative.");
        }

        String allowMint = trimmed(orderBook.getAllowMint());
        boolean mintTrue = "true".equalsIgnoreCase(allowMint);
        if (!mintTrue && !"false".equalsIgnoreCase(allowMint)) {
            problems.add(where + ": allow-mint is \"" + orderBook.getAllowMint()
                    + "\", but it has to be either true or false.");
            return;
        }

        // Minting pairs a buyer with a buyer of the opposite option, which only
        // means something when there are exactly two options. Rather than mint
        // wrongly, the file is refused.
        if (mintTrue && optionCount > 2) {
            problems.add(where + ": allow-mint is true and the event has " + optionCount
                    + " options. Minting is only supported for events with exactly two options.");
        }
    }

    private Set<String> lowercased(Collection<String> names) {
        Set<String> result = new HashSet<>();
        if (names != null) {
            for (String name : names) {
                if (name != null) {
                    result.add(name.trim().toLowerCase());
                }
            }
        }
        return result;
    }

    private String describeEvent(GMEvent raw, int index) {
        if (!isBlank(raw.getName())) {
            return "Event number " + (index + 1) + " in the file, \"" + raw.getName().trim() + "\"";
        }
        return "Event number " + (index + 1) + " in the file";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    // ---------- step 3: build the real thing ----------

    /** Called only after validation passed. */
    private List<Event> build(List<GMEvent> rawEvents, String uploaderName, int firstEventId) {
        List<Event> built = new ArrayList<>();
        int id = firstEventId;

        for (GMEvent raw : rawEvents) {
            List<String> optionNames = new ArrayList<>();
            for (String name : raw.getGMOptions().getGMOption()) {
                optionNames.add(name.trim());
            }

            String name = raw.getName().trim();
            String description = raw.getDescription() == null ? "" : raw.getDescription().trim();
            int percent = raw.getComision().getValue();
            CommissionType commissionType = CommissionType.fromXml(raw.getComision().getType());

            Event event;
            if (raw.getGMMethod().getGMLMSR() != null) {
                event = Event.lmsr(id, name, description, percent, commissionType,
                        optionNames, raw.getGMMethod().getGMLMSR().getB());
            } else {
                GMOrderBook ob = raw.getGMMethod().getGMOrderBook();
                event = Event.orderBook(id, name, description, percent, commissionType,
                        optionNames,
                        new OrderBookConfig(ob.getInitialShares(), ob.getD(),
                                Boolean.parseBoolean(trimmed(ob.getAllowMint()))));
            }

            // Whoever uploaded the file is the market maker of everything in it.
            event.setMarketMakerName(uploaderName);
            built.add(event);
            id++;
        }

        return built;
    }
}
