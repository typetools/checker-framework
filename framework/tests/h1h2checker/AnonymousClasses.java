import java.util.Comparator;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;

public class AnonymousClasses {
  private <@H1S1 T extends @H1S1 Comparator<T>> void testGenericAnonymous() {
    // :: error: (type.argument) :: error: (constructor.invocation)
    new @H1S1 Gen<T>() {};
    // :: error: (type.argument) :: warning: (cast.unsafe.constructor.invocation)
    new @H1S1 GenInter<T>() {};
  }
}

class Gen<@H1S2 F extends @H1S2 Object> {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  public @H1S2 Gen() {}
}

interface GenInter<@H1S2 F extends @H1S2 Object> {}

interface Foo {}
