package daikon.inv;
import daikon.*;
import utilMDE.Pair;
import java.util.*;

/**
 * Enumeration type for output style.
 * (Should this be somewhere else?)
 **/
public enum OutputFormat {

  /** The standard, concise Daikon output format */
  DAIKON("Daikon"),
  /** Design-By-Contract for Java (used by Parasoft JContract) */
  DBCJAVA("DBC") {
    public String ensures_tag() { return "@post"; }
    public String requires_tag() { return "@pre"; }
  },
  /** ESC/Java's annotation language */
  ESCJAVA("ESC/Java"),
  /** IOA language */
  IOA("IOA"),
  /** IOA language, sans invariant numbering */
  IOATEST("IOA_test"),
  /** Java boolean expression */
  JAVA("Java"),
  /** Java Modeling Language */
  JML("JML"),
  /** Simplify theorem prover */
  SIMPLIFY("Simplify");

  String name;

  OutputFormat(String name) { this.name = name; }

  public String toString() { return "OutputFormat:" + name; }

  public boolean isJavaFamily() {
    return (this == DBCJAVA || this == JML || this == JAVA);
  }

  // An alternative to valueOf(); the advantage is that it can be
  // case-sensitive, can permit alternative names, etc.  An enum cannot
  // override valueOf().
  /**
   * Return the appropriate OutputFormat for the given name, or null
   * if no such OutputFormat exists.
   **/
  public static OutputFormat get(String name) {
    if (name == null) { return null; }
    if (name.compareToIgnoreCase(DAIKON.name) == 0) { return DAIKON; }
    if (name.compareToIgnoreCase(DBCJAVA.name) == 0) { return DBCJAVA; }
    if (name.compareToIgnoreCase(ESCJAVA.name) == 0) { return ESCJAVA; }
    if (name.compareToIgnoreCase("ESC") == 0) { return ESCJAVA; }
    if (name.compareToIgnoreCase(IOA.name) == 0) { return IOA; }
    if (name.compareToIgnoreCase(IOATEST.name) == 0) { return IOATEST; }
    if (name.compareToIgnoreCase(JAVA.name) == 0) { return JAVA; }
    if (name.compareToIgnoreCase(JML.name) == 0) { return JML; }
    if (name.compareToIgnoreCase(SIMPLIFY.name) == 0) { return SIMPLIFY; }
    return null;
  }

  public String ensures_tag() { return "ensures"; }
  public String requires_tag() { return "requires"; }

}
