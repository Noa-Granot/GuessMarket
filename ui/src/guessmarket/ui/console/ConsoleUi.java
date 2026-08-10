package guessmarket.ui.console;

import guessmarket.engine.api.CloseReceipt;
import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.EventDto;
import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.OptionStateDto;
import guessmarket.engine.api.PurchaseReceipt;
import guessmarket.engine.api.TransactionDto;

import java.util.List;
import java.util.Scanner;

/**
 * The active side of the application: shows the menu, collects input, calls the
 * engine, prints what comes back. Every System.out in the project lives in this
 * package and this package only.
 *
 * The engine works in 0-based option indexes; everything shown to the user here
 * is numbered from 1. The conversion happens at the point of use and nowhere
 * else.
 */
class ConsoleUi {

    private static final String MONEY = "%.2f";

    private final GuessMarketEngine engine;
    private final InputReader input;
    private boolean running = true;

    ConsoleUi(GuessMarketEngine engine) {
        this.engine = engine;
        this.input = new InputReader(new Scanner(System.in));
    }

    void run() {
        System.out.println("Guess Market - console edition");
        System.out.println("Exercise 1");

        while (running) {
            printMenu();
            int choice = input.readIntInRange("Choose a command (1-6): ", 1, 6);
            System.out.println();
            try {
                dispatch(choice);
            } catch (EngineException e) {
                System.out.println("Cannot do that: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("--------------------------------");
        System.out.println("1. Load a system details file");
        System.out.println("2. Show all events");
        System.out.println("3. Show the trading state of an event");
        System.out.println("4. Participate in an event");
        System.out.println("5. Close an event");
        System.out.println("6. Exit");
        System.out.println("--------------------------------");
    }

    private void dispatch(int choice) {
        switch (choice) {
            case 1 -> loadFile();
            case 2 -> showEvents();
            case 3 -> showEventState();
            case 4 -> participate();
            case 5 -> closeEvent();
            case 6 -> exit();
            default -> System.out.println("Unknown command.");
        }
    }

    // ---------- command 1 ----------

    private void loadFile() {
        System.out.println("The XML loader is not built yet.");
        System.out.println("Loading the built-in demo events instead so the other commands can be tried.");
        engine.loadDemoData();
        System.out.println("Loaded " + engine.listEvents().size() + " demo events.");
        System.out.printf("Manager account after paying subsidies: " + MONEY + "%n", engine.managerBalance());
    }

    // ---------- command 2 ----------

    private void showEvents() {
        List<EventDto> events = engine.listEvents();
        System.out.println("Events currently loaded:");
        System.out.println();
        printEventList(events);
    }

    /** Prints the list and returns it, so callers can map a choice back to an id. */
    private void printEventList(List<EventDto> events) {
        for (int i = 0; i < events.size(); i++) {
            EventDto event = events.get(i);
            System.out.println((i + 1) + ") Event number " + event.id() + " - " + event.name());
            System.out.println("     Description: " + event.description());
            System.out.println("     Commission:  " + event.commissionPercent() + "% (" + event.commissionTypeDisplay() + ")");
            System.out.println("     Options:     " + String.join(", ", event.optionNames()));
            System.out.println("     Status:      " + event.statusDisplay());
            System.out.println();
        }
    }

    // ---------- command 3 ----------

    private void showEventState() {
        EventDto chosen = chooseFrom(engine.listEvents(), "Which event's state would you like to see");
        if (chosen == null) {
            return;
        }
        printEventState(engine.eventState(chosen.id()));
    }

    private void printEventState(EventStateDto state) {
        System.out.println("Trading state of event " + state.id() + " - " + state.name());
        System.out.println();
        System.out.println("  Current standing:");
        List<OptionStateDto> options = state.options();
        for (int i = 0; i < options.size(); i++) {
            OptionStateDto option = options.get(i);
            System.out.printf("    %d) %-6s value " + MONEY + "   shares bought: %d%n",
                    i + 1, option.name(), option.price(), option.sharesBought());
        }
        System.out.printf("  Event account balance: " + MONEY + "%n", state.accountBalance());
        System.out.printf("  Commission collected:  " + MONEY + "%n", state.commissionCollected());

        System.out.println();
        System.out.println("  Trade history (newest first):");
        List<TransactionDto> history = state.historyNewestFirst();
        if (history.isEmpty()) {
            System.out.println("    No trades yet.");
        } else {
            for (TransactionDto row : history) {
                System.out.printf("    #%d  %-6s x%d  paid " + MONEY + "%n",
                        row.serial(), row.optionName(), row.quantity(), row.shareCost() + row.commission());
            }
        }

        if (state.isClosed()) {
            System.out.println();
            System.out.println("  This event is closed. Winning option: " + state.winningOptionName());
            System.out.println("  Final share totals:");
            for (OptionStateDto option : options) {
                System.out.println("    " + option.name() + ": " + option.sharesBought());
            }
        }
    }

    // ---------- command 4 ----------

    private void participate() {
        EventDto chosen = chooseFrom(engine.listActiveEvents(), "Which event would you like to join");
        if (chosen == null) {
            return;
        }

        EventStateDto state = engine.eventState(chosen.id());
        System.out.println();
        System.out.println("Current standing:");
        List<OptionStateDto> options = state.options();
        for (int i = 0; i < options.size(); i++) {
            OptionStateDto option = options.get(i);
            System.out.printf("  %d) %-6s value " + MONEY + "   shares bought: %d%n",
                    i + 1, option.name(), option.price(), option.sharesBought());
        }
        System.out.println();

        int optionNumber = input.readIntInRange("Which option do you believe in (1-" + options.size() + ")? ", 1, options.size());
        long quantity = input.readPositiveLong("How many shares would you like to buy? ");

        PurchaseReceipt receipt = engine.buy(chosen.id(), optionNumber - 1, quantity);

        System.out.println();
        System.out.printf("Bought %d shares of %s.%n", receipt.quantity(), receipt.optionName());
        System.out.printf("  Shares:     " + MONEY + "%n", receipt.shareCost());
        System.out.printf("  Commission: " + MONEY + "%n", receipt.commission());
        System.out.printf("  Total paid: " + MONEY + "%n", receipt.total());
        System.out.println();
        printEventState(receipt.stateAfter());
    }

    // ---------- command 5 ----------

    private void closeEvent() {
        EventDto chosen = chooseFrom(engine.listActiveEvents(), "Which event would you like to close");
        if (chosen == null) {
            return;
        }

        EventStateDto state = engine.eventState(chosen.id());
        System.out.println();
        printEventState(state);
        System.out.println();

        List<OptionStateDto> options = state.options();
        int optionNumber = input.readIntInRange(
                "Which option did the event end with (1-" + options.size() + ")? ", 1, options.size());

        CloseReceipt receipt = engine.close(chosen.id(), optionNumber - 1);

        System.out.println();
        System.out.println("Event closed. Winning option: " + receipt.winningOptionName());
        System.out.printf("  Paid to winners:      " + MONEY + "%n", receipt.netPayout());
        System.out.printf("  Commission taken:     " + MONEY + "%n", receipt.commission());
        System.out.printf("  Returned to manager:  " + MONEY + "%n", receipt.returnedToManager());
        System.out.println();
        printEventState(receipt.stateAfter());
    }

    // ---------- command 6 ----------

    private void exit() {
        System.out.println("Goodbye.");
        running = false;
    }

    // ---------- shared ----------

    /**
     * Shows a numbered list and returns the event the user picked, or null if
     * the list was empty. This is the only place 1-based display numbers are
     * turned back into event ids.
     */
    private EventDto chooseFrom(List<EventDto> events, String prompt) {
        if (events.isEmpty()) {
            System.out.println("There are no events available for this command.");
            return null;
        }
        printEventList(events);
        int position = input.readIntInRange(prompt + " (1-" + events.size() + ")? ", 1, events.size());
        return events.get(position - 1);
    }
}
