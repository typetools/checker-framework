import org.checkerframework.checker.guieffect.qual.UIEffect;

public class TransitiveInheritance {

  public static class TopLevel {
    // Implicitly safe
    public void foo() {}
  }

  public static interface ITop {
    public void bar();
  }

  public static interface IIndirect {
    public void baz();
  }

  // Mid-level class and interface that do not redeclare or override the default safe methods
  public abstract static class MidLevel extends TopLevel implements IIndirect {}

  public static interface IMid extends ITop {}

  // Issue #3287 is that if foo or bar is overridden with a @UIEffect implementation here, the
  // "skip" in declarations causes the override error to not be issued
  // We check both classes and interfaces; the reported issue is related only to methods whose
  // nearest explicit definition lives in an interface
  public static class Base extends MidLevel implements IMid {

    // Should catch when the override is for a method originating in a class two levels up (here
    // TopLevel)
    @Override
    @UIEffect
    // :: error: (override.effect.invalid)
    public void foo() {}

    // Should catch when the override is for a method originating in an interface two levels up
    // (here ITop)
    @Override
    @UIEffect
    // :: error: (override.effect.invalid)
    public void bar() {}

    // Should catch when the override is for a method originating in an interface two levels up,
    // but which is implemented via class inheritance (here IIndirect, which is implemented by
    // MidLevel)
    @Override
    @UIEffect
    // :: error: (override.effect.invalid)
    public void baz() {}
  }

  public static interface IBase extends IMid {
    @Override
    @UIEffect
    // :: error: (override.effect.invalid)
    public void bar();
  }
}
