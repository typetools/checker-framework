import java.util.*;
import org.checkerframework.checker.tainting.qual.*;

public class Issue2330<T extends @Tainted Object> {
    // Checker can't verify that this creates an untainted Issue2330
    @SuppressWarnings("tainting")
    public @Untainted Issue2330(@PolyTainted int i) {}

    // Checker can't verify that this creates an untainted Issue2330
    @SuppressWarnings("tainting")
    public @Untainted Issue2330() {}

    public static void f(@PolyTainted int i) {
        new @Untainted Issue2330<@PolyTainted Integer>(i);
        new @Untainted Issue2330<@PolyTainted Integer>();
    }
}
