import checkers.nullness.quals.*;
import java.util.*;

class AnnotatedTypeParams {
    // @Nullable T is to be treated as <T extends @Nullable Object>
    <@Nullable T> T id(T t) {
        //:: (type.incompatible)
        return null;
    }

    void testId() {
        String a = "mark";
        String b = id(a);
        b.toCharArray();
    }

    void wildcards() {
        List<@Nullable ?> list = new ArrayList<@NonNull String>();
    }
}
