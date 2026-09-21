package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.engine.api.BookDto;
import guessmarket.engine.api.CloseReceipt;
import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.EventDto;
import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.HoldingDto;
import guessmarket.engine.api.OpenReceipt;
import guessmarket.engine.api.OptionStateDto;
import guessmarket.engine.api.OrderDto;
import guessmarket.engine.api.OrderReceipt;
import guessmarket.engine.api.PayoutDto;
import guessmarket.engine.api.PurchaseReceipt;
import guessmarket.engine.api.TradeDto;
import guessmarket.engine.api.TransactionDto;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * The events screen.
 *
 * This is the exercise 2 screen with one thing changed: everything it asks for
 * comes from the server. Nothing below the table knows that, which is what the
 * interface was for.
 *
 * The rule that runs through the whole class: every engine call is an HTTP
 * request, so none of them happen on the JavaFX thread. Each one goes through
 * a Task, and only the handlers, which JavaFX runs back on its own thread,
 * touch a control. runAction below is that shape written once.
 */
public class EventsController {

    private static final String ALL = "All";
    private static final String ORDER_BOOK = "Order Book";
    private static final String NOT_STARTED = "Not started";
    private static final String ACTIVE = "Active";

    @FXML private ToggleGroup typeFilter;
    @FXML private ToggleGroup statusFilter;
    @FXML private ToggleGroup commissionFilter;

    @FXML private TableView<EventRow> eventsTable;
    @FXML private TableColumn<EventRow, String> nameColumn;
    @FXML private TableColumn<EventRow, String> statusColumn;
    @FXML private TableColumn<EventRow, String> typeColumn;
    @FXML private TableColumn<EventRow, String> commissionColumn;
    @FXML private TableColumn<EventRow, String> makerColumn;
    @FXML private TableColumn<EventRow, Number> balanceColumn;

    @FXML private Button refreshButton;
    @FXML private Label statusLabel;

    @FXML private Label detailTitle;
    @FXML private Label detailSubtitle;
    @FXML private HBox actionBar;
    @FXML private Button openButton;
    @FXML private Button buyButton;
    @FXML private Button orderButton;
    @FXML private Button closeButton;
    @FXML private Label actionHint;
    @FXML private VBox lmsrBox;
    @FXML private VBox orderBookBox;
    @FXML private HBox booksRow;
    @FXML private VBox tradesBox;
    @FXML private VBox participationBox;

    private HttpGuessMarketEngine engine;
    private String userName;
    private Runnable onMarketChanged = () -> { };

    /** Everything the server last sent, before the filter line is applied. */
    private final List<EventRow> allRows = new ArrayList<>();

    /** The event the details panel is currently showing. */
    private EventStateDto shownState;

    /**
     * The last answers this screen drew, kept by identity rather than by value.
     *
     * When nothing has changed the server replies 204 and the engine hands back
     * the very same list it gave last time. Comparing the reference is then
     * enough to know there is nothing to redraw, which is what keeps a poll
     * every second from rebuilding the table under the person's mouse.
     */
    private List<EventDto> lastEvents;
    private EventStateDto lastState;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        typeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        commissionColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCommission()));
        makerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMarketMaker()));
        balanceColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getAccountBalance()));
        balanceColumn.setCellFactory(column -> moneyCell());

        eventsTable.setPlaceholder(new Label("No events yet. Upload a file on the Account tab."));
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, was, now) -> loadDetails(now));

        keepOneSelected(typeFilter);
        keepOneSelected(statusFilter);
        keepOneSelected(commissionFilter);

        clearDetails();
    }

    private ConnectionWatch watch = ConnectionWatch.NONE;

    /** Who to tell when the server stops, or starts, answering. */
    void setConnectionWatch(ConnectionWatch watch) {
        this.watch = watch == null ? ConnectionWatch.NONE : watch;
    }

    public void setEngine(HttpGuessMarketEngine engine) {
        this.engine = engine;
    }

    /** Who this client is. There is no acting-as here: it is always you. */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /** Told to whoever else needs to know that balances may have moved. */
    public void setOnMarketChanged(Runnable listener) {
        this.onMarketChanged = listener == null ? () -> { } : listener;
    }

    // ---------- the table ----------

    @FXML
    private void onRefresh() {
        refresh();
    }

    public void refresh() {
        fetchEvents(true);
    }

    /**
     * One tick of the poll timer.
     *
     * Quiet on purpose: it does not disable the Refresh button, and it redraws
     * nothing when the answer is the one already on screen.
     */
    public void pollTick() {
        fetchEvents(false);
        pollDetails();
    }

    private void fetchEvents(boolean byHand) {
        if (engine == null) {
            return;
        }
        if (byHand) {
            refreshButton.setDisable(true);
        }

        Task<List<EventDto>> task = new Task<>() {
            @Override
            protected List<EventDto> call() {
                return engine.listEvents();
            }
        };

        task.setOnSucceeded(event -> {
            if (byHand) {
                refreshButton.setDisable(false);
            }
            watch.reached();
            List<EventDto> events = task.getValue();
            if (events == lastEvents && !byHand) {
                // The server said nothing changed. The label is still reset,
                // in case it was showing a failure from before the server came back.
                statusLabel.setText(describe(allRows.size()));
                return;
            }
            lastEvents = events;
            replaceRows(events == null ? List.of() : events);
            statusLabel.setText(describe(allRows.size()));
        });

        task.setOnFailed(event -> {
            if (byHand) {
                refreshButton.setDisable(false);
            }
            Throwable cause = task.getException();
            watch.failed(cause);
            statusLabel.setText(cause == null ? "The refresh failed." : cause.getMessage());
        });

        start(task, "events-refresh");
    }

    /** The open event, re-read on the timer, redrawn only if it moved. */
    private void pollDetails() {
        EventRow row = eventsTable.getSelectionModel().getSelectedItem();
        if (row == null || engine == null) {
            return;
        }
        int wantedId = row.getId();

        Task<EventStateDto> task = new Task<>() {
            @Override
            protected EventStateDto call() {
                return engine.eventState(wantedId);
            }
        };
        task.setOnSucceeded(event -> {
            EventStateDto state = task.getValue();
            if (state == lastState) {
                return;
            }
            EventRow current = eventsTable.getSelectionModel().getSelectedItem();
            if (current == null || current.getId() != wantedId) {
                return;
            }
            lastState = state;
            render(state);
        });
        start(task, "event-poll");
    }

    /** After an action of our own, the copies held here are stale. */
    private void forgetLastDrawn() {
        lastEvents = null;
        lastState = null;
    }

    private String describe(int count) {
        if (count == 0) {
            return "No events in the market yet.";
        }
        return count + (count == 1 ? " event" : " events") + " in the market.";
    }

    private void replaceRows(List<EventDto> events) {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        int keepId = selected == null ? -1 : selected.getId();

        allRows.clear();
        for (EventDto event : events) {
            allRows.add(new EventRow(
                    event.id(),
                    event.name(),
                    event.statusDisplay(),
                    event.typeDisplay(),
                    event.commissionPercent() + "% (" + event.commissionTypeDisplay() + ")",
                    event.marketMakerName() == null ? "" : event.marketMakerName(),
                    event.accountBalance()));
        }

        applyFilters();

        if (keepId >= 0) {
            for (EventRow row : eventsTable.getItems()) {
                if (row.getId() == keepId) {
                    eventsTable.getSelectionModel().select(row);
                    return;
                }
            }
            // The selected event is no longer in view, so the panel goes blank
            // rather than describing something that is not on the list.
            clearDetails();
        }
    }

    private void applyFilters() {
        String type = selectedFilter(typeFilter);
        String status = selectedFilter(statusFilter);
        String commission = selectedFilter(commissionFilter);

        List<EventRow> visible = new ArrayList<>();
        for (EventRow row : allRows) {
            if (matches(type, row.getType())
                    && matches(status, row.getStatus())
                    && containsIgnoringCase(commission, row.getCommission())) {
                visible.add(row);
            }
        }
        eventsTable.setItems(FXCollections.observableArrayList(visible));
    }

    private boolean matches(String wanted, String actual) {
        return ALL.equalsIgnoreCase(wanted) || wanted.equalsIgnoreCase(actual);
    }

    private boolean containsIgnoringCase(String wanted, String actual) {
        return ALL.equalsIgnoreCase(wanted)
                || actual.toLowerCase().contains(wanted.toLowerCase());
    }

    private String selectedFilter(ToggleGroup group) {
        if (group == null || group.getSelectedToggle() == null) {
            return ALL;
        }
        Object data = group.getSelectedToggle().getUserData();
        return data == null ? ALL : data.toString();
    }

    /** A toggle group with nothing selected would hide every row. */
    private void keepOneSelected(ToggleGroup group) {
        if (group == null) {
            return;
        }
        group.selectedToggleProperty().addListener((observable, was, now) -> {
            if (now == null && was != null) {
                group.selectToggle(was);
                return;
            }
            applyFilters();
        });
    }

    private TableCell<EventRow, Number> moneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format("%.2f", value.doubleValue()));
            }
        };
    }

    // ---------- the details panel ----------

    /**
     * Fetches one event in full and shows it.
     *
     * The answer is dropped if the person has selected something else while it
     * was on its way, so a slow reply cannot overwrite a newer selection.
     */
    private void loadDetails(EventRow row) {
        if (row == null || engine == null) {
            clearDetails();
            return;
        }
        int wantedId = row.getId();

        Task<EventStateDto> task = new Task<>() {
            @Override
            protected EventStateDto call() {
                return engine.eventState(wantedId);
            }
        };

        task.setOnSucceeded(event -> {
            EventRow current = eventsTable.getSelectionModel().getSelectedItem();
            if (current == null || current.getId() != wantedId) {
                return;
            }
            lastState = task.getValue();
            render(task.getValue());
        });

        task.setOnFailed(event -> {
            Throwable cause = task.getException();
            clearDetails();
            detailTitle.setText(row.getName());
            detailSubtitle.setText(cause == null
                    ? "The event could not be loaded." : cause.getMessage());
        });

        start(task, "event-details");
    }

    /** Re-reads the event the panel is showing, after something changed it. */
    private void reloadDetails() {
        loadDetails(eventsTable.getSelectionModel().getSelectedItem());
    }

    private void clearDetails() {
        shownState = null;
        detailTitle.setText("Select an event to see its details");
        detailSubtitle.setText("");
        actionHint.setText("");
        lmsrBox.getChildren().clear();
        booksRow.getChildren().clear();
        tradesBox.getChildren().clear();
        participationBox.getChildren().clear();
        show(lmsrBox, false);
        show(orderBookBox, false);
        show(tradesBox, false);
        show(participationBox, false);
        actionBar.setVisible(false);
        actionBar.setManaged(false);
    }

    private void render(EventStateDto state) {
        clearDetails();
        if (state == null) {
            return;
        }
        shownState = state;

        detailTitle.setText(state.name());
        detailSubtitle.setText(state.description());

        actionBar.setVisible(true);
        actionBar.setManaged(true);
        updateActions(state);

        if (ORDER_BOOK.equals(state.typeDisplay())) {
            showOrderBook(state);
        } else {
            showLmsr(state);
        }
        showParticipations(state);
    }

    /**
     * Only the market maker may open or close, and only an active LMSR event
     * can be bought into. The hint says why a button is disabled rather than
     * leaving the person guessing.
     */
    private void updateActions(EventStateDto state) {
        boolean isMaker = userName != null && userName.equalsIgnoreCase(state.marketMakerName());
        boolean notStarted = NOT_STARTED.equals(state.statusDisplay());
        boolean active = ACTIVE.equals(state.statusDisplay());
        boolean lmsr = !ORDER_BOOK.equals(state.typeDisplay());

        openButton.setDisable(!(isMaker && notStarted));
        closeButton.setDisable(!(isMaker && active));
        buyButton.setDisable(!(active && lmsr));
        orderButton.setDisable(!(active && !lmsr));
        buyButton.setVisible(lmsr);
        buyButton.setManaged(lmsr);
        orderButton.setVisible(!lmsr);
        orderButton.setManaged(!lmsr);

        if (!isMaker && notStarted) {
            actionHint.setText("Only " + state.marketMakerName() + " can open this event.");
        } else if (state.isClosed()) {
            actionHint.setText("This event is closed and can no longer be traded.");
        } else {
            actionHint.setText("");
        }
    }

    private void showLmsr(EventStateDto state) {
        show(lmsrBox, true);

        lmsrBox.getChildren().add(sectionTitle("Current standing"));
        for (OptionStateDto option : state.options()) {
            lmsrBox.getChildren().add(new Label(String.format(
                    "%s     value %.2f     shares bought: %d",
                    option.name(), option.price(), option.sharesBought())));
        }

        lmsrBox.getChildren().add(sectionTitle("Account"));
        lmsrBox.getChildren().add(new Label(String.format("Balance: %.2f", state.accountBalance())));
        lmsrBox.getChildren().add(new Label(
                String.format("Commission collected: %.2f", state.commissionCollected())));

        lmsrBox.getChildren().add(sectionTitle("Trade history"));
        if (state.historyNewestFirst().isEmpty()) {
            lmsrBox.getChildren().add(new Label("No trades yet."));
        } else {
            for (TransactionDto t : state.historyNewestFirst()) {
                lmsrBox.getChildren().add(new Label(String.format(
                        "#%d   %s   %s   x%d   paid %.2f   commission %.2f",
                        t.serial(), t.userName(), t.optionName(), t.quantity(),
                        t.shareCost(), t.commission())));
            }
        }

        if (state.isClosed()) {
            lmsrBox.getChildren().add(sectionTitle("Result"));
            lmsrBox.getChildren().add(new Label("Winning option: " + state.winningOptionName()));
        }
    }

    /**
     * One order book pane per option, side by side, as the sketch draws it.
     *
     * Exercise 2 had two panes written into the layout. The panes are built
     * from the event now, so a three option event gets three, and the row
     * scrolls sideways rather than squeezing them.
     */
    private void showOrderBook(EventStateDto state) {
        show(orderBookBox, true);

        for (BookDto book : state.books()) {
            VBox pane = new VBox(3);
            pane.getStyleClass().add("book-pane");
            pane.setMinWidth(210);
            HBox.setHgrow(pane, Priority.ALWAYS);

            pane.getChildren().add(sectionTitle(book.optionName()));
            pane.getChildren().add(new Label("LAST    " + money(book.last())));
            pane.getChildren().add(new Label("BID     " + money(book.bestBid())));
            pane.getChildren().add(new Label("ASK     " + money(book.bestAsk())));
            pane.getChildren().add(new Label("MID     " + money(book.mid())));
            pane.getChildren().add(new Label("SPREAD  " + money(book.spread())));
            pane.getChildren().add(new Label("Shares in issue: " + book.sharesInIssue()));

            pane.getChildren().add(sectionTitle("Buy orders"));
            addOrders(pane, book.bids());
            pane.getChildren().add(sectionTitle("Sell orders"));
            addOrders(pane, book.asks());

            booksRow.getChildren().add(pane);
        }

        if (!state.tradesNewestFirst().isEmpty()) {
            show(tradesBox, true);
            tradesBox.getChildren().add(sectionTitle("Trades, newest first"));
            for (TradeDto trade : state.tradesNewestFirst()) {
                String who = trade.sellerName() == null
                        ? trade.buyerName() + " (mint)"
                        : trade.buyerName() + " from " + trade.sellerName();
                tradesBox.getChildren().add(new Label(String.format(
                        "#%d  %s  %d %s at %.2f",
                        trade.serial(), who, trade.quantity(), trade.optionName(), trade.price())));
            }
        }
    }

    private void addOrders(VBox box, List<OrderDto> orders) {
        if (orders.isEmpty()) {
            box.getChildren().add(new Label("   none"));
            return;
        }
        for (OrderDto order : orders) {
            box.getChildren().add(new Label(String.format(
                    "   %s  %d at %.2f", order.userName(), order.remaining(), order.price())));
        }
    }

    private void showParticipations(EventStateDto state) {
        show(participationBox, true);
        participationBox.getChildren().add(sectionTitle("Participations"));

        if (state.participations().isEmpty()) {
            participationBox.getChildren().add(new Label("Nobody holds shares in this event yet."));
            return;
        }
        for (HoldingDto holding : state.participations()) {
            participationBox.getChildren().add(new Label(String.format(
                    "%s holds %d of %s", holding.userName(), holding.shares(), holding.optionName())));
        }
    }

    /** The statistics are missing rather than zero when nothing supports them. */
    private String money(Double value) {
        return value == null ? "-" : String.format("%.2f", value);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private void show(Region box, boolean visible) {
        box.setVisible(visible);
        box.setManaged(visible);
    }

    // ---------- the four actions ----------

    @FXML
    private void onOpenEvent() {
        EventStateDto state = shownState;
        if (state == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Open event");
        confirm.setHeaderText("Open \"" + state.name() + "\"?");
        confirm.setContentText(String.format(
                "You will pay %.2f from your own account to open it.", state.openingCost()));
        if (confirm.showAndWait().orElse(null) != ButtonType.OK) {
            return;
        }

        runAction("The event was not opened",
                () -> engine.openEvent(state.id(), userName),
                (OpenReceipt receipt) -> info("Event opened", String.format(
                        "You paid %.2f. Your balance is now %.2f.",
                        receipt.amountPaid(), receipt.marketMakerBalanceAfter())));
    }

    @FXML
    private void onBuyShares() {
        EventStateDto state = shownState;
        if (state == null) {
            return;
        }

        Optional<BuyDialog.Choice> choice =
                new BuyDialog(engine, state, userName).showAndWait();
        if (choice.isEmpty()) {
            return;
        }
        BuyDialog.Choice c = choice.get();

        runAction("Nothing was bought",
                () -> engine.buy(state.id(), userName, c.optionIndex(), c.quantity()),
                (PurchaseReceipt receipt) -> info("Shares bought", String.format(
                        "You bought %d shares of %s.%n%nShares: %.2f%nCommission: %.2f%n"
                                + "Total paid: %.2f%n%nYour balance is now %.2f.",
                        receipt.quantity(), receipt.optionName(), receipt.shareCost(),
                        receipt.commission(), receipt.total(), receipt.buyerBalanceAfter())));
    }

    @FXML
    private void onPlaceOrder() {
        EventStateDto state = shownState;
        if (state == null) {
            return;
        }

        Optional<OrderDialog.Choice> choice =
                new OrderDialog(state, userName, 0).showAndWait();
        if (choice.isEmpty()) {
            return;
        }
        OrderDialog.Choice c = choice.get();

        runAction("The order was not accepted",
                () -> engine.placeOrder(state.id(), userName, c.optionIndex(),
                        c.side(), c.quantity(), c.price()),
                (OrderReceipt receipt) -> {
                    StringBuilder text = new StringBuilder();
                    text.append(String.format("%s order for %d shares of %s at %.2f.%n",
                            receipt.sideDisplay(), receipt.quantitySubmitted(),
                            receipt.optionName(), c.price()));
                    text.append(String.format("Filled straight away: %d%n", receipt.quantityFilled()));
                    text.append(String.format("Left resting in the book: %d%n", receipt.quantityResting()));

                    if (!receipt.trades().isEmpty()) {
                        text.append(System.lineSeparator()).append("Trades:")
                            .append(System.lineSeparator());
                        for (TradeDto trade : receipt.trades()) {
                            if (trade.sellerName() == null) {
                                text.append(String.format("  mint: %s receives %d %s at %.2f%n",
                                        trade.buyerName(), trade.quantity(),
                                        trade.optionName(), trade.price()));
                            } else {
                                text.append(String.format("  %s bought %d %s from %s at %.2f%n",
                                        trade.buyerName(), trade.quantity(), trade.optionName(),
                                        trade.sellerName(), trade.price()));
                            }
                        }
                    }
                    text.append(System.lineSeparator())
                        .append(String.format("Your balance is now %.2f", receipt.balanceAfter()));
                    info("Order submitted", text.toString());
                });
    }

    @FXML
    private void onCloseEvent() {
        EventStateDto state = shownState;
        if (state == null) {
            return;
        }

        List<String> names = new ArrayList<>();
        for (OptionStateDto option : state.options()) {
            names.add(option.name());
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Close event");
        dialog.setHeaderText("Close \"" + state.name() + "\"");
        dialog.setContentText("Which option won?");

        Optional<String> winner = dialog.showAndWait();
        if (winner.isEmpty()) {
            return;
        }
        int winningIndex = names.indexOf(winner.get());

        runAction("The event was not closed",
                () -> engine.closeEvent(state.id(), userName, winningIndex),
                (CloseReceipt receipt) -> {
                    StringBuilder text = new StringBuilder();
                    text.append(String.format("Winning option: %s%n", receipt.winningOptionName()));
                    text.append(String.format("Winning shares: %d%n", receipt.winningShares()));
                    text.append(String.format("Paid to winners: %.2f%n", receipt.netPaidToWinners()));
                    text.append(String.format("Commission: %.2f%n", receipt.commission()));
                    text.append(String.format("To the market maker: %.2f%n",
                            receipt.returnedToMarketMaker()));
                    if (!receipt.payouts().isEmpty()) {
                        text.append(System.lineSeparator()).append("Payouts:")
                            .append(System.lineSeparator());
                        for (PayoutDto payout : receipt.payouts()) {
                            text.append(String.format("  %s: %.2f%n",
                                    payout.userName(), payout.amount()));
                        }
                    }
                    info("Event closed", text.toString());
                });
    }

    /**
     * Every action is the same shape: the buttons go dead, the request goes out
     * on a background thread, and when it comes back the screen is reloaded and
     * the person is told what happened. A refusal from the engine arrives as an
     * EngineException carrying a sentence written to be read.
     */
    private <T> void runAction(String failHeader, Callable<T> work, Consumer<T> onSuccess) {
        setActionsDisabled(true);

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(event -> {
            setActionsDisabled(false);
            forgetLastDrawn();
            refresh();
            reloadDetails();
            onMarketChanged.run();
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            setActionsDisabled(false);
            Throwable cause = task.getException();
            String message = cause instanceof EngineException && cause.getMessage() != null
                    ? cause.getMessage()
                    : String.valueOf(cause);
            problem(failHeader, message);
        });

        start(task, "market-action");
    }

    private void setActionsDisabled(boolean disabled) {
        openButton.setDisable(disabled);
        buyButton.setDisable(disabled);
        orderButton.setDisable(disabled);
        closeButton.setDisable(disabled);
    }

    private void start(Task<?> task, String name) {
        Thread worker = new Thread(task, name);
        worker.setDaemon(true);
        worker.start();
    }

    private void info(String header, String body) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(header);
        alert.setHeaderText(header);
        alert.setContentText(body);
        alert.showAndWait();
    }

    private void problem(String header, String body) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(header);
        alert.setHeaderText(header);
        alert.setContentText(body);
        alert.showAndWait();
    }
}
