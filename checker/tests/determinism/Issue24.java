import org.checkerframework.checker.determinism.qual.*;

// @skip-test
public class Issue24 {
    public static String formatInts(@Det int i, @PolyDet int j) {
        return String.format("%i %i", i, j);
    }

    public static String formatInts2(@PolyDet int i, @Det int j) {
        return String.format("%i %i", i, j);
    }
}
