public class UseStub {
  int test() {
    // The stub file makes the return type of System.getProperty non-null.
    return System.getProperty("user.dir").length();
  }
}
