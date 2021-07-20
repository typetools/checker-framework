// Test case for Issue 1406
// https://github.com/typetools/checker-framework/issues/1406

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.dataflow.qual.Pure;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"purity", "contracts.postcondition.not.satisfied"}) // Only test parsing
public class Issue1406 {

    public static void main(String[] args) {}

    @Pure
    @EnsuresNonNull("myMethod(#1).get(0)")
    List<String> myMethod(int arg) {
        List<String> result = new ArrayList<>();
        result.add("non-null value");
        return result;
    }

    String client(int arg) {
        return myMethod(arg).get(0);
    }

    @Pure
    @EnsuresNonNull("myMethod2().get(0)")
    List<String> myMethod2() {
        List<String> result = new ArrayList<>();
        result.add("non-null value");
        return result;
    }

    String client2() {
        return myMethod2().get(0);
    }
}
