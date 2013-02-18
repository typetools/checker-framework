import checkers.nullness.quals.*;

class AnnotatedGenerics2 {
// Top-level class to ensure that both classes are processed.

//:: error: (commitment.fields.uninitialized)
class AnnotatedGenerics2Nble<T extends @Nullable Object> {
    @NonNull T myFieldNN;
    @Nullable T myFieldNble;
    T myFieldT;

    void fields1() {
        myFieldNN = myFieldNN;
        myFieldNble = myFieldNN;
        myFieldT = myFieldNN;
    }

}
}