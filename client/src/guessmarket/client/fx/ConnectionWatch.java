package guessmarket.client.fx;

/**
 * How the two screens tell the window whether the server is still there.
 *
 * Every poll reports here: reached() when an answer came back, failed() when
 * it did not. The window turns that into one banner across the top, so a dead
 * server is announced once and in the same place, whichever tab is showing.
 */
interface ConnectionWatch {

    void reached();

    void failed(Throwable cause);

    /** For a screen used before the window has introduced itself. */
    ConnectionWatch NONE = new ConnectionWatch() {
        @Override
        public void reached() {
        }

        @Override
        public void failed(Throwable cause) {
        }
    };
}
