// Test case for Issue 2480:
// https://github.com/typetools/checker-framework/issues/2480

import java.util.List;

@SuppressWarnings({"unchecked", ""}) // check for crashes only
abstract class Issue2480 {
    void testCase() {
        for (Class<?> wrapperType : of(Character.class, Boolean.class)) {}
    }

    abstract <E> List<E> of(E... e1);
}
