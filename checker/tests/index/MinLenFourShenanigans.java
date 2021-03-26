package index;

public class MinLenFourShenanigans {
  public static boolean isInterned(Object value) {
    if (value == null) {
      // nothing to do
      return true;
    } else if (value instanceof String) {
      // Used to issue the below error.
      // MinLenFourShenanigans.java:7: warning: [cast.unsafe] "@MinLen(0) Object" may not be
      // casted to the type "@MinLen(4) String"
      return (value == ((String) value).intern());
    }
    return false;
  }

  public static boolean isInterned2(Object value) {
    if (value instanceof String) {
      return (value == ((String) value).intern());
    }
    return false;
  }
}
