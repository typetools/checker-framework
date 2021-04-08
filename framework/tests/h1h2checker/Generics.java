import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Generics {

  class Generics1<T extends @H1Top @H2Top Object> {

    T m(@H1S2 @H2S2 T p) {
      T l = p;
      // :: error: (return.type.incompatible)
      return l;
    }

    void unsound(Generics1<@H1S1 @H2S1 Object> p, @H1S2 @H2S2 Object p2) {
      @H1S1 @H2S1 Object o = p.m(p2);
    }
  }

  class Generics2<T extends @H1Top Object> {

    T m(@H1S2 T p) {
      T l = p;
      // :: error: (return.type.incompatible)
      return l;
    }

    void unsound(Generics2<@H1S1 Object> p, @H1S2 Object p2) {
      @H1S1 Object o = p.m(p2);
    }
  }

  class Generics3<T extends @H1S1 Object> {

    // See comments in BaseTypeVisitor about type variable checks.
    // The currently desired behavior is that the annotation on the
    // type variable overrides the bound.
    // TODO?:: error: (type.invalid)
    void m(@H1S2 T p) {}
  }
}
