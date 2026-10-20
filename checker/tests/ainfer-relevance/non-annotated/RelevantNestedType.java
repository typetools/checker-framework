import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// `Map.Entry` is a nested type and it has type parameters.  A program may refer to `Map.Entry` by
// the nested name "Map.Entry", by the simple name "Entry", or by the fully-qualified name
// "java.util.Map.Entry".  Inference must retain an annotation on a relevant type no matter how the
// program spells the type's name, and no matter whether the program writes type arguments.
public class RelevantNestedType {

  static Map.Entry<String, String> nestedName;
  static Entry<String, String> simpleName;
  static java.util.Map.Entry<String, String> qualifiedName;
  static Map.Entry rawName;

  // AbstractMap.SimpleEntry is relevant because it is a subtype of Map.Entry, even though the name
  // "AbstractMap.SimpleEntry" does not appear in the checker's `@RelevantJavaTypes`.
  static AbstractMap.SimpleEntry<String, String> subtype;

  static void assignFields() {
    nestedName = getSibling1();
    simpleName = getSibling1();
    qualifiedName = getSibling1();
    rawName = getSibling1();
    subtype = getSibling1Subtype();
  }

  static void useFields() {
    // :: warning: [argument]
    expectsSibling1(nestedName);
    // :: warning: [argument]
    expectsSibling1(simpleName);
    // :: warning: [argument]
    expectsSibling1(qualifiedName);
    // :: warning: [argument]
    expectsSibling1Subtype(subtype);
  }

  static void expectsSibling1(Map.@AinferSibling1 Entry<String, String> e) {}

  static void expectsSibling1Subtype(AbstractMap.@AinferSibling1 SimpleEntry<String, String> e) {}

  static Map.@AinferSibling1 Entry<String, String> getSibling1() {
    return null;
  }

  static AbstractMap.@AinferSibling1 SimpleEntry<String, String> getSibling1Subtype() {
    return null;
  }
}
