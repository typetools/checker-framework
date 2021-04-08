import org.checkerframework.framework.testchecker.util.*;

public abstract class MethodOverrideBadReturn {

  public abstract @Odd String method();

  public static class SubclassA extends MethodOverrideBadReturn {
    // :: error: (override.return.invalid)
    public String method() {
      return "";
    }
  }
}
