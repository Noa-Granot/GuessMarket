package guessmarket.ui.fx;

import guessmarket.engine.api.NewEventSpec;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * BONUS: collects a new event from the person creating it.
 *
 * The fields that only apply to one trading method appear and disappear with
 * the method, so there is never a box on screen that would be ignored.
 */
class NewEventDialog extends Dialog<NewEventSpec> {

    private static final String LMSR = "LMSR";
    private static final String ORDER_BOOK = "Order Book";

    private final TextField nameField = new TextField();
    private final TextArea descriptionField = new TextArea();
    private final TextField commissionField = new TextField("5");
    private final ComboBox<String> commissionTypeBox = new ComboBox<>();
    private final TextField optionOneField = new TextField();
    private final TextField optionTwoField = new TextField();
    private final ComboBox<String> methodBox = new ComboBox<>();

    private final TextField liquidityField = new TextField("100");
    private final TextField initialField = new TextField("100");
    private final TextField baseField = new TextField("1");
    private final CheckBox mintBox = new CheckBox("allow minting");

    private final Label lmsrLabel = new Label("Liquidity b:");
    private final Label initialLabel = new Label("Initial shares:");
    private final Label baseLabel = new Label("Base value d:");

    private final Label hint = new Label();

    NewEventDialog(String creator) {
        setTitle("Create an event");
        setHeaderText(creator + " will be its market maker");

        descriptionField.setPrefRowCount(2);
        descriptionField.setWrapText(true);
        commissionTypeBox.getItems().addAll("on-purchase", "on-close");
        commissionTypeBox.getSelectionModel().selectFirst();
        methodBox.getItems().addAll(LMSR, ORDER_BOOK);
        methodBox.getSelectionModel().selectFirst();
        mintBox.setSelected(true);

        hint.setWrapText(true);
        hint.setMaxWidth(320);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(7);
        grid.setPrefWidth(420);
        int row = 0;
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionField, 1, row++);
        grid.add(new Label("Option 1:"), 0, row);
        grid.add(optionOneField, 1, row++);
        grid.add(new Label("Option 2:"), 0, row);
        grid.add(optionTwoField, 1, row++);
        grid.add(new Label("Commission %:"), 0, row);
        grid.add(commissionField, 1, row++);
        grid.add(new Label("Charged:"), 0, row);
        grid.add(commissionTypeBox, 1, row++);
        grid.add(new Label("Method:"), 0, row);
        grid.add(methodBox, 1, row++);
        grid.add(lmsrLabel, 0, row);
        grid.add(liquidityField, 1, row++);
        grid.add(initialLabel, 0, row);
        grid.add(initialField, 1, row++);
        grid.add(baseLabel, 0, row);
        grid.add(baseField, 1, row++);
        grid.add(mintBox, 1, row++);
        grid.add(hint, 0, row, 2, 1);
        getDialogPane().setContent(grid);

        methodBox.valueProperty().addListener((o, a, b) -> updateVisibility());
        updateVisibility();

        ButtonType create = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);

        // Keep the dialog open on a bad entry so the reason stays visible.
        getDialogPane().lookupButton(create).addEventFilter(ActionEvent.ACTION, event -> {
            if (build() == null) {
                event.consume();
            }
        });

        setResultConverter(button -> button == create ? build() : null);
    }

    /** Only the fields that belong to the chosen method are shown. */
    private void updateVisibility() {
        boolean orderBook = ORDER_BOOK.equals(methodBox.getSelectionModel().getSelectedItem());
        setShown(lmsrLabel, !orderBook);
        setShown(liquidityField, !orderBook);
        setShown(initialLabel, orderBook);
        setShown(initialField, orderBook);
        setShown(baseLabel, orderBook);
        setShown(baseField, orderBook);
        setShown(mintBox, orderBook);
    }

    private void setShown(javafx.scene.Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /**
     * Returns null if anything is unusable, which keeps the dialog open. The
     * engine checks all of this again, because it cannot trust a caller.
     */
    private NewEventSpec build() {
        boolean orderBook = ORDER_BOOK.equals(methodBox.getSelectionModel().getSelectedItem());

        if (nameField.getText().isBlank()
                || optionOneField.getText().isBlank()
                || optionTwoField.getText().isBlank()) {
            hint.setText("Name and both options are required.");
            return null;
        }

        Integer commission = whole(commissionField.getText());
        if (commission == null || commission < 0 || commission > 90) {
            hint.setText("Commission: a whole number 0 to 90.");
            return null;
        }

        int b = 0;
        int initial = 0;
        int base = 0;
        if (orderBook) {
            Integer i = whole(initialField.getText());
            Integer d = whole(baseField.getText());
            if (i == null || i <= 0 || d == null || d <= 0) {
                hint.setText("Initial shares and base value must be above 0.");
                return null;
            }
            initial = i;
            base = d;
        } else {
            Integer value = whole(liquidityField.getText());
            if (value == null || value <= 0) {
                hint.setText("Liquidity b: a whole number above 0.");
                return null;
            }
            b = value;
        }

        return new NewEventSpec(
                nameField.getText().trim(),
                descriptionField.getText().trim(),
                commission,
                commissionTypeBox.getSelectionModel().getSelectedItem(),
                List.of(optionOneField.getText().trim(), optionTwoField.getText().trim()),
                orderBook, b, initial, base, mintBox.isSelected());
    }

    private Integer whole(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
