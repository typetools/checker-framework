import java.util.List;
import java.util.ArrayList;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.dataflow.qual.*;

// Test case for a mysterious error.
abstract class EnsuresNonNullIfTest2a {
    @EnsuresNonNullIf(result=true, expression="xxx")
    abstract boolean isFoo();

    boolean foobar() {
        List<String> list = new ArrayList<String>();

        // Remove the label and the error goes away,
        // see version ...2b below.
        aloop:
            for (;;) {
                isFoo();

                // One error for dereferencing possibly-null list
                return list.size() != 5;
            }
    }
}

abstract class EnsuresNonNullIfTest2b {
    @EnsuresNonNullIf(result=true, expression="xxx")
    abstract boolean isFoo();

    boolean foobar() {
        List<String> list = new ArrayList<String>();

        // Remove the label and the error goes away
        // aloop:
            for (;;) {
                isFoo();

                // assert list != null : "@AssumeAssertion(nullness)";

                // One error for dereferencing possibly-null split_children
                return list.size() != 5;
            }
    }
}
