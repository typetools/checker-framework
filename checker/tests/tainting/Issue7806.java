import java.lang.invoke.MethodHandle;

public class Issue7806 {
  static void funcNoCrash(MethodHandle mhCreate, Long value) throws Throwable {
    mhCreate.invoke(value);
  }

  static void funcCrash(MethodHandle mhCreate, long value) throws Throwable {
    mhCreate.invoke(value);
  }
}
