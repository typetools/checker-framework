import org.checkerframework.checker.nullness.qual.*;

public class HasInnerClass<E> {
  public class InternalEdge {
    public void m() {
      HasInnerClass<?>.InternalEdge other = null;
    }
  }
}
