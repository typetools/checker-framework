// A switch selector is a use of the variables in it.
public class Test {
  public int test(String t, boolean b) {
    String s = t;
    switch (s) {
      case "a":
        return 1;
      default:
        break;
    }
    // A switch whose selector is never null has no null check.
    switch (b ? "a" : "b") {
      case "a":
        return 2;
      default:
        return 3;
    }
  }
}
