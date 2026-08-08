// Code that is written inside a method but runs elsewhere -- the body of a lambda, or the body of
// a method of a class that is declared within the method -- is not a side effect of the enclosing
// method.  Each place where such code runs is checked on its own.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class NestedCodeSeonly {

  static int staticField;

  interface Runner {
    void run();
  }

  @SideEffectsOnly("this")
  void storesALambda() {
    Runner r = () -> staticField = 1;
  }

  @SideEffectsOnly("this")
  void invokesALambda() {
    Runner r = () -> staticField = 1;
    // `Runner.run` has no side-effect annotation, so it might modify anything.
    // :: error: (purity.unknown.sideeffectsonly)
    r.run();
  }

  @SideEffectsOnly("this")
  void declaresALocalClass() {
    class Local {
      int ownField;

      void modify() {
        ownField = 1;
        staticField = 1;
      }
    }
  }

  @SideEffectsOnly("this")
  void createsAnAnonymousClass() {
    // Creating the object runs the anonymous class's constructor, which has no side-effect
    // annotation.  The diagnostic names the anonymous class by its supertype.
    Runner r =
        // :: error: (purity.unknown.sideeffectsonly)
        new Runner() {
          @Override
          public void run() {
            staticField = 1;
          }
        };
  }
}
