# Guess Market

A prediction market simulator in Java. Users trade shares in the outcome of real-world events, and the price of a share reads directly as the market's estimate of the probability — a share trading at 0.73 means the market thinks that outcome is 73% likely.

Two independent pricing mechanisms are implemented, which is the interesting part of the project:

- **LMSR** (Logarithmic Market Scoring Rule) — an automated market maker. You can always buy, because a formula quotes a price against a subsidised pool. No counterparty needed.
- **Order book (CLOB)** — a continuous limit order book, the way an exchange actually works. Buyers and sellers post orders and trade only when the prices cross.

Built as coursework for an Object-Oriented Programming module, over three exercises: a console application, then a JavaFX desktop client, then client–server.

---

## What it does

- Loads a market definition from XML, validated against a schema and then against the application's own rules
- Multiple users, each with an account that cannot go negative
- Each event has a **market maker** who funds it from his own money, opens it, and decides it at the end
- **LMSR trading** — buy shares at a price the formula quotes
- **Order book trading** — post limit orders, match against resting orders, and mint new shares when two opposite bids together cover the payout
- Live market statistics per option: last traded price, best bid, best ask, mid and spread
- Commission charged either on each purchase or on the winners at settlement
- Save and restore the whole system, trade history included

---

## Architecture

Two modules, shipped as two jars, with a strict one-way dependency.

```
ui      active   menus, input, every printed line, the JavaFX controllers
 |
 v
engine  passive  answers requests, never prints, does not know who is calling
```

Three decisions did most of the work:

**The engine is passive.** It exposes one interface, `GuessMarketEngine`. The console UI of exercise 1 and the JavaFX client of exercise 2 talk to exactly the same interface, and the concrete implementation is named in one place. Replacing the entire front end changed no engine code.

**Nothing crosses the boundary except immutable records.** Data leaves the engine as DTOs at every level, so the UI physically cannot reach a live domain object and mutate it. It costs a conversion step and buys the guarantee.

**Objects don't know about things above them.** An `Event` doesn't know users exist; a `User` doesn't know about events. When an order matches, the event works out who owes what into a `MatchResult` and hands it back — the engine is the only place the two meet. This is what let a whole second trading mechanism be added without touching the first.

---

## The trading mechanisms

### LMSR

Prices come from the cost function

```
C(q) = b · ln( Σ e^(qᵢ/b) )
```

where `qᵢ` is the shares outstanding in option *i* and `b` is a liquidity parameter. Buying costs `C(after) − C(before)`, and the market maker seeds the pool with `C(0,0) = b·ln(2)`.

The implementation holds `b` but not the quantities — those are passed in on every call — so the maths is testable in isolation from anything else.

One subtlety worth calling out: both formulae subtract the largest exponent before calling `Math.exp`. Without that, a large `q/b` overflows to `Infinity` and every price silently becomes `NaN`. No exception, just nonsense on screen. Buying a billion shares is a valid test case and it returns sane numbers.

### Order book

Bids sorted highest-first, asks lowest-first, ties broken by arrival order — price-time priority in two comparators. An incoming order walks the opposite side of the book, filling partially and consuming several resting orders if needed. The resting order sets the trade price, as an exchange does. Nobody trades with themselves.

**Minting** is the part specific to prediction markets. Because a YES share and a NO share together always pay out exactly the base value `d`, two buyers wanting opposite outcomes can have shares created for them out of nothing rather than needing a seller. If a bid of 0.60 on YES is resting and a bid of 0.55 on NO arrives, they sum to 1.15 — more than `d`. The resting order pays what it asked, 0.60, and the arriving order pays the complement, 0.40. So the pool receives exactly `d` per pair no matter how far the two prices overshoot, and the books always balance at settlement.

Orders are only accepted if the user can cover them, counting what is already committed to their resting orders. A match can therefore never fail halfway through and leave the ledger inconsistent.

---

## Running it

Requires **Java 25** and the **JavaFX SDK**.

```
java --module-path "path/to/javafx-sdk/lib" \
     --add-modules javafx.controls,javafx.fxml \
     -cp "ui.jar;engine.jar;lib/*" \
     guessmarket.ui.fx.GuessMarketApp
```

Sample market files are in `samples/`. `small.xml` has one event of each type; `multiple.xml` is larger; the `error-*.xml` files are deliberately invalid and exercise the validation.

---

## Project structure

```
engine/
  model/         Event, EventOption, Account, User, Transaction, enums
  pricing/       LmsrMarket — the only place the LMSR formulae appear
  orderbook/     Order, OrderBook, Trade, MatchResult — matching and minting
  xml/           JAXB loading and validation, generated bindings
  persistence/   save and restore
  api/           the interface, the DTOs, the one exception type

ui/
  fx/            JavaFX application, FXML views and controllers
```

---

## Notes on testing

The financial logic is checked by money conservation rather than by asserting individual numbers: open an event, trade, mint, close it, and the total across every account must equal what it was at the start. That catches a whole class of bug that reads fine line by line.

The LMSR side is verified against a worked example from the specification — with `b = 100`, buying 100 shares costs 62.01 and leaves prices at 0.73 / 0.27. The order book is verified against a reference simulation provided with the course, including the mint case, where 35 shares at 0.58 and 0.42 must put exactly 35.00 into the event account.
