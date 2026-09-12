package guessmarket.ui.fx;

/**
 * BONUS: the colour schemes the person can switch between.
 *
 * Each is one stylesheet, and only colours, fonts and sizes differ. Nothing
 * moves, so the layout still matches the sketch whichever is chosen.
 */
enum Skin {
    DEFAULT("Default", "app.css"),
    DARK("Dark", "skin-dark.css"),
    WARM("Warm", "skin-warm.css");

    private final String display;
    private final String stylesheet;

    Skin(String display, String stylesheet) {
        this.display = display;
        this.stylesheet = stylesheet;
    }

    String getDisplay() {
        return display;
    }

    String getStylesheet() {
        return stylesheet;
    }

    static Skin byDisplay(String display) {
        for (Skin skin : values()) {
            if (skin.display.equals(display)) {
                return skin;
            }
        }
        return DEFAULT;
    }
}
