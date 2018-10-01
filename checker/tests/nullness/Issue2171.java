import org.checkerframework.checker.nullness.qual.*;

public class Issue2171 {
    static void varArgsMethod(@PolyNull Object... args) {}

    static void callToVarArgsObject(
            @PolyNull Object pn, @NonNull Object nn, @Nullable Object nble) {
        varArgsMethod(nble, nble);
        varArgsMethod(nble, nn);
        varArgsMethod(nble, pn);
        varArgsMethod(nn, nble);
        varArgsMethod(nn, nn);
        varArgsMethod(nn, pn);
        varArgsMethod(pn, nble);
        varArgsMethod(pn, nn);
        varArgsMethod(pn, pn);
    }
}
