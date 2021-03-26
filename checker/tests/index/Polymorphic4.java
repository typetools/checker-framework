import org.checkerframework.common.value.qual.PolyValue;

// @skip-test until #153 is resolved.

public class Polymorphic4 {

  public static String @PolyValue [] quantify(String @PolyValue [] vars) {

    String[] result = new String[vars.length];

    return result;
  }
}
