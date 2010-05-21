import checkers.nullness.quals.*;
public class VoidUse {

  private Class<?> main_class = Void.TYPE;

  public Void voidReturn(Void p) {
      voidReturn(null);
      return null;
  }

  // Void is treated as Nullable.  Is there a value on having it be
  // NonNull?
  public static abstract class VoidTestNode<T> { }

  public static class VoidTestInvNode extends VoidTestNode<@NonNull Void> { }

}

