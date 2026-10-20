import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

public class Issue2171 {
  static void varArgsMethod(@PolyNull Object... args) {}

  static void callToVarargsObject(@PolyNull Object pn, @NonNull Object nn, @Nullable Object nble) {
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
  static void genVarargsMethod(List<? extends @PolyNull Object>... args) {}

  @SuppressWarnings("unchecked")
  static void genCallToVarargsObject(
      List<@PolyNull Object> pn, List<@NonNull Object> nn, List<@Nullable Object> nble) {
    genVarargsMethod(nble, nble);
    genVarargsMethod(nble, nn);
    genVarargsMethod(nble, pn);
    genVarargsMethod(nn, nble);
    genVarargsMethod(nn, nn);
    genVarargsMethod(nn, pn);
    genVarargsMethod(pn, nble);
    genVarargsMethod(pn, nn);
    genVarargsMethod(pn, pn);
  }
}
