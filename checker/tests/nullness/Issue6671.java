package open.falsepos;

// Use the Nullness Checker
public interface Issue6671<E extends Exception> {
  Object _apply(Object t) throws E;

  default Object apply(Object t) {
    try {
      return _apply(t);
    } catch (Exception e) {
      sneakyThrow(e);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void sneakyThrow(final Throwable x) throws T {
    throw (T) x;
  }
}
