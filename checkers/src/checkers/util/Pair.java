package checkers.util;

/**
 * Simple pair class for multiple returns.
 *
 * TODO: as class is immutable, use @Covariant annotation.
 */
public class Pair<V1, V2> {
    public final V1 first;
    public final V2 second;

    private Pair(V1 v1, V2 v2) {
        this.first = v1;
        this.second = v2;
    }

    public static <V1, V2> Pair<V1, V2> of(V1 v1, V2 v2) {
        return new Pair<V1, V2>(v1, v2);
    }
    
    @Override
    public String toString() {
        return "Pair(" + first + ", " + second + ")";
    }
}
