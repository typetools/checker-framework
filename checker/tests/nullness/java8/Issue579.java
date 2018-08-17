// Test case for Issue579
// https://github.com/typetools/checker-framework/issues/579

import java.util.Comparator;

class Issue570<T> implements Comparator<T> {
    private final Comparator<T> real;

    @SuppressWarnings("unchecked")
    Issue570(Comparator<? super T> real) {
        this.real = (Comparator<T>) real;
    }

    @Override
    public int compare(T a, T b) {
        throw new RuntimeException();
    }

    @Override
    public Comparator<T> thenComparing(Comparator<? super T> other) {
        // :: warning: (known.nonnull)
        return new Issue570<>(real == null ? other : real.thenComparing(other));
    }
}
