package guessmarket.ui.fx;

import guessmarket.engine.api.EventDto;
import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.OptionStateDto;
import guessmarket.engine.api.TransactionDto;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The events screen. The filter line and the table are on the left, the details
 * of the selected event on the right, following the supplied sketch.
 */
public class EventsController {

    private static final String ALL = "All";
    private static final String ORDER_BOOK = "Order Book";

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
    @FXML private VBox lmsrBox;
    @FXML private VBox orderBookBox;
    @FXML private VBox bookOneBox;
    @FXML private VBox bookTwoBox;
    @FXML private VBox participationBox;

    private final ObservableList<EventRow> allRows = FXCollections.observableArrayList();
    private FilteredList<EventRow> visibleRows;

    private GuessMarketEngine engine;

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

    /** Reloads the table from the engine. Called after a file loads. */
    public void refresh() {
        allRows.clear();
        clearDetails();

        if (engine == null || !engine.isLoaded()) {
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

    private void clearDetails() {
        detailTitle.setText("Select an event to see its details");
        detailSubtitle.setText("");
        lmsrBox.getChildren().clear();
        bookOneBox.getChildren().clear();
        bookTwoBox.getChildren().clear();
        participationBox.getChildren().clear();
        show(lmsrBox, false);
        show(orderBookBox, false);
        show(participationBox, false);
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
        detailSubtitle.setText(row.getType() + "  ·  " + row.getStatus()
                + "  ·  market maker: " + (row.getMarketMaker().isEmpty() ? "none" : row.getMarketMaker()));

        if (ORDER_BOOK.equals(row.getType())) {
            showOrderBook(state);
        } else {
            showLmsr(state);
        }

        show(participationBox, true);
        participationBox.getChildren().add(sectionTitle("Participations information"));
        participationBox.getChildren().add(new Label(
                "Shown once users can trade, in the next stage of the exercise."));
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
                String.format("Comision collected: %.2f", state.commissionCollected())));

        lmsrBox.getChildren().add(sectionTitle("Trade history"));
        if (state.historyNewestFirst().isEmpty()) {
            lmsrBox.getChildren().add(new Label("No trades yet."));
        } else {
            for (TransactionDto t : state.historyNewestFirst()) {
                lmsrBox.getChildren().add(new Label(String.format(
                        "#%d   %s   x%d   paid %.2f   comision %.2f",
                        t.serial(), t.optionName(), t.quantity(), t.shareCost(), t.commission())));
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
            boxes[i].getChildren().add(new Label("Order book"));
            boxes[i].getChildren().add(new Label("LAST   -"));
            boxes[i].getChildren().add(new Label("BID    -"));
            boxes[i].getChildren().add(new Label("ASK    -"));
            boxes[i].getChildren().add(new Label("MID    -"));
            boxes[i].getChildren().add(new Label("SPREAD -"));
            if (i < options.size()) {
                boxes[i].getChildren().add(new Label(
                        "Shares held: " + options.get(i).sharesBought()));
            }
        }
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }
}
