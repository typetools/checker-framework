import lib.SuperTypeArg;

/**
 * A field whose type is a classpath class whose supertype has a type argument whose class file is
 * absent. See Issue8055.java.
 */
public class Field {

  SuperTypeArg field;
}
