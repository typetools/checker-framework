public class Issue6743 {
  void foo(boolean b) {
    Object o = b ? null : null;
  }
}
