import org.checkerframework.framework.testchecker.util.*;

public abstract class MethodOverrideBadParam {

  public abstract void method(String s);

  public static class SubclassA extends MethodOverrideBadParam {
    // :: error: (override.param.invalid)
    public void method(@Odd String s) {}
  }
}
