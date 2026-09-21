package guessmarket.client.fx;

import guessmarket.client.net.HttpGuessMarketEngine;
import guessmarket.engine.api.EventStateDto;
import guessmarket.engine.api.OptionStateDto;
import guessmarket.engine.api.QuoteDto;

import javafx.concurrent.Task;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asks which option to buy and how many shares, and shows what it would cost
 * before anything is bought.
 *
 * The exercise 2 version asked the engine for a quote on every keystroke, which
 * was free when the engine was in this process. It is an HTTP request now, so
 * the quote is fetched on a background thread and the dialog stays usable while
 * it is in flight. Each request carries a number, and an answer is thrown away
 * if the person has typed again since it was sent — otherwise a slow reply for
 * "10" could land after a quick one for "100" and show the wrong cost.
 */
class BuyDialog extends Dialog<BuyDialog.Choice> {

    /** What the user chose. Option index starts at 0, as the engine expects. */
    record Choice(int optionIndex, long quantity) {
    }

    private final ComboBox<String> optionBox = new ComboBox<>();
    private final TextField quantityField = new TextField();
    private final Label quoteLabel = new Label();

    private final HttpGuessMarketEngine engine;
    private final EventStateDto state;

    /** Which quote request is the current one. */
    private final AtomicLong latestRequest = new AtomicLong();

    BuyDialog(HttpGuessMarketEngine engine, EventStateDto state, String buyer) {
        this.engine = engine;
        this.state = state;

        setTitle("Buy shares");
        setResizable(true);
        setHeaderText(buyer + " buying in \"" + state.name() + "\"");

        List<OptionStateDto> options = state.options();
        for (OptionStateDto option : options) {
            optionBox.getItems().add(option.name());
        }
        optionBox.getSelectionModel().selectFirst();
        quantityField.setPromptText("whole number above zero");

        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(300);
        quoteLabel.setMinHeight(Region.USE_PREF_SIZE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPrefWidth(380);
        grid.add(new Label("Option:"), 0, 0);
        grid.add(optionBox, 1, 0);
        grid.add(new Label("Shares:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("Cost:"), 0, 2);
        grid.add(quoteLabel, 1, 2);
        getDialogPane().setContent(grid);

        ButtonType buy = new ButtonType("Buy", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(buy, ButtonType.CANCEL);

        // Keep the dialog open on a bad entry so the reason stays visible.
        getDialogPane().lookupButton(buy).addEventFilter(ActionEvent.ACTION, event -> {
            if (parseQuantity() == null) {
                quoteLabel.setText("Enter a whole number of shares above zero.");
                resizeToFit();
                event.consume();
            }
        });

        quantityField.textProperty().addListener((observable, was, now) -> updateQuote());
        optionBox.valueProperty().addListener((observable, was, now) -> updateQuote());
        updateQuote();

        setResultConverter(button -> {
            if (button != buy) {
                return null;
            }
            Long quantity = parseQuantity();
            if (quantity == null) {
                return null;
            }
            return new Choice(optionBox.getSelectionModel().getSelectedIndex(), quantity);
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

    private void updateQuote() {
        Long quantity = parseQuantity();
        if (quantity == null) {
            quoteLabel.setText("Enter a whole number of shares above zero.");
            return;
        }

        int optionIndex = optionBox.getSelectionModel().getSelectedIndex();
        if (optionIndex < 0) {
            return;
        }

        long request = latestRequest.incrementAndGet();
        quoteLabel.setText("Asking the server...");

        Task<QuoteDto> task = new Task<>() {
            @Override
            protected QuoteDto call() {
                return engine.quote(state.id(), optionIndex, quantity);
            }
        };

        task.setOnSucceeded(event -> {
            if (request != latestRequest.get()) {
                return; // the person typed again; this answer is stale
            }
            QuoteDto quote = task.getValue();
            quoteLabel.setText(String.format(
                    "%.2f for the shares, %.2f commission, %.2f in total.",
                    quote.shareCost(), quote.commission(), quote.total()));
            resizeToFit();
        });

        task.setOnFailed(event -> {
            if (request != latestRequest.get()) {
                return;
            }
            Throwable cause = task.getException();
            quoteLabel.setText(cause == null ? "The quote failed." : cause.getMessage());
            resizeToFit();
        });

        Thread worker = new Thread(task, "quote");
        worker.setDaemon(true);
        worker.start();
    }

    /** null when the field does not hold a whole number above zero. */
    private Long parseQuantity() {
        String text = quantityField.getText() == null ? "" : quantityField.getText().trim();
        try {
            long value = Long.parseLong(text);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
