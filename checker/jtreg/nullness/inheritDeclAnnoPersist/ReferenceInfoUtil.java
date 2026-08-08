// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handle the same type qualifier appearing multiple times.

import java.lang.classfile.Annotation;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ReferenceInfoUtil {

  public static List<Annotation> extendedAnnotationsOf(ClassModel cm) {
    List<Annotation> annos = new ArrayList<>();
    findAnnotations(cm, annos);
    return annos;
  }

  // /////////////////// Extract annotations //////////////////
  private static void findAnnotations(ClassModel cm, List<Annotation> annos) {
    for (MethodModel m : cm.methods()) {
      findAnnotations(m, annos);
    }
  }

  /**
   * Adds to {@code annos} the annotations in the method's {@code RuntimeVisibleAnnotations}
   * attribute, skipping those whose name is already in {@code annos}.
   *
   * @param m a method
   * @param annos the list to which the annotations are added
   */
  private static void findAnnotations(MethodModel m, List<Annotation> annos) {
    m.findAttribute(Attributes.runtimeVisibleAnnotations())
        .ifPresent(
            attr -> {
              for (Annotation an : attr.annotations()) {
                if (!containsName(annos, an)) {
                  annos.add(an);
                }
              }
            });
  }

  private static Annotation findAnnotation(String name, List<Annotation> annotations) {
    String properName = "L" + name + ";";
    for (Annotation anno : annotations) {
      if (anno.className().equalsString(properName)) {
        return anno;
      }
    }
    return null;
  }

  public static boolean compare(
      List<String> expectedAnnos, List<Annotation> actualAnnos, String diagnostic) {
    if (actualAnnos.size() != expectedAnnos.size()) {
      throw new ComparisonException(
          "Wrong number of annotations; " + diagnostic, expectedAnnos, actualAnnos);
    }
    for (String annoName : expectedAnnos) {
      Annotation anno = findAnnotation(annoName, actualAnnos);
      if (anno == null) {
        throw new ComparisonException(
            "Expected annotation not found: " + annoName + "; " + diagnostic,
            expectedAnnos,
            actualAnnos);
      }
    }
    return true;
  }

  private static boolean containsName(List<Annotation> annos, Annotation anno) {
    for (Annotation an : annos) {
      if (an.className().equalsString(anno.className().stringValue())) {
        return true;
      }
    }
    return false;
  }
}

class ComparisonException extends RuntimeException {
  private static final long serialVersionUID = -3930499712333815821L;

  public final List<String> expected;
  public final List<Annotation> found;

  public ComparisonException(String message, List<String> expected, List<Annotation> found) {
    super(message);
    this.expected = expected;
    this.found = found;
  }

  public String toString() {
    StringJoiner foundString = new StringJoiner(",");
    for (Annotation anno : found) {
      foundString.add(anno.className().stringValue());
    }
    return String.join(
        System.lineSeparator(),
        super.toString(),
        "\tExpected: "
            + expected.size()
            + " annotations; but found: "
            + found.size()
            + " annotations",
        "  Expected: " + expected,
        "  Found: " + foundString);
  }
}
