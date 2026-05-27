# taps-to-trips

A small Java 21 application that turns a CSV of bus-card tap events (`taps.csv`) into a CSV of
completed, incomplete, and cancelled trips (`trips.csv`), priced per a fixed fare table.

## Build & run

```bash
./gradlew build
./gradlew run --args="sample/taps.csv out/trips.csv"
```

If `--args` is omitted, defaults are `taps.csv` (input) and `trips.csv` (output) in the current
directory.

## Tests

```bash
./gradlew test
```

## Approach, assumptions, and design notes

See [docs below — filled in once the implementation lands] and the inline Javadoc on
`TripMatcher`.

<!-- README is scaffolded here and will be expanded in the final commit. -->
