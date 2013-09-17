import java.util.regex.*;

// @skip-test BUG, but disabled to avoid breaking the build

class TwoStaticInitBlocks {

  static {
    System.out.println("First static initializer block, but not the only one.");
  }

  public static final String ws_regexp;

  static {
    ws_regexp = "hello";
  }

}
