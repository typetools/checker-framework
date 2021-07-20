import org.checkerframework.checker.index.qual.IndexFor;

import java.text.DecimalFormat;

public final class Stopwatch {
    private static final DecimalFormat[] timeFormat = {
        new DecimalFormat("#.#"),
        new DecimalFormat("#.#"),
        new DecimalFormat("#.#"),
        new DecimalFormat("#.#"),
        new DecimalFormat("#.#"),
    };

    public DecimalFormat format(@IndexFor("Stopwatch.timeFormat") int digits) {
        return Stopwatch.timeFormat[digits];
    }
}
