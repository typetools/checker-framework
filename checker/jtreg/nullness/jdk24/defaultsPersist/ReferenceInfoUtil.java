// Keep in sync with ../../jdk25/defaultsPersist/ReferenceInfoUtil.java, which uses the
// java.lang.classfile API in place of the com.sun.tools.classfile API that was removed in Java 25.

// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handle the same type qualifier appearing multiple times.

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPool.UnexpectedEntry;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.RuntimeTypeAnnotations_attribute;
import com.sun.tools.classfile.TypeAnnotation;
import com.sun.tools.classfile.TypeAnnotation.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReferenceInfoUtil {

  public static final int IGNORE_VALUE = -321;

  /** If true, don't collect annotations on constructors. */
  boolean ignoreConstructors;

  /**
   * Creates a new ReferenceInfoUtil.
   *
   * @param ignoreConstructors if true, don't collect annotations on constructor
   */
  public ReferenceInfoUtil(boolean ignoreConstructors) {
    this.ignoreConstructors = ignoreConstructors;
  }

  public static List<TypeAnnotation> extendedAnnotationsOf(
      ClassFile c, boolean ignoreConstructors) {
    ReferenceInfoUtil riu = new ReferenceInfoUtil(ignoreConstructors);
    List<TypeAnnotation> annos = new ArrayList<>();
    riu.findAnnotations(c, annos);
    return annos;
  }

  // /////////////////// Extract type annotations //////////////////
  private void findAnnotations(ClassFile c, List<TypeAnnotation> annos) {
    findAnnotations(c, Attribute.RuntimeVisibleTypeAnnotations, annos);
    findAnnotations(c, Attribute.RuntimeInvisibleTypeAnnotations, annos);

    for (Field f : c.fields) {
      findAnnotations(c, f, annos);
    }
    for (Method m : c.methods) {
      String methodName;
      try {
        methodName = m.getName(c.constant_pool);
      } catch (Exception e) {
        throw new Error(e);
      }
      if (ignoreConstructors && methodName.equals("<init>")) {
        continue;
      }

      findAnnotations(c, m, annos);
    }
  }

  private static void findAnnotations(ClassFile c, Method m, List<TypeAnnotation> annos) {
    findAnnotations(c, m, Attribute.RuntimeVisibleTypeAnnotations, annos);
    findAnnotations(c, m, Attribute.RuntimeInvisibleTypeAnnotations, annos);
  }

  private static void findAnnotations(ClassFile c, Field m, List<TypeAnnotation> annos) {
    findAnnotations(c, m, Attribute.RuntimeVisibleTypeAnnotations, annos);
    findAnnotations(c, m, Attribute.RuntimeInvisibleTypeAnnotations, annos);
  }

  /**
   * Test the result of Attributes.getIndex according to expectations encoded in the method's name.
   */
  private static void findAnnotations(ClassFile c, String name, List<TypeAnnotation> annos) {
    int index = c.attributes.getIndex(c.constant_pool, name);
    if (index != -1) {
      Attribute attr = c.attributes.get(index);
      assert attr instanceof RuntimeTypeAnnotations_attribute;
      RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute) attr;
      annos.addAll(Arrays.asList(tAttr.annotations));
    }
  }

  /**
   * Test the result of Attributes.getIndex according to expectations encoded in the method's name.
   */
  private static void findAnnotations(
      ClassFile c, Method m, String name, List<TypeAnnotation> annos) {
    int index = m.attributes.getIndex(c.constant_pool, name);
    if (index != -1) {
      Attribute attr = m.attributes.get(index);
      assert attr instanceof RuntimeTypeAnnotations_attribute;
      RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute) attr;
      annos.addAll(Arrays.asList(tAttr.annotations));
    }

    int cindex = m.attributes.getIndex(c.constant_pool, Attribute.Code);
    if (cindex != -1) {
      Attribute cattr = m.attributes.get(cindex);
      assert cattr instanceof Code_attribute;
      Code_attribute cAttr = (Code_attribute) cattr;
      index = cAttr.attributes.getIndex(c.constant_pool, name);
      if (index != -1) {
        Attribute attr = cAttr.attributes.get(index);
        assert attr instanceof RuntimeTypeAnnotations_attribute;
        RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute) attr;
        annos.addAll(Arrays.asList(tAttr.annotations));
      }
    }
  }

  /**
   * Test the result of Attributes.getIndex according to expectations encoded in the method's name.
   */
  private static void findAnnotations(
      ClassFile c, Field m, String name, List<TypeAnnotation> annos) {
    int index = m.attributes.getIndex(c.constant_pool, name);
    if (index != -1) {
      Attribute attr = m.attributes.get(index);
      assert attr instanceof RuntimeTypeAnnotations_attribute;
      RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute) attr;
      annos.addAll(Arrays.asList(tAttr.annotations));
    }
  }

  // /////////////////////// Equality testing /////////////////////
  private static boolean areEquals(int a, int b) {
    return a == b || a == IGNORE_VALUE || b == IGNORE_VALUE;
  }

  private static boolean areEquals(int[] a, int[] a2) {
    if (a == a2) {
      return true;
    }
    if (a == null || a2 == null) {
      return false;
    }

    int length = a.length;
    if (a2.length != length) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      if (!areEquals(a[i], a2[i])) {
        return false;
      }
    }

    return true;
  }

  public static boolean areEquals(Position p1, Position p2) {
    if (p1 == p2) {
      return true;
    }
    if (p1 == null || p2 == null) {
      return false;
    }

    boolean result =
        ((p1.type == p2.type)
            && (p1.location.equals(p2.location))
            && areEquals(p1.offset, p2.offset)
            && areEquals(p1.lvarOffset, p2.lvarOffset)
            && areEquals(p1.lvarLength, p2.lvarLength)
            && areEquals(p1.lvarIndex, p2.lvarIndex)
            && areEquals(p1.bound_index, p2.bound_index)
            && areEquals(p1.parameter_index, p2.parameter_index)
            && areEquals(p1.type_index, p2.type_index)
            && areEquals(p1.exception_index, p2.exception_index));
    return result;
  }

  public static String positionCompareStr(Position p1, Position p2) {
    return String.join(
        System.lineSeparator(),
        "type = " + p1.type + ", " + p2.type,
        "offset = " + p1.offset + ", " + p2.offset,
        "lvarOffset = " + Arrays.toString(p1.lvarOffset) + ", " + Arrays.toString(p2.lvarOffset),
        "lvarLength = " + Arrays.toString(p1.lvarLength) + ", " + Arrays.toString(p2.lvarLength),
        "lvarIndex = " + Arrays.toString(p1.lvarIndex) + ", " + Arrays.toString(p2.lvarIndex),
        "bound_index = " + p1.bound_index + ", " + p2.bound_index,
        "parameter_index = " + p1.parameter_index + ", " + p2.parameter_index,
        "type_index = " + p1.type_index + ", " + p2.type_index,
        "exception_index = " + p1.exception_index + ", " + p2.exception_index,
        "");
  }

  private static TypeAnnotation findAnnotation(
      String name, Position expected, List<TypeAnnotation> annotations, ClassFile c)
      throws InvalidIndex, UnexpectedEntry {
    String properName = "L" + name + ";";
    for (TypeAnnotation anno : annotations) {
      String actualName = c.constant_pool.getUTF8Value(anno.annotation.type_index);
      if (properName.equals(actualName) && areEquals(expected, anno.position)) {
        return anno;
      }
    }
    return null;
  }

  public static boolean compare(
      List<AnnoPosPair> expectedAnnos,
      List<TypeAnnotation> actualAnnos,
      ClassFile c,
      String diagnostic)
      throws InvalidIndex, UnexpectedEntry {
    if (actualAnnos.size() != expectedAnnos.size()) {
      throw new ComparisonException(
          "Wrong number of annotations in " + c + "; " + diagnostic, expectedAnnos, actualAnnos);
    }

    // Each expected annotation must be matched by a different actual annotation.
    List<TypeAnnotation> unmatched = new ArrayList<>(actualAnnos);
    for (AnnoPosPair e : expectedAnnos) {
      String aName = e.first;
      Position expected = e.second;
      TypeAnnotation actual = findAnnotation(aName, expected, unmatched, c);
      if (actual == null) {
        throw new ComparisonException(
            "Expected annotation not found: "
                + aName
                + " position: "
                + expected
                + "; "
                + diagnostic,
            expectedAnnos,
            actualAnnos);
      }
      unmatched.remove(actual);
    }
    return true;
  }
}

class ComparisonException extends RuntimeException {
  private static final long serialVersionUID = -3930499712333815821L;

  public final List<AnnoPosPair> expected;
  public final List<TypeAnnotation> found;

  public ComparisonException(
      String message, List<AnnoPosPair> expected, List<TypeAnnotation> found) {
    super(message);
    this.expected = expected;
    this.found = found;
  }

  public String toString() {
    return String.format(
        "%s%n  Expected (%d): %s%n  Found (%d): %s",
        super.toString(), expected.size(), expected, found.size(), found);
  }
}
