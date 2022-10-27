// Test case based on a crash encountered when running WPI on plume-util's Intern.java.

public final class InternCrash {

  public static String [] intern(String [] a) {
    return a;
  }

  public static Object intern(Object a) {
    if (a instanceof String[]) {
      // Crash caused by this line.
      String[] asArray = (String[]) a;
      return intern(asArray);
    } else {
      return null;
    }
  }
}
