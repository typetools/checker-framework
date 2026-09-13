// Keep in sync with ../../jdk25/inheritDeclAnnoPersist/ReferenceInfoUtil.java, which uses the
// java.lang.classfile API in place of the com.sun.tools.classfile API that was removed in Java 25.

// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handle the same type qualifier appearing multiple times.

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPool.UnexpectedEntry;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.RuntimeAnnotations_attribute;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ReferenceInfoUtil {

  public static final int IGNORE_VALUE = -321;

  public static List<Annotation> extendedAnnotationsOf(ClassFile c) {
    List<Annotation> annos = new ArrayList<>();
    findAnnotations(c, annos);
    return annos;
  }

  // /////////////////// Extract annotations //////////////////
  private static void findAnnotations(ClassFile c, List<Annotation> annos) {
    for (Method m : c.methods) {
      findAnnotations(c, m, Attribute.RuntimeVisibleAnnotations, annos);
    }
  }

  /**
   * Adds to {@code annos} the declaration annotations in the {@code name} attribute of {@code m}.
   */
  private static void findAnnotations(ClassFile c, Method m, String name, List<Annotation> annos) {
    int index = m.attributes.getIndex(c.constant_pool, name);
    if (index != -1) {
      Attribute attr = m.attributes.get(index);
      assert attr instanceof RuntimeAnnotations_attribute;
      RuntimeAnnotations_attribute tAttr = (RuntimeAnnotations_attribute) attr;
      for (Annotation an : tAttr.annotations) {
        if (!containsName(annos, an, c)) {
          annos.add(an);
        }
      }
    }
  }

  private static Annotation findAnnotation(String name, List<Annotation> annotations, ClassFile c)
      throws InvalidIndex, UnexpectedEntry {
    String properName = "L" + name + ";";
    for (Annotation anno : annotations) {
      String actualName = c.constant_pool.getUTF8Value(anno.type_index);
      if (properName.equals(actualName)) {
        return anno;
      }
    }
    return null;
  }

  public static boolean compare(
      List<String> expectedAnnos, List<Annotation> actualAnnos, ClassFile c, String diagnostic)
      throws InvalidIndex, UnexpectedEntry {
    if (actualAnnos.size() != expectedAnnos.size()) {
      throw new ComparisonException(
          "Wrong number of annotations; " + diagnostic, expectedAnnos, actualAnnos, c);
    }
    // Each expected annotation must be matched by a different actual annotation.
    List<Annotation> unmatched = new ArrayList<>(actualAnnos);
    for (String annoName : expectedAnnos) {
      Annotation anno = findAnnotation(annoName, unmatched, c);
      if (anno == null) {
        throw new ComparisonException(
            "Expected annotation not found: " + annoName + "; " + diagnostic,
            expectedAnnos,
            actualAnnos,
            c);
      }
      unmatched.remove(anno);
    }
    return true;
  }

  private static boolean containsName(List<Annotation> annos, Annotation anno, ClassFile c) {
    try {
      for (Annotation an : annos) {
        if (c.constant_pool
            .getUTF8Value(an.type_index)
            .equals(c.constant_pool.getUTF8Value(anno.type_index))) {
          return true;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}

class ComparisonException extends RuntimeException {
  private static final long serialVersionUID = -3930499712333815821L;

  public final List<String> expected;
  public final List<Annotation> found;
  public final ClassFile c;

  public ComparisonException(
      String message, List<String> expected, List<Annotation> found, ClassFile c) {
    super(message);
    this.expected = expected;
    this.found = found;
    this.c = c;
  }

  public String toString() {
    StringJoiner foundString = new StringJoiner(",");
    for (Annotation anno : found) {
      try {
        foundString.add(c.constant_pool.getUTF8Value(anno.type_index));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
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
