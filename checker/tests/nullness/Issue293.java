// Test for Issue 293:
// https://code.google.com/p/checker-framework/issues/detail?id=293
// @skip-test
class Issue293 {
  void foobar() {
    String s;
    try {
      s = read();
    } catch(Exception e) {
      // Because of definite assignment, s cannot be mentioned here.
      write("Catch.");
      return;
    } finally {
      // Because of definite assignment, s cannot be mentioned here.
      write("Finally.");
    }

    // s is definitely initialized here.
    write(s);
  }

  String read() throws Exception {
    throw new Exception();
  }

  void write(String p) {
    System.out.println(p);
  }
}
