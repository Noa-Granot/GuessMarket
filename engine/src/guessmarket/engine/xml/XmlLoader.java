package guessmarket.engine.xml;

import guessmarket.engine.api.LoadException;
import guessmarket.engine.model.CommissionType;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.MarketSystem;
import guessmarket.engine.model.OrderBookConfig;
import guessmarket.engine.model.User;
import guessmarket.engine.xml.generated.Comision;
import guessmarket.engine.xml.generated.EventRef;
import guessmarket.engine.xml.generated.GMEvent;
import guessmarket.engine.xml.generated.GMLMSR;
import guessmarket.engine.xml.generated.GMMethod;
import guessmarket.engine.xml.generated.GMOptions;
import guessmarket.engine.xml.generated.GMOrderBook;
import guessmarket.engine.xml.generated.GMUser;
import guessmarket.engine.xml.generated.GuessMarket;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns an XML file into a MarketSystem, or reports why it could not.
 * It returns a new MarketSystem and never touches the one the engine already
 * holds, so a broken file cannot overwrite a good one. Validation collects
 * every problem before throwing, so a file can be fixed in one pass.
 * The JAXB classes are read here and nowhere else.
 */
public class XmlLoader {

    private static final int REQUIRED_OPTION_COUNT = 2;
    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;

    public MarketSystem load(String rawPath) {
        File file = resolveFile(rawPath);
        GuessMarket root = unmarshal(file);

        List<GMEvent> rawEvents = extractEvents(root);
        List<GMUser> rawUsers = extractUsers(root);

        List<String> problems = new ArrayList<>();
        validateEvents(rawEvents, problems);
        validateUsers(rawUsers, problems);
        validateMarketMakers(rawEvents, rawUsers, problems);

        if (!problems.isEmpty()) {
            throw new LoadException(
                    "The file was read but its contents are not valid, so nothing was loaded.",
                    problems);
        }

        return build(rawEvents, rawUsers);
    }

    // ---------- step 1: is there a file at all ----------

    private File resolveFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new LoadException("No path was entered.");
        }

        String path = rawPath.trim();
        // A path copied from Explorer can arrive wrapped in quotes.
        if (path.length() >= 2 && path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1).trim();
        }

        if (!path.toLowerCase().endsWith(".xml")) {
            throw new LoadException("The file must be an XML file, so its name has to end with .xml. "
                    + "The path entered was: " + path);
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new LoadException("No file was found at: " + path);
        }
        if (file.isDirectory()) {
            throw new LoadException("That path is a folder, not a file: " + path);
        }
        if (!file.canRead()) {
            throw new LoadException("The file exists but cannot be read: " + path);
        }
        return file;
    }

    // ---------- step 2: parse it ----------

    private GuessMarket unmarshal(File file) {
        try {
            JAXBContext context = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object result = unmarshaller.unmarshal(file);
            if (!(result instanceof GuessMarket guessMarket)) {
                throw new LoadException("The file is XML, but it is not a Guess Market file. "
                        + "The outermost element should be Guess-Market.");
            }
            return guessMarket;
        } catch (JAXBException e) {
            throw new LoadException("The file could not be read as XML. "
                    + describeCause(e) + " Check that the file is not damaged and that every tag is closed.", e);
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
                    + "A Guess Market file needs at least one GM-event inside GM-events.");
        }
        return root.getGMEvents().getGMEvent();
    }

    private List<GMUser> extractUsers(GuessMarket root) {
        if (root.getGMUsers() == null || root.getGMUsers().getGMUser() == null
                || root.getGMUsers().getGMUser().isEmpty()) {
            throw new LoadException("The file does not define any users. "
                    + "A Guess Market file needs at least one GM-user inside GM-users.");
        }
        return root.getGMUsers().getGMUser();
    }

    // ---------- step 3: is the content sane ----------

    private void validateEvents(List<GMEvent> rawEvents, List<String> problems) {
        Set<Integer> seenIds = new HashSet<>();
        Set<Integer> reportedDuplicates = new HashSet<>();

        for (int i = 0; i < rawEvents.size(); i++) {
            GMEvent raw = rawEvents.get(i);
            String where = describeEvent(raw, i);

            if (!seenIds.add(raw.getId()) && reportedDuplicates.add(raw.getId())) {
                problems.add(where + ": the event number " + raw.getId()
                        + " is used by more than one event. Every event needs its own number.");
            }

            if (isBlank(raw.getName())) {
                problems.add(where + ": the event has no name. The name attribute cannot be empty.");
            }
            validateCommission(raw.getComision(), where, problems);
            validateOptions(raw.getGMOptions(), where, problems);
            validateMethod(raw.getGMMethod(), where, problems);
        }
    }

    private void validateCommission(Comision comision, String where, List<String> problems) {
        if (comision == null) {
            problems.add(where + ": the comision element is missing.");
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
                    + "\", but the only accepted values are on-purchase and on-close.");
        }
    }

    private boolean isKnownCommissionType(String type) {
        try {
            CommissionType.fromXml(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void validateOptions(GMOptions options, String where, List<String> problems) {
        if (options == null || options.getGMOption() == null || options.getGMOption().isEmpty()) {
            problems.add(where + ": the event has no options. It needs exactly "
                    + REQUIRED_OPTION_COUNT + ".");
            return;
        }

        List<String> names = options.getGMOption();
        if (names.size() != REQUIRED_OPTION_COUNT) {
            problems.add(where + ": the event has " + names.size() + " option"
                    + (names.size() == 1 ? "" : "s") + ", but every event needs exactly "
                    + REQUIRED_OPTION_COUNT + ".");
        }

        for (int i = 0; i < names.size(); i++) {
            if (isBlank(names.get(i))) {
                problems.add(where + ": option number " + (i + 1) + " has no name.");
            }
        }
    }

    private void validateMethod(GMMethod method, String where, List<String> problems) {
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
        if (orderBook.getInital() <= 0) {
            problems.add(where + ": the initial share count is " + orderBook.getInital()
                    + ", but it has to be greater than zero.");
        }
        String allowMint = orderBook.getAllowMint();
        if (!"true".equalsIgnoreCase(trimmed(allowMint)) && !"false".equalsIgnoreCase(trimmed(allowMint))) {
            problems.add(where + ": allow-mint is \"" + allowMint
                    + "\", but it has to be either true or false.");
        }
    }

    private void validateUsers(List<GMUser> rawUsers, List<String> problems) {
        Set<String> seenNames = new HashSet<>();
        Set<String> reportedDuplicates = new HashSet<>();

        for (int i = 0; i < rawUsers.size(); i++) {
            GMUser raw = rawUsers.get(i);
            String where = describeUser(raw, i);

            if (isBlank(raw.getName())) {
                problems.add(where + ": the user has no name. The name attribute cannot be empty.");
            } else {
                String key = raw.getName().trim().toLowerCase();
                if (!seenNames.add(key) && reportedDuplicates.add(key)) {
                    problems.add(where + ": the name \"" + raw.getName().trim()
                            + "\" is used by more than one user. Every user needs a unique name.");
                }
            }

            if (raw.getInitialCash() <= 0) {
                problems.add(where + ": the starting balance is " + raw.getInitialCash()
                        + ", but every user has to start with more than zero.");
            }
        }
    }

    /**
     * Two rules that need the events and the users together: a market maker
     * cannot point at an event that does not exist, and every event needs
     * exactly one market maker.
     */
    private void validateMarketMakers(List<GMEvent> rawEvents, List<GMUser> rawUsers,
                                      List<String> problems) {
        Set<Integer> eventIds = new HashSet<>();
        for (GMEvent raw : rawEvents) {
            eventIds.add(raw.getId());
        }

        java.util.Map<Integer, List<String>> makersByEvent = new java.util.LinkedHashMap<>();

        for (int i = 0; i < rawUsers.size(); i++) {
            GMUser raw = rawUsers.get(i);
            if (raw.getGMMareketMaker() == null) {
                continue;
            }
            String who = isBlank(raw.getName()) ? describeUser(raw, i) : raw.getName().trim();

            for (EventRef ref : raw.getGMMareketMaker().getEvent()) {
                if (!eventIds.contains(ref.getId())) {
                    problems.add("The user \"" + who + "\" is set as market maker of event number "
                            + ref.getId() + ", but no such event exists in the file.");
                    continue;
                }
                makersByEvent.computeIfAbsent(ref.getId(), k -> new ArrayList<>()).add(who);
            }
        }

        for (GMEvent raw : rawEvents) {
            List<String> makers = makersByEvent.getOrDefault(raw.getId(), List.of());
            if (makers.isEmpty()) {
                problems.add("Event number " + raw.getId() + " has no market maker. "
                        + "Exactly one user must be set as its market maker.");
            } else if (makers.size() > 1) {
                problems.add("Event number " + raw.getId() + " has more than one market maker ("
                        + String.join(", ", makers) + "). Exactly one user must be set as its market maker.");
            }
        }
    }

    private String describeEvent(GMEvent raw, int index) {
        if (!isBlank(raw.getName())) {
            return "Event number " + (index + 1) + " in the file, \"" + raw.getName().trim()
                    + "\" (id " + raw.getId() + ")";
        }
        return "Event number " + (index + 1) + " in the file (id " + raw.getId() + ")";
    }

    private String describeUser(GMUser raw, int index) {
        if (!isBlank(raw.getName())) {
            return "User number " + (index + 1) + " in the file, \"" + raw.getName().trim() + "\"";
        }
        return "User number " + (index + 1) + " in the file";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    // ---------- step 4: build the real thing ----------

    /** Called only after validation passed. */
    private MarketSystem build(List<GMEvent> rawEvents, List<GMUser> rawUsers) {
        MarketSystem system = new MarketSystem();

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
                event = Event.lmsr(raw.getId(), name, description, percent, commissionType,
                        optionNames, raw.getGMMethod().getGMLMSR().getB());
            } else {
                GMOrderBook ob = raw.getGMMethod().getGMOrderBook();
                event = Event.orderBook(raw.getId(), name, description, percent, commissionType,
                        optionNames,
                        new OrderBookConfig(ob.getInital(), ob.getD(),
                                Boolean.parseBoolean(trimmed(ob.getAllowMint()))));
            }
            system.addEvent(event);
        }

        for (GMUser raw : rawUsers) {
            User user = new User(raw.getName().trim(), raw.getInitialCash());
            if (raw.getGMMareketMaker() != null) {
                for (EventRef ref : raw.getGMMareketMaker().getEvent()) {
                    user.addMarketMakerEvent(ref.getId());
                    system.getEvent(ref.getId()).setMarketMakerName(user.getName());
                }
            }
            system.addUser(user);
        }

        return system;
    }
}
