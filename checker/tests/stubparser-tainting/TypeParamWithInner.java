import org.checkerframework.checker.tainting.qual.Untainted;

// T extends @Untainted String in stub file. Tests bug where the presence of an inner class causes
// annotations on type variables to be forgotten.
public class TypeParamWithInner<T extends String> {
  public class Inner {}

  public void requiresUntainted(T param) {
    @Untainted String s = param;
  }
}
