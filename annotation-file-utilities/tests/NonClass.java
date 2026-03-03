public class NonClass {
  interface I extends Comparable<I> {}

  interface A {
    void m(Object p);
  }

  enum B {
    ONE;

    void m() {
      Object l;
    }
  }

  @interface C {
    String value() default "Ha!";
  }

  enum E {
    D((String) new String());

    E(String s) {
      a =
          new A() {
            @Override
            public void m(Object p) {}
          };
    }

    final A a;
  }
}
