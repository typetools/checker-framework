public class TypeVarInstanceOf {
  public static <T> void clone(final T obj) {
    if (obj instanceof Cloneable) {}
  }
}
