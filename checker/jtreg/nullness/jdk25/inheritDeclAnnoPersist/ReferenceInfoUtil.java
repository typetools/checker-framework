// Keep in sync with ../../jdk24/inheritDeclAnnoPersist/ReferenceInfoUtil.java, which uses the
// com.sun.tools.classfile API that was removed in Java 25.  This version uses the
// java.lang.classfile API.

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

  public static final int IGNORE_VALUE = -321;

  public static List<Annotation> extendedAnnotationsOf(ClassModel c) {
    List<Annotation> annos = new ArrayList<>();
    findAnnotations(c, annos);
    return annos;
  }

  // /////////////////// Extract annotations //////////////////
  private static void findAnnotations(ClassModel c, List<Annotation> annos) {
    for (MethodModel m : c.methods()) {
      findAnnotations(m, annos);
    }
  }

  /** Adds to {@code annos} the runtime-visible declaration annotations on {@code m}. */
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
      String actualName = anno.className().stringValue();
      if (properName.equals(actualName)) {
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
    // Each expected annotation must be matched by a different actual annotation.
    List<Annotation> unmatched = new ArrayList<>(actualAnnos);
    for (String annoName : expectedAnnos) {
      Annotation anno = findAnnotation(annoName, unmatched);
      if (anno == null) {
        throw new ComparisonException(
            "Expected annotation not found: " + annoName + "; " + diagnostic,
            expectedAnnos,
            actualAnnos);
      }
      unmatched.remove(anno);
    }
    return true;
  }

  private static boolean containsName(List<Annotation> annos, Annotation anno) {
    for (Annotation an : annos) {
      if (an.className().stringValue().equals(anno.className().stringValue())) {
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
