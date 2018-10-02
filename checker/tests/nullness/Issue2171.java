import java.util.List;
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

    @SuppressWarnings("unchecked")
    static void genVarArgsMethod(List<? extends @PolyNull Object>... args) {}

    @SuppressWarnings("unchecked")
    static void genCallToVarArgsObject(
            List<@PolyNull Object> pn, List<@NonNull Object> nn, List<@Nullable Object> nble) {
        genVarArgsMethod(nble, nble);
        genVarArgsMethod(nble, nn);
        genVarArgsMethod(nble, pn);
        genVarArgsMethod(nn, nble);
        genVarArgsMethod(nn, nn);
        genVarArgsMethod(nn, pn);
        genVarArgsMethod(pn, nble);
        genVarArgsMethod(pn, nn);
        genVarArgsMethod(pn, pn);
    }
}
