package guessmarket.ui.fx;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * BONUS: three short animations, all well under two seconds.
 *
 * They are off until the person turns them on, so nothing here can slow down
 * someone who would rather the program just responded. When they are off every
 * method returns immediately and the node is left exactly as it was.
 */
final class Animations {

    private static boolean enabled = false;

    private Animations() {
    }

    static void setEnabled(boolean on) {
        enabled = on;
    }

    static boolean isEnabled() {
        return enabled;
    }

    /** Used when the details panel switches to a different event or user. */
    static void fadeIn(Node node) {
        if (!enabled || node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /** Used on the panel that changed after a trade, so the change is noticed. */
    static void pulse(Node node) {
        if (!enabled || node == null) {
            return;
        }
        ScaleTransition scale = new ScaleTransition(Duration.millis(140), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.03);
        scale.setToY(1.03);
        scale.setCycleCount(2);
        scale.setAutoReverse(true);
        scale.play();
    }

    /** Used when an action is refused, in place of only a dialog. */
    static void shake(Node node) {
        if (!enabled || node == null) {
            return;
        }
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), node);
        shake.setFromX(0);
        shake.setByX(7);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }
}
