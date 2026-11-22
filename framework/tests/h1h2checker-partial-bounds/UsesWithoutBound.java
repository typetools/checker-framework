import java.util.ArrayList;
import java.util.List;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;

// The default qualifier upper bound of a type has no qualifier in the H2 hierarchy, so a use of a
// type whose declaration has no H2 qualifier can have any H2 qualifier.
public class UsesWithoutBound {

  @H2S1 UsesWithoutBound field1;
  @H2Top UsesWithoutBound field2;
  @H2Bot UsesWithoutBound field3;

  @H2S1 UsesWithoutBound[] array1;
  @H2S2 UsesWithoutBound @H2S1 [] array2;
  @H2S1 int primitive1;
  @H2Top int primitive2;

  List<@H2S1 UsesWithoutBound> list = new ArrayList<@H2S1 UsesWithoutBound>();

  void uses(@H2S1 UsesWithoutBound p1, @H2Top UsesWithoutBound p2, @H2Bot String p3) {
    UsesWithoutBound local1 = p1;
    @H2Top UsesWithoutBound local2 = p2;
    // :: warning: [cast.unsafe.constructor.invocation]
    @H2S1 UsesWithoutBound local3 = new @H2S1 UsesWithoutBound();
    UsesWithoutBound local4 = new UsesWithoutBound();
    // :: error: [assignment]
    @H2S1 UsesWithoutBound local5 = p2;
    Object[] local6 = new Object[] {p1, p2};
    int @H2S1 [] local7 = new int @H2S1 [] {1, 2};
  }

  void casts(@H2S1 Object o, @H2Top Object top) {
    @H2S1 UsesWithoutBound c1 = (UsesWithoutBound) o;
    @H2S1 UsesWithoutBound c2 = (@H2S1 UsesWithoutBound) o;
    // :: warning: [cast.unsafe]
    @H2S1 UsesWithoutBound c3 = (@H2S1 UsesWithoutBound) top;
    // :: error: [assignment]
    @H2S1 UsesWithoutBound c4 = (UsesWithoutBound) top;
    @H2S1 int i = (@H2S1 int) primitive1;
  }

  void getClass(@H2S1 UsesWithoutBound p) {
    Class<?> c = p.getClass();
  }

  void anonymous() {
    // :: warning: [cast.unsafe.constructor.invocation]
    Object o = new @H2S1 Object() {};
    Runnable r =
        new Runnable() {
          @Override
          public void run() {}
        };
  }
}

class NoBoundSub extends UsesWithoutBound {}

// The class's bound in H2 is top, which is not a subtype of the qualifiers on its supertypes.
// :: error: [declaration.inconsistent.with.extends.clause] :: error:
// [declaration.inconsistent.with.implements.clause]
class NoBoundSub2 extends @H2S1 UsesWithoutBound implements @H2S2 Runnable {
  @Override
  public void run() {}
}

// The H1 bound is still enforced.
@SuppressWarnings({"inconsistent.constructor.type", "super.invocation"})
@H1S1 class H1Bounded {
  void uses(@H1S1 @H2S1 H1Bounded p1, @H1S1 @H2Top H1Bounded p2) {}

  // :: error: [annotations.on.use]
  void invalid(@H1Top H1Bounded p) {}
}

// An H2 bound that is written on a declaration is still enforced.
@SuppressWarnings({"inconsistent.constructor.type", "super.invocation"})
@H2S1 class H2Bounded {
  void uses(@H2S1 H2Bounded p1, @H2Bot H2Bounded p2) {}

  // :: error: [annotations.on.use]
  void invalid(@H2Top H2Bounded p) {}
}

enum NoBoundEnum {
  A,
  B;

  void uses(@H2S1 NoBoundEnum p) {
    NoBoundEnum e1 = A;
    @H2S1 NoBoundEnum e2 = p;
  }
}

interface NoBoundInterface {}

@SuppressWarnings({"inconsistent.constructor.type", "super.invocation"})
@H2S1 class ImplementsNoBound implements NoBoundInterface {}
