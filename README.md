# taps-to-trips

A small Java 21 application that turns a CSV of bus-card tap events (`taps.csv`) into a CSV of
completed, incomplete, and cancelled trips (`trips.csv`), priced per a fixed fare table.

## Build & run

Requires **JDK 21**. The Gradle wrapper is checked in; no other tooling is needed.

```bash
./gradlew build
./gradlew run --args="sample/taps.csv out/trips.csv"
```

If `--args` is omitted, the defaults are `taps.csv` (input) and `trips.csv` (output) in the current
directory.

You can also run the produced jar directly:

```bash
java -jar build/libs/taps-to-trips.jar sample/taps.csv out/trips.csv
```

## Tests

```bash
./gradlew test
```

The test suite covers fare lookups, trip matching/classification, CSV reading and writing, and one
end-to-end integration test that runs `App.run` against `sample/taps.csv` and asserts the output
matches `sample/trips.csv` exactly.

## Assumptions

- **Input ordering is not trusted.** Rows are sorted by `DateTimeUTC` within each matching group
  before pairing, so out-of-order input is handled.
- **Orphan OFFs** (an OFF with no preceding ON in the same group) are treated as `INCOMPLETE`,
  priced as the maximum fare reachable from the OFF stop, and a warning is logged.
- **Two consecutive ONs** in the same group close the prior ON as `INCOMPLETE` (no implicit OFF).

## Approach

- **Matching key: `(PAN, CompanyId, BusID)`.** A passenger's tap sequence on one bus, for one
  operator. This is what gets paired into trips; different buses for the same PAN are independent.
- **Output sort: `Started` ascending**, with orphan OFFs (which have no `Started`) falling back to
  `Finished`. Produces a stable, easily-diffed output regardless of input order.
- **Money: `BigDecimal`** formatted as `$X.XX`, `HALF_UP` rounded to 2 decimals. A dedicated
  Money type is overkill here — single currency, no FX. 
- **Fares are hard-coded** in `FareTable.standard()` to match the spec. The `FareTable(Map)`
  constructor is the extension seam — a production version could load fares from config or a
  database without touching the matcher.

## Project layout

```
src/main/java/tapstotrips/
├── App.java                       CLI entry point
├── model/{Tap,Trip,TapType,TripStatus}.java
├── pricing/FareTable.java         fares + max-from-stop
├── matching/TripMatcher.java      ON/OFF pairing + classification
└── csv/{TapCsvReader,TripCsvWriter}.java
sample/
├── taps.csv                       the spec's example input
└── trips.csv                      expected output (also used as the integration-test fixture)
```

## Notes

- PANs in `sample/taps.csv` are Worldpay test card numbers (`5500005555555559`,
  `4111111111111111`) — no real card numbers are used anywhere in the project.
