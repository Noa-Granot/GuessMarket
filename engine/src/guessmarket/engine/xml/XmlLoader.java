package guessmarket.engine.xml;

import guessmarket.engine.api.LoadException;
import guessmarket.engine.model.CommissionType;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.MarketSystem;
import guessmarket.engine.xml.generated.Comision;
import guessmarket.engine.xml.generated.GMEvent;
import guessmarket.engine.xml.generated.GMLMSR;
import guessmarket.engine.xml.generated.GMOptions;
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
 * Turns an XML file on disk into a MarketSystem, or explains in plain sentences
 * why it could not.
 *
 * Three things about the shape of this class are deliberate:
 *
 * 1. It returns a brand new MarketSystem and never touches the one the engine is
 *    already holding. That is the whole mechanism behind "a broken file must not
 *    overwrite the good file already loaded" -- the engine simply does not
 *    assign the result unless this method returns normally.
 *
 * 2. Validation runs to completion and collects every problem before throwing.
 *    Reporting one mistake at a time would make fixing a bad file tedious.
 *
 * 3. The generated JAXB classes are read here and nowhere else. They are an
 *    accident of the file format, so they stop at this boundary and the rest of
 *    the engine only ever sees Event, EventOption and friends.
 */
public class XmlLoader {

    private static final int REQUIRED_OPTION_COUNT = 2;
    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;

    /**
     * @param rawPath full path to the file, as typed by the user
     * @return a fully built, fully validated system
     * @throws LoadException if anything at all is wrong
     */
    public MarketSystem load(String rawPath) {
        File file = resolveFile(rawPath);
        GuessMarket root = unmarshal(file);
        List<GMEvent> rawEvents = extractEvents(root);

        List<String> problems = validate(rawEvents);
        if (!problems.isEmpty()) {
            throw new LoadException(
                    "The file was read but its contents are not valid, so nothing was loaded.",
                    problems);
        }

        return build(rawEvents);
    }

    // ---------- step 1: is there a file at all ----------

    private File resolveFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new LoadException("No path was entered.");
        }

        String path = rawPath.trim();
        // A path pasted from Windows Explorer often arrives wrapped in quotes.
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

    // ---------- step 3: is the content sane ----------

    private List<String> validate(List<GMEvent> rawEvents) {
        List<String> problems = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();
        Set<Integer> reportedDuplicates = new HashSet<>();

        for (int i = 0; i < rawEvents.size(); i++) {
            GMEvent raw = rawEvents.get(i);
            String where = describe(raw, i);

            if (!seenIds.add(raw.getId()) && reportedDuplicates.add(raw.getId())) {
                problems.add(where + ": the event number " + raw.getId()
                        + " is used by more than one event. Every event needs its own number.");
            }

            validateName(raw, where, problems);
            validateCommission(raw.getComision(), where, problems);
            validateOptions(raw.getGMOptions(), where, problems);
            validateMethod(raw, where, problems);
        }
        return problems;
    }

    private void validateName(GMEvent raw, String where, List<String> problems) {
        if (isBlank(raw.getName())) {
            problems.add(where + ": the event has no name. The name attribute cannot be empty.");
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

    private void validateMethod(GMEvent raw, String where, List<String> problems) {
        if (raw.getGMMethod() == null || raw.getGMMethod().getGMLMSR() == null) {
            problems.add(where + ": the trading method is missing. Exercise 1 events must define GM-LMSR.");
            return;
        }
        GMLMSR lmsr = raw.getGMMethod().getGMLMSR();
        if (lmsr.getB() <= 0) {
            problems.add(where + ": the liquidity value b is " + lmsr.getB()
                    + ", but it has to be greater than zero.");
        }
    }

    /** Identifies an event in a message even when its id or name is missing. */
    private String describe(GMEvent raw, int index) {
        String label = "Event number " + (index + 1) + " in the file (id " + raw.getId() + ")";
        if (!isBlank(raw.getName())) {
            label = "Event number " + (index + 1) + " in the file, \"" + raw.getName().trim()
                    + "\" (id " + raw.getId() + ")";
        }
        return label;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ---------- step 4: build the real thing ----------

    /**
     * Only ever called once validation has passed, so the guards inside Event
     * and MarketSystem are a safety net here rather than the error path.
     */
    private MarketSystem build(List<GMEvent> rawEvents) {
        MarketSystem system = new MarketSystem();

        for (GMEvent raw : rawEvents) {
            List<String> optionNames = new ArrayList<>();
            for (String name : raw.getGMOptions().getGMOption()) {
                optionNames.add(name.trim());
            }

            system.addEvent(new Event(
                    raw.getId(),
                    raw.getName().trim(),
                    raw.getDescription() == null ? "" : raw.getDescription().trim(),
                    raw.getComision().getValue(),
                    CommissionType.fromXml(raw.getComision().getType()),
                    optionNames,
                    raw.getGMMethod().getGMLMSR().getB()));
        }

        system.paySubsidies();
        return system;
    }
}
