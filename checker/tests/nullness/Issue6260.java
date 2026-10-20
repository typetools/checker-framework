public class Issue6260 {
  enum MyE {
    FOO;

    MyE getIt() {
      return FOO;
    }

    String go() {
      MyE e = getIt();
      switch (e) {
        case FOO:
          return "foo";
      }
      throw new AssertionError(e);
    }
  }
}
