// Test case for Issue 989
// https://github.com/typetools/checker-framework/issues/989
// @skip-test

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

interface ListWrapper989<E> extends List<@NonNull E> {}

public class Issue989 {

    void use(ListWrapper989<Boolean> list) {
        list.get(0);
    }
}
