// Testcase for Issue 672
// https://github.com/typetools/checker-framework/issues/672

final class Issue672 extends Throwable {
  final Throwable ex;

  Issue672(Throwable x) {
    ex = x;
  }

  static Issue672 test1(Throwable x, boolean flag) {
    return new Issue672(x instanceof Exception ? x : ((flag ? x : new Issue672(x))));
  }

  static Issue672 test2(Throwable x, boolean flag) {
    return (new Issue672(x instanceof Exception ? x : ((flag ? x : new Issue672(x)))));
  }

  static Issue672 test3(Throwable x) {
    return test1(x instanceof Exception ? x : new Issue672(x), false);
  }
}
