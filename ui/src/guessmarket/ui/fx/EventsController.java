package guessmarket.ui.fx;

import guessmarket.engine.api.CloseReceipt;
import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.EventDto;
import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.HoldingDto;
import guessmarket.engine.api.OpenReceipt;
import guessmarket.engine.api.OptionStateDto;
import guessmarket.engine.api.PayoutDto;
import guessmarket.engine.api.PurchaseReceipt;
import guessmarket.engine.api.TransactionDto;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The events screen. The filter line and the table are on the left, the details
 * and the actions on the right, following the supplied sketch.
 *
 * Every action is performed as the user chosen in the top bar, so the buttons
 * enable and disable according to who that is.
 */
public class EventsController {

    private static final String ALL = "All";
    private static final String ORDER_BOOK = "Order Book";
    private static final String NOT_STARTED = "Not started";
    private static final String ACTIVE = "Active";

    @FXML private TableView<EventRow> eventsTable;
    @FXML private TableColumn<EventRow, String> nameColumn;
    @FXML private TableColumn<EventRow, String> statusColumn;
    @FXML private TableColumn<EventRow, String> typeColumn;
    @FXML private TableColumn<EventRow, String> commissionColumn;
    @FXML private TableColumn<EventRow, String> makerColumn;
    @FXML private TableColumn<EventRow, Number> balanceColumn;

    @FXML private ToggleGroup typeFilter;
    @FXML private ToggleGroup statusFilter;
    @FXML private ToggleGroup commissionFilter;

    @FXML private Label detailTitle;
    @FXML private Label detailSubtitle;
    @FXML private HBox actionBar;
    @FXML private Button openButton;
    @FXML private Button buyButton;
    @FXML private Button closeButton;
    @FXML private Label actionHint;
    @FXML private VBox lmsrBox;
    @FXML private VBox orderBookBox;
    @FXML private VBox bookOneBox;
    @FXML private VBox bookTwoBox;
    @FXML private VBox participationBox;

    private final ObservableList<EventRow> allRows = FXCollections.observableArrayList();
    private FilteredList<EventRow> visibleRows;

    private GuessMarketEngine engine;
    private String actingUser;

    /** Told to the main controller so the users screen can refresh too. */
    private Runnable onSystemChanged = () -> { };

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        typeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        commissionColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCommission()));
        makerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMarketMaker()));
        balanceColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getAccountBalance()));
        balanceColumn.setCellFactory(column -> moneyCell());

        visibleRows = new FilteredList<>(allRows, row -> true);
        eventsTable.setItems(visibleRows);
        eventsTable.setPlaceholder(new Label("No events to show."));
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, was, now) -> showDetails(now));

        keepOneSelected(typeFilter);
        keepOneSelected(statusFilter);
        keepOneSelected(commissionFilter);

        clearDetails();
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    public void setOnSystemChanged(Runnable listener) {
        this.onSystemChanged = listener == null ? () -> { } : listener;
    }

    /** Called when the person picks a different user in the top bar. */
    public void setActingUser(String userName) {
        this.actingUser = userName;
        updateActions(selectedState());
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

    /** A toggle group with nothing selected would hide every row. */
    private void keepOneSelected(ToggleGroup group) {
        if (group == null) {
            return;
        }
        group.selectedToggleProperty().addListener((obs, was, now) -> {
            if (now == null && was != null) {
                group.selectToggle(was);
                return;
            }
            applyFilters();
        });
    }

    /** Reloads the table from the engine, keeping the selected row if it is still there. */
    public void refresh() {
        int keepId = -1;
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            keepId = selected.getId();
        }

        allRows.clear();
        if (engine == null || !engine.isLoaded()) {
            clearDetails();
            return;
        }

        List<EventRow> rows = new ArrayList<>();
        for (EventDto event : engine.listEvents()) {
            rows.add(new EventRow(
                    event.id(),
                    event.name(),
                    event.statusDisplay(),
                    event.typeDisplay(),
                    event.commissionPercent() + "% (" + event.commissionTypeDisplay() + ")",
                    event.marketMakerName() == null ? "" : event.marketMakerName(),
                    event.accountBalance()));
        }
        allRows.setAll(rows);
        applyFilters();

        if (keepId >= 0) {
            for (EventRow row : visibleRows) {
                if (row.getId() == keepId) {
                    eventsTable.getSelectionModel().select(row);
                    return;
                }
            }
        }
        clearDetails();
    }

    private void applyFilters() {
        String type = selectedFilter(typeFilter);
        String status = selectedFilter(statusFilter);
        String commission = selectedFilter(commissionFilter);

        visibleRows.setPredicate(row ->
                matches(type, row.getType())
                        && matches(status, row.getStatus())
                        && containsIgnoringCase(commission, row.getCommission()));
    }

    private boolean matches(String wanted, String actual) {
        return ALL.equalsIgnoreCase(wanted) || wanted.equalsIgnoreCase(actual);
    }

    private boolean containsIgnoringCase(String wanted, String actual) {
        return ALL.equalsIgnoreCase(wanted)
                || actual.toLowerCase().contains(wanted.toLowerCase());
    }

    /** Reads the userData of the selected toggle, which the fxml sets. */
    private String selectedFilter(ToggleGroup group) {
        if (group == null || group.getSelectedToggle() == null) {
            return ALL;
        }
        Object data = group.getSelectedToggle().getUserData();
        return data == null ? ALL : data.toString();
    }

    private EventRow selectedRow() {
        return eventsTable.getSelectionModel().getSelectedItem();
    }

    private EventStateDto selectedState() {
        EventRow row = selectedRow();
        if (row == null || engine == null || !engine.isLoaded()) {
            return null;
        }
        return engine.eventState(row.getId());
    }

    // ---------- actions ----------

    @FXML
    private void onOpenEvent() {
        EventStateDto state = selectedState();
        if (state == null || actingUser == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Open event");
        confirm.setHeaderText("Open \"" + state.name() + "\"?");
        confirm.setContentText(String.format(
                "%s will pay %.2f from their own account to open it.",
                actingUser, state.openingCost()));
        if (confirm.showAndWait().orElse(null) != javafx.scene.control.ButtonType.OK) {
            return;
        }

        try {
            OpenReceipt receipt = engine.openEvent(state.id(), actingUser);
            refreshAll();
            info("Event opened", String.format(
                    "%s paid %.2f. Their balance is now %.2f.",
                    receipt.marketMakerName(), receipt.amountPaid(),
                    receipt.marketMakerBalanceAfter()));
        } catch (EngineException e) {
            problem("The event was not opened", e.getMessage());
        }
    }

    @FXML
    private void onBuyShares() {
        EventStateDto state = selectedState();
        if (state == null || actingUser == null) {
            return;
        }

        Optional<BuyDialog.Choice> choice = new BuyDialog(engine, state, actingUser).showAndWait();
        if (choice.isEmpty()) {
            return;
        }

        try {
            PurchaseReceipt receipt = engine.buy(
                    state.id(), actingUser, choice.get().optionIndex(), choice.get().quantity());
            refreshAll();
            info("Shares bought", String.format(
                    "%s bought %d shares of %s.%n%nShares: %.2f%nCommission: %.2f%nTotal paid: %.2f"
                            + "%n%nTheir balance is now %.2f.",
                    receipt.userName(), receipt.quantity(), receipt.optionName(),
                    receipt.shareCost(), receipt.commission(), receipt.total(),
                    receipt.buyerBalanceAfter()));
        } catch (EngineException e) {
            problem("Nothing was bought", e.getMessage());
        }
    }

    @FXML
    private void onCloseEvent() {
        EventStateDto state = selectedState();
        if (state == null || actingUser == null) {
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

        try {
            CloseReceipt receipt = engine.closeEvent(state.id(), actingUser, winningIndex);
            refreshAll();

            StringBuilder text = new StringBuilder();
            text.append(String.format("Winning option: %s%n", receipt.winningOptionName()));
            text.append(String.format("Winning shares: %d%n", receipt.winningShares()));
            text.append(String.format("Paid to winners: %.2f%n", receipt.netPaidToWinners()));
            text.append(String.format("Commission: %.2f%n", receipt.commission()));
            text.append(String.format("To the market maker: %.2f%n", receipt.returnedToMarketMaker()));
            if (!receipt.payouts().isEmpty()) {
                text.append(System.lineSeparator()).append("Payouts:").append(System.lineSeparator());
                for (PayoutDto payout : receipt.payouts()) {
                    text.append(String.format("  %s: %.2f%n", payout.userName(), payout.amount()));
                }
            }
            info("Event closed", text.toString());
        } catch (EngineException e) {
            problem("The event was not closed", e.getMessage());
        }
    }

    private void refreshAll() {
        refresh();
        onSystemChanged.run();
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

    // ---------- details ----------

    private void clearDetails() {
        detailTitle.setText("Select an event to see its details");
        detailSubtitle.setText("");
        actionHint.setText("");
        lmsrBox.getChildren().clear();
        bookOneBox.getChildren().clear();
        bookTwoBox.getChildren().clear();
        participationBox.getChildren().clear();
        show(lmsrBox, false);
        show(orderBookBox, false);
        show(participationBox, false);
        actionBar.setVisible(false);
        actionBar.setManaged(false);
    }

    private void show(VBox box, boolean visible) {
        box.setVisible(visible);
        box.setManaged(visible);
    }

    private void showDetails(EventRow row) {
        clearDetails();
        if (row == null || engine == null || !engine.isLoaded()) {
            return;
        }

        EventStateDto state = engine.eventState(row.getId());
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
     * Only the market maker may open or close, and only an active LMSR event can
     * be bought into. The hint says why a button is disabled, rather than
     * leaving the person guessing.
     */
    private void updateActions(EventStateDto state) {
        if (state == null) {
            return;
        }
        boolean isMaker = actingUser != null && actingUser.equals(state.marketMakerName());
        boolean notStarted = NOT_STARTED.equals(state.statusDisplay());
        boolean active = ACTIVE.equals(state.statusDisplay());
        boolean lmsr = !ORDER_BOOK.equals(state.typeDisplay());

        openButton.setDisable(!(isMaker && notStarted));
        closeButton.setDisable(!(isMaker && active));
        buyButton.setDisable(!(active && lmsr));

        if (actingUser == null) {
            actionHint.setText("Choose who you are acting as, at the top of the window.");
        } else if (!isMaker && notStarted) {
            actionHint.setText("Only " + state.marketMakerName() + " can open this event.");
        } else if (ORDER_BOOK.equals(state.typeDisplay()) && active) {
            actionHint.setText("Order book trading is not implemented yet.");
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
     * The sketch puts one order book per option, side by side. The books
     * themselves are filled in once order book trading is implemented.
     */
    private void showOrderBook(EventStateDto state) {
        show(orderBookBox, true);

        List<OptionStateDto> options = state.options();
        VBox[] boxes = {bookOneBox, bookTwoBox};

        for (int i = 0; i < boxes.length; i++) {
            String name = i < options.size() ? options.get(i).name() : "Option " + (i + 1);
            boxes[i].getChildren().add(sectionTitle(name));
            boxes[i].getChildren().add(new Label("LAST   -"));
            boxes[i].getChildren().add(new Label("BID    -"));
            boxes[i].getChildren().add(new Label("ASK    -"));
            boxes[i].getChildren().add(new Label("MID    -"));
            boxes[i].getChildren().add(new Label("SPREAD -"));
            if (i < options.size()) {
                boxes[i].getChildren().add(new Label(
                        "Shares in issue: " + options.get(i).sharesBought()));
            }
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

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }
}
