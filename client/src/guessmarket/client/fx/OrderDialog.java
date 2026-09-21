package guessmarket.client.fx;

import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.OptionStateDto;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.util.List;

/**
 * Asks for an order: which option, buy or sell, how many, and at what price.
 *
 * Nothing here talks to the server. Everything it needs is already in the
 * event state the details panel fetched, so the dialog is the same as it was
 * in exercise 2. The price ceiling is checked here as well as in the engine,
 * so the person is told before the request is made rather than after.
 */
class OrderDialog extends Dialog<OrderDialog.Choice> {

    record Choice(int optionIndex, String side, long quantity, double price) {
    }

    private static final String BUY = "Buy";
    private static final String SELL = "Sell";

    private final ComboBox<String> optionBox = new ComboBox<>();
    private final ComboBox<String> sideBox = new ComboBox<>();
    private final TextField quantityField = new TextField();
    private final TextField priceField = new TextField();
    private final Label hint = new Label();

    private final double highestPrice;

    OrderDialog(EventStateDto state, String trader, int preselectedOption) {
        setTitle("Place an order");
        setResizable(true);
        setHeaderText(trader + " trading in \"" + state.name() + "\"");

        double d = state.basePrice() == null ? 1.0 : state.basePrice();
        highestPrice = d - 0.01;

        List<OptionStateDto> options = state.options();
        for (OptionStateDto option : options) {
            optionBox.getItems().add(option.name());
        }
        optionBox.getSelectionModel().select(
                Math.max(0, Math.min(preselectedOption, options.size() - 1)));

        sideBox.getItems().addAll(BUY, SELL);
        sideBox.getSelectionModel().selectFirst();

        quantityField.setPromptText("whole number above zero");
        priceField.setPromptText(String.format("up to %.2f", highestPrice));

        hint.setWrapText(true);
        hint.setMaxWidth(300);
        hint.setMinHeight(Region.USE_PREF_SIZE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPrefWidth(380);
        grid.add(new Label("Option:"), 0, 0);
        grid.add(optionBox, 1, 0);
        grid.add(new Label("Action:"), 0, 1);
        grid.add(sideBox, 1, 1);
        grid.add(new Label("Shares:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(hint, 1, 4);
        getDialogPane().setContent(grid);

        ButtonType submit = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

        // Keep the dialog open on a bad entry so the reason stays visible.
        getDialogPane().lookupButton(submit).addEventFilter(ActionEvent.ACTION, event -> {
            String problem = firstProblem();
            if (problem != null) {
                hint.setText(problem);
                resizeToFit();
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button != submit || firstProblem() != null) {
                return null;
            }
            return new Choice(
                    optionBox.getSelectionModel().getSelectedIndex(),
                    sideBox.getValue(),
                    parseQuantity(),
                    parsePrice());
        });
    }


    /**
     * A dialog is sized when it opens. The hint below wraps onto a second line
     * as soon as there is something to say, which makes the content taller than
     * the window and pushes the buttons off the bottom. Asking the window to
     * size itself to its contents again is what keeps Submit reachable.
     */
    private void resizeToFit() {
        javafx.scene.Scene scene = getDialogPane().getScene();
        if (scene != null && scene.getWindow() != null) {
            scene.getWindow().sizeToScene();
        }
    }

    /** The first thing wrong with what has been entered, or null. */
    private String firstProblem() {
        long quantity = parseQuantity();
        if (quantity <= 0) {
            return "Enter a whole number of shares above zero.";
        }
        double price = parsePrice();
        if (price <= 0) {
            return "Enter a price above zero.";
        }
        if (price > highestPrice) {
            return String.format("The price cannot be above %.2f, which is the base value "
                    + "minus a penny.", highestPrice);
        }
        return null;
    }

    private long parseQuantity() {
        String text = quantityField.getText() == null ? "" : quantityField.getText().trim();
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double parsePrice() {
        String text = priceField.getText() == null ? "" : priceField.getText().trim();
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
