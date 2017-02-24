// Test case for Issue 979:
// https://github.com/typetools/checker-framework/issues/979

// @below-java8-jdk-skip-test

import java.util.List;

@SuppressWarnings("nullness") // don't bother with implementations
class ISOuter {
    static <V> List<V> wrap(V value) {
        return null;
    }

    static <T> List<T> empty() {
        return null;
    }
}

class InferenceSimpler {
    List<List<String>> foo() {
        return ISOuter.wrap(ISOuter.empty());
    }
}
