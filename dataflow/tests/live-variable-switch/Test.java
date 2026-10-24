// A switch selector is a use of the variables in it.
public class Test {
  public int test(String t) {
    String s = t;
    switch (s) {
      case "a":
        return 1;
      default:
        return 2;
    }
  }
}
