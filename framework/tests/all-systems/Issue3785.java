import java.util.List;

public class Issue3785<T> {
  public Issue3785(List<T> l) {}

  <L> void method(L l) {}

  @SuppressWarnings("unchecked")
  void use(Issue3785 p, Object o, List<String> list) {
    // This type-checks in Java, but probably shouldn't.
    p.<Void>method(o);
    p.method(o);
    new Issue3785(list);
  }

  /*
  <L> L method2(L l) { return l;}

  @SuppressWarnings("unchecked")
  void use2(Issue3785<String> p, Object o) {
      // javac issues: "error: incompatible types: Object cannot be converted to Void"
      p.<Void>method(o);
      // javac issues: "error: incompatible types: Object cannot be converted to Void"
      Void s = p.<Void>method2(o);
  }
  */
}

/*
class Issue3785NonParameterized {
    <L> void method(L l) {}

    @SuppressWarnings("unchecked")
    void use(Issue3785NonParameterized p, Object o) {
        // javac issues: "error: incompatible types: Object cannot be converted to Void"
        p.<Void>method(o);
    }
}
*/
