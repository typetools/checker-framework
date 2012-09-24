import com.sun.source.tree.Tree;

import checkers.nullness.quals.*;
import checkers.quals.*;

public class VoidUse {

  // Void is treated as Nullable, therefore also Class<Void>
  // is Nullable. Should this fail? The upper bound of the
  // wildcard is implicitly Nullable (because of the bound in
  // Class), therefore it passes.
  //TODO:: error: (assignment.type.incompatible)
  private Class<?> main_class1 = Void.TYPE;

  private Class<? extends @Nullable Object> main_class2 = Void.TYPE;

  public Void voidReturn(Void p) {
      voidReturn(null);
      return null;
  }

  // Void is treated as Nullable.  Is there a value on having it be
  // NonNull?
  public static abstract class VoidTestNode<T> { }

  public static class VoidTestInvNode extends VoidTestNode<@NonNull Void> { }

  class Scanner<P> {
    public void scan(Object tree, P p) {}
  }

  //:: error: (type.argument.type.incompatible)
  class MyScanner extends Scanner<Void> {
    void use(MyScanner ms) {
      ms.scan(new Object(), null);
    }
  }

  //:: error: (type.argument.type.incompatible)
  class MyScanner2 extends Scanner<@Nullable Object> {
    void use(MyScanner2 ms) {
      ms.scan(new Object(), null);
    }
  }
}
