import lib.Factory;
import lib.SuperTypeArg;

/**
 * Naming a classpath class whose supertype has a type argument whose class file is absent, in an
 * expression that dataflow analyzes. See Issue8055.java.
 */
public class ClassLiteral {

  // The Beam shape: a class literal in a field initializer.
  static final Object KNOWN = SuperTypeArg.class;

  // A class literal in a method body fails the same way.
  Object classLiteralInBody() {
    return SuperTypeArg.class;
  }

  // So does an object creation expression.
  Object objectCreation() {
    return new SuperTypeArg();
  }

  // So does a local variable whose initializer has that type.
  void localVariable() {
    SuperTypeArg s = Factory.make();
  }
}
