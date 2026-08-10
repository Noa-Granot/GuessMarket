package guessmarket.engine.api;

import guessmarket.engine.model.CommissionType;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.MarketSystem;

import java.util.List;

/**
 * Two hand-written events, standing in for the XML loader until it exists.
 *
 * Event 1 uses b = 100 so its numbers match the worked example in appendix A:
 * the opening subsidy is 69.31, and buying 100 YES costs about 62.
 *
 * DELETE THIS CLASS once loadFile works.
 */
final class DemoData {

    private DemoData() {
    }

    static MarketSystem build() {
        MarketSystem system = new MarketSystem();

        system.addEvent(new Event(
                1,
                "Rain in Tel Aviv tomorrow",
                "Resolves YES if any measurable rain falls at the Tel Aviv station before midnight.",
                5,
                CommissionType.ON_PURCHASE,
                List.of("YES", "NO"),
                100));

        system.addEvent(new Event(
                2,
                "Bitcoin above 100k by year end",
                "Resolves YES if BTC closes above 100,000 USD on 31 December.",
                10,
                CommissionType.ON_CLOSE,
                List.of("YES", "NO"),
                50));

        return system;
    }
}
