package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.engine.api.EngineException;
import guessmarket.engine.api.EventRoleDto;
import guessmarket.engine.api.HoldingDto;
import guessmarket.engine.api.LoadException;
import guessmarket.engine.api.MovementDto;
import guessmarket.engine.api.UserDto;
import guessmarket.engine.api.UserStateDto;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The account screen: the file loader, everyone in the market, your own
 * balance, the events you are in, and every line your balance has moved
 * through.
 *
 * Two different kinds of question are answered here, and they come from two
 * different endpoints on purpose. What everyone is allowed to know about
 * everyone else — name, balance, whether they are a market maker — is /users.
 * Everything about you, including your own movement lines, is /user-state,
 * which can only ever describe the session asking.
 */
public class AccountPane extends VBox {

    private final HttpGuessMarketEngine engine;
    private final String userName;
    private final Runnable onMarketChanged;

    private final Button loadButton = new Button("Load file");
    private final Label pathLabel = new Label("No file chosen");
    private final Label messageLabel = new Label();

    private final Label balanceLabel = new Label("Balance: -");
    private final Label roleLabel = new Label();
    private final TextField amountField = new TextField();
    private final Button fundsButton = new Button("Load funds");
    private final Label fundsMessage = new Label();

    private final Button refreshButton = new Button("Refresh");
    private final TableView<UserRow> usersTable = new TableView<>();
    private final VBox eventsBox = new VBox(3);
    private final TableView<MovementDto> movementsTable = new TableView<>();

    /**
     * The last answers this screen drew, by identity. The engine hands back the
     * same object when the server said nothing changed, so a poll that finds
     * nothing new redraws nothing.
     */
    private List<UserDto> lastUsers;
    private UserStateDto lastMine;

    public AccountPane(HttpGuessMarketEngine engine, String userName, Runnable onMarketChanged) {
        this.engine = engine;
        this.userName = userName;
        this.onMarketChanged = onMarketChanged == null ? () -> { } : onMarketChanged;

        getStyleClass().add("account");
        setSpacing(10);
        setPadding(new Insets(12));

        SplitPane halves = new SplitPane();
        halves.getItems().addAll(buildLeft(), buildRight());
        halves.setDividerPositions(0.42);
        VBox.setVgrow(halves, Priority.ALWAYS);

        getChildren().addAll(buildLoadBar(), messageLabel, halves);
        refresh();
    }

    // ---------- the file loader, at the top as the sketch draws it ----------

    private HBox buildLoadBar() {
        loadButton.getStyleClass().add("load-button");
        loadButton.setOnAction(event -> chooseAndUpload());

        pathLabel.getStyleClass().add("path-label");
        pathLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pathLabel, Priority.ALWAYS);

        messageLabel.setWrapText(true);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.getStyleClass().add("load-message");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        refreshButton.setOnAction(event -> refresh());

        HBox bar = new HBox(10, loadButton, pathLabel, refreshButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("load-bar");
        return bar;
    }

    // ---------- left: everyone else, then you ----------

    private VBox buildLeft() {
        TableColumn<UserRow, String> nameColumn = new TableColumn<>("User");
        nameColumn.setPrefWidth(130);
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<UserRow, Number> balanceColumn = new TableColumn<>("Balance");
        balanceColumn.setPrefWidth(90);
        balanceColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getBalance()));
        balanceColumn.setCellFactory(column -> moneyCell());

        TableColumn<UserRow, String> roleColumn = new TableColumn<>("Role");
        roleColumn.setPrefWidth(100);
        roleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));

        usersTable.getColumns().addAll(nameColumn, balanceColumn, roleColumn);
        usersTable.setPlaceholder(new Label("Nobody is logged in yet."));
        VBox.setVgrow(usersTable, Priority.ALWAYS);

        balanceLabel.getStyleClass().add("balance-label");
        roleLabel.getStyleClass().add("detail-subtitle");

        amountField.setPromptText("amount");
        amountField.setPrefWidth(110);
        amountField.setOnAction(event -> addFunds());

        fundsButton.getStyleClass().add("load-button");
        fundsButton.setOnAction(event -> addFunds());

        fundsMessage.setWrapText(true);
        fundsMessage.setMinHeight(Region.USE_PREF_SIZE);
        fundsMessage.getStyleClass().add("load-message");
        fundsMessage.setVisible(false);
        fundsMessage.setManaged(false);

        HBox fundsBar = new HBox(8, amountField, fundsButton);
        fundsBar.setAlignment(Pos.CENTER_LEFT);

        VBox mine = new VBox(6, sectionTitle("Your account"), balanceLabel, roleLabel,
                fundsBar, fundsMessage);
        mine.getStyleClass().add("account-card");

        VBox left = new VBox(8, sectionTitle("Everyone in the market"), usersTable, mine);
        left.setPadding(new Insets(0, 6, 0, 0));
        return left;
    }

    // ---------- right: your events, then your lines ----------

    private VBox buildRight() {
        eventsBox.getStyleClass().add("participation-pane");

        TableColumn<MovementDto, Number> serialColumn = new TableColumn<>("#");
        serialColumn.setPrefWidth(40);
        serialColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().serial()));
        serialColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.valueOf(value.intValue()));
            }
        });

        TableColumn<MovementDto, String> reasonColumn = new TableColumn<>("What happened");
        reasonColumn.setPrefWidth(300);
        reasonColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason()));

        TableColumn<MovementDto, Number> changeColumn = new TableColumn<>("Change");
        changeColumn.setPrefWidth(90);
        changeColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().change()));
        changeColumn.setCellFactory(column -> signedMoneyCell());

        TableColumn<MovementDto, Number> afterColumn = new TableColumn<>("Balance");
        afterColumn.setPrefWidth(90);
        afterColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().balanceAfter()));
        afterColumn.setCellFactory(column -> moneyCell());

        movementsTable.getColumns().addAll(serialColumn, reasonColumn, changeColumn, afterColumn);
        movementsTable.setPlaceholder(new Label("Nothing has moved in your account yet."));
        VBox.setVgrow(movementsTable, Priority.ALWAYS);

        VBox right = new VBox(8,
                sectionTitle("Events you are in, or run"), eventsBox,
                sectionTitle("Every line your balance moved through"), movementsTable);
        right.setPadding(new Insets(0, 0, 0, 6));
        return right;
    }

    // ---------- reading from the server ----------

    /** Everything on this screen, in two requests. */
    public void refresh() {
        lastUsers = null;
        lastMine = null;
        refreshUsers();
        refreshMine();
    }

    private ConnectionWatch watch = ConnectionWatch.NONE;

    /** Who to tell when the server stops, or starts, answering. */
    void setConnectionWatch(ConnectionWatch watch) {
        this.watch = watch == null ? ConnectionWatch.NONE : watch;
    }

    /** One tick of the poll timer: same two requests, redrawing only changes. */
    public void pollTick() {
        refreshUsers();
        refreshMine();
    }

    /** Kept for the shell, which calls it after a trade. */
    public void refreshBalance() {
        refresh();
    }

    private void refreshUsers() {
        Task<List<UserDto>> task = new Task<>() {
            @Override
            protected List<UserDto> call() {
                return engine.listUsers();
            }
        };
        task.setOnSucceeded(event -> {
            watch.reached();
            List<UserDto> users = task.getValue();
            if (users == null || users == lastUsers) {
                return;
            }
            lastUsers = users;
            List<UserRow> rows = new ArrayList<>();
            for (UserDto user : users) {
                rows.add(new UserRow(
                        user.name(),
                        user.balance(),
                        user.isMarketMaker() ? "Market maker" : "",
                        user.name().equalsIgnoreCase(userName)));
            }
            usersTable.setItems(FXCollections.observableArrayList(rows));
        });
        // Before this, a failed poll on this screen said nothing at all.
        task.setOnFailed(event -> watch.failed(task.getException()));
        start(task, "users");
    }

    private void refreshMine() {
        Task<UserStateDto> task = new Task<>() {
            @Override
            protected UserStateDto call() {
                return engine.userState(null);
            }
        };
        task.setOnSucceeded(event -> {
            UserStateDto me = task.getValue();
            if (me == null || me == lastMine) {
                return;
            }
            lastMine = me;
            balanceLabel.setText(String.format("Balance: %.2f", me.balance()));
            roleLabel.setText(me.isMarketMaker()
                    ? "You are the market maker of at least one event."
                    : "You are not a market maker.");
            showEvents(me.events());
            movementsTable.setItems(FXCollections.observableArrayList(
                    me.movements() == null ? List.of() : me.movements()));
        });
        task.setOnFailed(event -> watch.failed(task.getException()));
        start(task, "user-state");
    }

    private void showEvents(List<EventRoleDto> roles) {
        eventsBox.getChildren().clear();
        if (roles == null || roles.isEmpty()) {
            eventsBox.getChildren().add(new Label("You are not in any event yet."));
            return;
        }
        for (EventRoleDto role : roles) {
            StringBuilder line = new StringBuilder();
            line.append(role.eventName()).append("  -  ").append(role.statusDisplay());
            if (role.isMarketMaker()) {
                line.append("  -  you are the market maker");
            }
            Label heading = new Label(line.toString());
            heading.getStyleClass().add("role-heading");
            eventsBox.getChildren().add(heading);

            for (HoldingDto holding : role.holdings()) {
                eventsBox.getChildren().add(new Label(String.format(
                        "      %d of %s", holding.shares(), holding.optionName())));
            }
        }
    }

    // ---------- the two things this screen can do ----------

    private void chooseAndUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an exercise 3 events file");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files", "*.xml"));

        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        pathLabel.setText(file.getAbsolutePath());
        upload(file);
    }

    private void upload(File file) {
        loadButton.setDisable(true);
        showMessage("Sending " + file.getName() + " to the server...", false);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return engine.upload(file);
            }
        };
        task.setOnSucceeded(event -> {
            loadButton.setDisable(false);
            showMessage(task.getValue(), false);
            onMarketChanged.run();
            refresh();
        });
        task.setOnFailed(event -> {
            loadButton.setDisable(false);
            showMessage(describe(task.getException()), true);
        });
        start(task, "upload");
    }

    private void addFunds() {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText() == null
                    ? "" : amountField.getText().trim());
        } catch (NumberFormatException e) {
            showFunds("Enter an amount as a number.", true);
            return;
        }
        if (amount <= 0) {
            showFunds("The amount has to be above zero.", true);
            return;
        }

        fundsButton.setDisable(true);
        Task<Double> task = new Task<>() {
            @Override
            protected Double call() {
                return engine.addFunds(null, amount);
            }
        };
        task.setOnSucceeded(event -> {
            fundsButton.setDisable(false);
            amountField.setText("");
            showFunds(String.format("%.2f added.", amount), false);
            refresh();
            onMarketChanged.run();
        });
        task.setOnFailed(event -> {
            fundsButton.setDisable(false);
            showFunds(describe(task.getException()), true);
        });
        start(task, "funds");
    }

    // ---------- small things ----------

    /** A rejected file has a sentence and then one line per problem. */
    private String describe(Throwable cause) {
        if (cause instanceof LoadException load) {
            StringBuilder text = new StringBuilder(load.getMessage());
            for (String problem : load.getProblems()) {
                text.append(System.lineSeparator()).append("- ").append(problem);
            }
            return text.toString();
        }
        if (cause instanceof EngineException && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return cause == null ? "The request failed." : String.valueOf(cause);
    }

    /** Generic, because both tables have a money column over different rows. */
    private <S> TableCell<S, Number> moneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format("%.2f", value.doubleValue()));
            }
        };
    }

    private <S> TableCell<S, Number> signedMoneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText("");
                    return;
                }
                // The sign is the point of this column, so it is always shown.
                setText(String.format("%+.2f", value.doubleValue()));
            }
        };
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private void showMessage(String text, boolean bad) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().remove("load-message-bad");
        if (bad) {
            messageLabel.getStyleClass().add("load-message-bad");
        }
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showFunds(String text, boolean bad) {
        fundsMessage.setText(text);
        fundsMessage.getStyleClass().remove("load-message-bad");
        if (bad) {
            fundsMessage.getStyleClass().add("load-message-bad");
        }
        fundsMessage.setVisible(true);
        fundsMessage.setManaged(true);
    }

    private void start(Task<?> task, String name) {
        Thread worker = new Thread(task, name);
        worker.setDaemon(true);
        worker.start();
    }
}
