// Test case for Issue 808
// https://github.com/typetools/checker-framework/issues/808

import java.util.Arrays;
import java.util.List;

public class Issue808 {
    void f() {
        Arrays.asList(0, 0, "", Arrays.asList(new Object[0]));
        foo(new Object(), bar());
        new Issue808(bar());
        foo(bar());
        List<Object> list = Arrays.asList(new Object[0]);
    }

    <T> T bar() {
        throw new RuntimeException();
    }

    void foo(Object... param) {}

    Issue808(Object... param) {}
}
