import lib.Factory;
import lib.SuperTypeArg;

/**
 * A method parameter or return type that is a classpath class whose supertype has a type argument
 * whose class file is absent. See MissingClassFile.java.
 */
public class Parameter {

  void parameter(SuperTypeArg s) {}

  SuperTypeArg returnType() {
    return Factory.make();
  }
}
