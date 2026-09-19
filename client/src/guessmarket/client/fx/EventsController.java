package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.engine.api.EventDto;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * The events screen.
 *
 * This is the exercise 2 screen with one thing changed: the rows come from the
 * server instead of from an engine in this process. Nothing below the table
 * knows that, which is what the interface was for.
 *
 * The rule that matters here: engine.listEvents() is an HTTP request, so it
 * never runs on the JavaFX thread. It runs inside a Task, and only the two
 * handlers, which JavaFX runs back on its own thread, touch any control. The
 * same shape will carry the poll timer.
 */
public class EventsController {

    private static final String ALL = "All";

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

    private HttpGuessMarketEngine engine;

    /** Everything the server last sent, before the filter line is applied. */
    private final List<EventRow> allRows = new ArrayList<>();

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
                .addListener((observable, was, now) -> showDetails(now));

        keepOneSelected(typeFilter);
        keepOneSelected(statusFilter);
        keepOneSelected(commissionFilter);

        showDetails(null);
    }

    public void setEngine(HttpGuessMarketEngine engine) {
        this.engine = engine;
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    /**
     * Asks the server for the events and puts them in the table.
     *
     * The request is made on a background thread. When it comes back, the
     * selected event is kept if it is still there, so a refresh does not throw
     * away what the person was looking at — which matters even more once this
     * is running on a timer.
     */
    public void refresh() {
        if (engine == null) {
            return;
        }
        refreshButton.setDisable(true);

        Task<List<EventDto>> task = new Task<>() {
            @Override
            protected List<EventDto> call() {
                return engine.listEvents();
            }
        };

        task.setOnSucceeded(event -> {
            refreshButton.setDisable(false);
            List<EventDto> events = task.getValue();
            replaceRows(events == null ? List.of() : events);
            statusLabel.setText(describe(allRows.size()));
        });

        task.setOnFailed(event -> {
            refreshButton.setDisable(false);
            Throwable cause = task.getException();
            statusLabel.setText(cause == null ? "The refresh failed." : cause.getMessage());
        });

        Thread worker = new Thread(task, "events-refresh");
        worker.setDaemon(true);
        worker.start();
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
        }
    }

    /**
     * The filter line, unchanged from exercise 2 in behaviour.
     *
     * The rows are filtered into a fresh list rather than through a
     * FilteredList, because the whole table is rebuilt from the server on every
     * refresh anyway and a plain list is one less thing to keep in step.
     */
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

    /** Reads the userData of the selected toggle, which the fxml sets. */
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

    /** The right half is still a placeholder; this is as far as it goes for now. */
    private void showDetails(EventRow row) {
        if (row == null) {
            detailTitle.setText("Select an event to see its details");
            detailSubtitle.setText("");
            return;
        }
        detailTitle.setText(row.getName());
        detailSubtitle.setText(row.getType() + " - " + row.getStatus()
                + " - market maker " + row.getMarketMaker());
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
}
