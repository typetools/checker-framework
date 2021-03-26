// :: error: (type.checking.not.run)
public class ResolveError {
  void m() {
    // :: error: cannot find symbol
    Unresolved.foo();
  }
}
