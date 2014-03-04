import checkers.nullness.quals.*;

public class HasInnerClass<E> {
  public class InternalEdge {
    public void m() {
      HasInnerClass<?>.InternalEdge other = null;
    }
  }
	
}
