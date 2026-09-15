/**
 * {@code Sub}'s superclass is an inner class of an unrelated generic class, {@code Issue8168Gen}.
 * Calling an inherited method whose signature mentions {@code Issue8168Gen}'s type variable crashed
 * the Checker Framework with "Enclosing type not found", because {@code Issue8168Gen} is not an
 * enclosing type of {@code Sub}.
 */
public class Issue8168 {
  Issue8168Gen<String> gen = new Issue8168Gen<>();

  abstract class Sub extends Issue8168Gen<String>.Inner {
    Sub() {
      gen.super();
    }

    void call(String s) {
      use(s);
      String unused = getArg();
    }
  }

  /** The instantiation of {@code Issue8168Gen} is two supertypes away from {@code SubSub}. */
  abstract class SubSub extends Sub {
    void callFromSubSub(String s) {
      use(s);
      String unused = getArg();
    }
  }
}

/**
 * {@code EnclosedSub} is lexically enclosed by a parameterization of {@code Issue8168Gen} and also
 * extends an inner class of a different parameterization of it. Per JLS 4.5.2, the type of an
 * inherited member comes from the superclass type, so {@code T} is {@code String}, not {@code
 * Integer}.
 */
class Issue8168Outer extends Issue8168Gen<Integer> {
  abstract class EnclosedSub extends Issue8168Gen<String>.Inner {
    EnclosedSub(Issue8168Gen<String> outer) {
      outer.super();
    }

    void call(String s) {
      use(s);
      String unused = getArg();
    }
  }
}

class Issue8168Gen<T> {
  abstract class Inner {
    abstract void use(T arg);

    abstract T getArg();
  }
}
