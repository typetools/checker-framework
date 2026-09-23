import java.util.*;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A type in the compilation unit's own package shadows a type that is imported on demand and a
// type in `java.lang`.  This compilation unit is in the unnamed package, whose members are the
// top-level types of every compilation unit that has no package declaration.  Inference must
// resolve the name "Runnable" to the top-level class `Runnable` rather than to
// `java.lang.Runnable`, and the name "List" to the top-level class `List` rather than to
// `java.util.List`.  Both top-level classes are relevant because they are subtypes of
// `CharSequence`, whereas `java.lang.Runnable` and `java.util.List` are irrelevant.  If inference
// resolves either name incorrectly and therefore discards the annotation, then the second
// (validation) pass of this test issues the warning that is written below.
public class SamePackageShadows {

  static Runnable runnableField;
  static List listField;

  static void assignFields() {
    runnableField = getSibling1Runnable();
    listField = getSibling1List();
  }

  static void useFields() {
    // :: warning: [argument]
    expectsSibling1Runnable(runnableField);
    // :: warning: [argument]
    expectsSibling1List(listField);
  }

  static void expectsSibling1Runnable(@AinferSibling1 Runnable r) {}

  static void expectsSibling1List(@AinferSibling1 List l) {}

  static @AinferSibling1 Runnable getSibling1Runnable() {
    return null;
  }

  static @AinferSibling1 List getSibling1List() {
    return null;
  }
}
