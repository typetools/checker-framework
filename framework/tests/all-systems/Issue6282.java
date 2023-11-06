import java.lang.invoke.MethodHandle;
import java.lang.reflect.AccessibleObject;

public class Issue6282 {
  public static final MethodHandle setAccessible0_Method = setAccessible0_Method();

  public static final MethodHandle setAccessible0_Method() {
    throw new RuntimeException();
  }

  public static void setAccessible(final AccessibleObject accessibleObject) {

    try {
      boolean newFlag = (boolean) setAccessible0_Method.invokeExact(accessibleObject, true);
      assert newFlag;
    } catch (Throwable throwable) {
      throw new AssertionError(throwable);
    }
  }
}
