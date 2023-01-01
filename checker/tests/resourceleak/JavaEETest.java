// Test for https://github.com/typetools/checker-framework/issues/5472

class JavaEETest {
  static void foo(javax.servlet.ServletResponse s) throws java.io.IOException {
    s.getWriter();
  }
}
