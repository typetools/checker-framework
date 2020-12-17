// Test case for issue #341:
// https://github.com/typetools/checker-framework/issues/341

public class Issue341 {

    static class Provider {
        public final Object get = new Object();
    }

    Object execute(Provider p) {
        final Object result;
        try {
            result = p.get;
        } finally {
        }
        return result;
    }
}
