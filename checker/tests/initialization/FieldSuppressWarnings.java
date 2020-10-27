public class FieldSuppressWarnings {

    // :: error: (initialization.fields.uninitialized)
    static class FieldSuppressWarnings1 {
        private Object notInitialized;
    }

    static class FieldSuppressWarnings2 {
        @SuppressWarnings("initialization.fields.uninitialized")
        private Object notInitializedButSuppressed1;
    }

    static class FieldSuppressWarnings3 {
        @SuppressWarnings("initialization")
        private Object notInitializedButSuppressed2;
    }

    static class FieldSuppressWarnings4 {
        private Object initialized1;

        {
            initialized1 = new Object();
        }
    }

    static class FieldSuppressWarnings5 {
        private Object initialized2 = new Object();
    }
}
