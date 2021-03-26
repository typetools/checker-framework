import org.checkerframework.framework.testchecker.util.*;

public abstract class MethodOverrides {

  public abstract @Odd String method();

  public abstract String methodSub();

  public abstract void param(@Odd String s);

  public abstract void paramSup(@Odd String s);

  public abstract void receiver(@Odd MethodOverrides this);

  public abstract void receiverSub(@Odd MethodOverrides this);

  public static class SubclassA extends MethodOverrides {

    public @Odd String method() {
      // :: error: (assignment.type.incompatible)
      @Odd String s = "";
      return s;
    }

    public @Odd String methodSub() {
      // :: error: (assignment.type.incompatible)
      @Odd String s = "";
      return s;
    }

    public void param(@Odd String s) {}

    public void paramSup(String s) {}

    public void receiver(@Odd SubclassA this) {}

    public void receiverSub() {}
  }

  static class X {
    <T> T @Odd [] method(T @Odd [] t) {
      return null;
    }
  }

  static class Y extends X {
    @Override
    <S> S @Odd [] method(S @Odd [] s) {
      return null;
    }
  }

  static class Z extends X {
    @Override
    // return type is an incorrect override, as it's a supertype
    // :: error: (override.return.invalid)
    <A> A[] method(A[] s) {
      return null;
    }
  }

  static class Z2 extends X {
    @Override
    // :: error: (override.return.invalid) :: error: (override.param.invalid)
    <A> @Odd A[] method(@Odd A[] s) {
      return null;
    }
  }

  static class ClX<T> {
    T @Odd [] method(T @Odd [] t) {
      return null;
    }
  }

  static class ClY<S> extends ClX<S> {
    @Override
    S @Odd [] method(S @Odd [] s) {
      return null;
    }
  }

  static class ClZ<S> extends ClX<S> {
    @Override
    // :: error: (override.return.invalid) :: error: (override.param.invalid)
    @Odd S[] method(@Odd S[] s) {
      return null;
    }
  }

  // TODO others...
}
