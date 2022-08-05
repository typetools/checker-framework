public class StringConcatenation<V1, V2> {
    public final V1 first;
    public final V2 second;

    private StringConcatenation(V1 v1, V2 v2) {
        this.first = v1;
        this.second = v2;
    }

    public static <V1, V2> StringConcatenation<V1, V2> of(V1 v1, V2 v2) {
        return new StringConcatenation<>(v1, v2);
    }

    public String toString() {
        return "StringConcatenation(" + first + ", " + second + ")";
    }
}
