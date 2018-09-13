import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestUserCollection<E> extends ArrayList<E> {
    public @PolyDet("down") boolean contains(@PolyDet Object o) {
        return true;
    }

    void callContains(@OrderNonDet TestUserCollection<@Det Integer> list, @Det Object o) {
        @Det boolean result = list.contains(o);
    }

    // :: error: (invalid.element.type)
    void callContains1(@OrderNonDet TestUserCollection<@NonDet Integer> list) {}
}
