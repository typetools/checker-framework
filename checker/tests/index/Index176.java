// Test case for Issue 176:
// https://github.com/kelloggm/checker-framework/issues/176

import org.checkerframework.checker.index.qual.IndexFor;

public class Index176 {
  void test(String arglist, @IndexFor("#1") int pos) {
    int semi_pos = arglist.indexOf(";");
    if (semi_pos == -1) {
      throw new Error("Malformed arglist: " + arglist);
    }
    arglist.substring(pos, semi_pos + 1);
    // :: error: (argument)
    arglist.substring(pos, semi_pos + 2);
  }
}
