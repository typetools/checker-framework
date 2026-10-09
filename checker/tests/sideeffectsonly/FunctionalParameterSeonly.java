// A @SideEffectsOnly method may call the functional method of one of its functional-interface
// parameters.  Every call to the method requires the argument to be side-effect-free, so the call
// side-effects nothing.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class FunctionalParameterSeonly {

  int count;

  @SideEffectsOnly("this")
  void runs(Runnable r) {
    r.run();
    count++;
  }

  /** The assumption does not apply to an alias of the parameter. */
  @SideEffectsOnly("this")
  void runsAlias(Runnable r) {
    Runnable a = r;
    // :: error: (purity.unknown.sideeffectsonly)
    a.run();
  }

  void callers() {
    runs(() -> {});
    // :: error: (purity.assign.field)
    runs(() -> count++);
  }

  static class Unannotated {
    void m(Runnable r) {}
  }

  /** A call through Unannotated.m checks nothing about r. */
  static class OverridesUnannotated extends Unannotated {
    @Override
    @SideEffectsOnly("this")
    void m(Runnable r) {
      // :: error: (purity.unknown.sideeffectsonly)
      r.run();
    }
  }

  static class Annotated {
    @SideEffectsOnly("this")
    void m(Runnable r) {
      r.run();
    }
  }

  interface AnnotatedInterface {
    @SideEffectsOnly("this")
    void m(Runnable r);
  }

  /**
   * The override inherits @SideEffectsOnly, so calls through it require r to be side-effect-free.
   */
  static class InheritsSideEffectsOnly implements AnnotatedInterface {
    @Override
    public void m(Runnable r) {
      r.run();
    }
  }

  /** A call through Annotated.m requires r to be side-effect-free. */
  static class OverridesAnnotated extends Annotated {
    @Override
    @SideEffectsOnly("this")
    void m(Runnable r) {
      r.run();
    }
  }
}
