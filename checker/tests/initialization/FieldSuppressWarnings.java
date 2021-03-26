public class FieldSuppressWarnings {

  static class FieldSuppressWarnings1 {
    // :: error: (initialization.field.uninitialized)
    private Object notInitialized;
  }

  static class FieldSuppressWarnings2 {
    @SuppressWarnings("initialization.field.uninitialized")
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
