import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class QualifierBoundDirection {

  static <@H1S1 A> void take(A a1, A a2) {}

  static <B extends @H1S2 Object> B make(B b) {
    return b;
  }

  void test(@H1S2 String s, @H1S2 String t) {
    take(make(s), t);
  }
}
