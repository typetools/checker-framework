// Calling a constructor runs more than its body.  Unless the constructor delegates to another
// constructor of the same class, the class's instance initializers run first, and unless the
// constructor contains an explicit constructor call of either kind, the superclass's no-argument
// constructor runs before those.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ImplicitConstructorCode {

  static List<Integer> log = new ArrayList<>();

  @SideEffectsOnly("ImplicitConstructorCode.log")
  static int logAndReturn() {
    log.add(1);
    return 0;
  }

  static class UnannotatedSuper {
    UnannotatedSuper() {
      log.add(1);
    }
  }

  static class AnnotatedSuper {
    @SideEffectsOnly({"this", "ImplicitConstructorCode.log"})
    AnnotatedSuper() {
      log.add(1);
    }
  }

  static class ImplicitSuperCallIsChecked extends UnannotatedSuper {
    // The compiler inserts a call to `UnannotatedSuper()`, which has no side-effect annotation, so
    // it might modify arbitrary state.
    @SideEffectsOnly("this")
    // :: error: (purity.unknown.sideeffectsonly)
    ImplicitSuperCallIsChecked() {}
  }

  static class ImplicitSuperCallSideEffects extends AnnotatedSuper {
    // `AnnotatedSuper()` modifies `ImplicitConstructorCode.log`, which this annotation does not
    // list.
    @SideEffectsOnly("this")
    // :: error: (purity.incorrect.sideeffectsonly)
    ImplicitSuperCallSideEffects() {}
  }

  static class ImplicitSuperCallPermitted extends AnnotatedSuper {
    @SideEffectsOnly({"this", "ImplicitConstructorCode.log"})
    ImplicitSuperCallPermitted() {}
  }

  static class InstanceInitializersAreChecked {
    // A field initializer runs as part of every constructor that does not delegate to another
    // constructor of the same class.
    // :: error: (purity.incorrect.sideeffectsonly)
    int f = logAndReturn();

    {
      // :: error: (purity.incorrect.sideeffectsonly)
      log.add(1);
    }

    @SideEffectsOnly("this")
    InstanceInitializersAreChecked() {}

    @SideEffectsOnly({"this", "ImplicitConstructorCode.log"})
    InstanceInitializersAreChecked(int unused) {}

    // Delegating to another constructor of the same class does not run the instance initializers
    // a second time.
    @SideEffectsOnly("this")
    InstanceInitializersAreChecked(boolean unused) {
      // :: error: (purity.incorrect.sideeffectsonly)
      this(0);
    }
  }

  static class StaticInitializersAreNotChecked {
    // A static initializer runs at class initialization, not at construction.
    static int g = logAndReturn();

    static {
      log.add(1);
    }

    @SideEffectsOnly("this")
    StaticInitializersAreNotChecked() {}
  }
}
