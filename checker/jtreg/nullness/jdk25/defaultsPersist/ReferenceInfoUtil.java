// Keep in sync with ../../jdk24/defaultsPersist/ReferenceInfoUtil.java, which uses the
// com.sun.tools.classfile API that was removed in Java 25.  This version uses the
// java.lang.classfile API.

// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handle the same type qualifier appearing multiple times.

import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.CodeAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;

public class ReferenceInfoUtil {

  public static final int IGNORE_VALUE = -321;

  /**
   * Maps a label to a bytecode index. It is used for type annotations that are not within a method
   * body, where no label can occur.
   */
  private static final ToIntFunction<Label> NO_LABELS =
      label -> {
        throw new AssertionError("unexpected label outside a method body: " + label);
      };

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

  public static List<AnnoPosPair> extendedAnnotationsOf(ClassModel c, boolean ignoreConstructors) {
    ReferenceInfoUtil riu = new ReferenceInfoUtil(ignoreConstructors);
    List<AnnoPosPair> annos = new ArrayList<>();
    riu.findAnnotations(c, annos);
    return annos;
  }

  // /////////////////// Extract type annotations //////////////////
  private void findAnnotations(ClassModel c, List<AnnoPosPair> annos) {
    findAnnotations(c, NO_LABELS, annos);

    for (FieldModel f : c.fields()) {
      findAnnotations(f, NO_LABELS, annos);
    }
    for (MethodModel m : c.methods()) {
      if (ignoreConstructors && m.methodName().equalsString("<init>")) {
        continue;
      }

      findAnnotations(m, NO_LABELS, annos);
      CodeAttribute code = m.findAttribute(Attributes.code()).orElse(null);
      if (code != null) {
        findAnnotations(code, code::labelToBci, annos);
      }
    }
  }

  /**
   * Adds to {@code annos} the visible and invisible type annotations on {@code element}.
   *
   * @param element the class, field, method, or code attribute to search
   * @param labelToBci maps a label to a bytecode index
   * @param annos the list to add to
   */
  private static void findAnnotations(
      AttributedElement element, ToIntFunction<Label> labelToBci, List<AnnoPosPair> annos) {
    element
        .findAttribute(Attributes.runtimeVisibleTypeAnnotations())
        .ifPresent(a -> addAll(a.annotations(), labelToBci, annos));
    element
        .findAttribute(Attributes.runtimeInvisibleTypeAnnotations())
        .ifPresent(a -> addAll(a.annotations(), labelToBci, annos));
  }

  /**
   * Adds to {@code annos} an {@link AnnoPosPair} for each of {@code typeAnnos}.
   *
   * @param typeAnnos the type annotations to add
   * @param labelToBci maps a label to a bytecode index
   * @param annos the list to add to
   */
  private static void addAll(
      List<TypeAnnotation> typeAnnos, ToIntFunction<Label> labelToBci, List<AnnoPosPair> annos) {
    for (TypeAnnotation ta : typeAnnos) {
      annos.add(
          AnnoPosPair.of(ta.annotation().className().stringValue(), Position.of(ta, labelToBci)));
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
            && areEquals(p1.boundIndex, p2.boundIndex)
            && areEquals(p1.parameterIndex, p2.parameterIndex)
            && areEquals(p1.typeIndex, p2.typeIndex)
            && areEquals(p1.exceptionIndex, p2.exceptionIndex));
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
        "boundIndex = " + p1.boundIndex + ", " + p2.boundIndex,
        "parameterIndex = " + p1.parameterIndex + ", " + p2.parameterIndex,
        "typeIndex = " + p1.typeIndex + ", " + p2.typeIndex,
        "exceptionIndex = " + p1.exceptionIndex + ", " + p2.exceptionIndex,
        "");
  }

  private static AnnoPosPair findAnnotation(
      String descriptor, Position expected, List<AnnoPosPair> annotations) {
    for (AnnoPosPair anno : annotations) {
      String actualName = anno.first;
      if (descriptor.equals(actualName) && areEquals(expected, anno.second)) {
        return anno;
      }
    }
    return null;
  }

  public static boolean compare(
      List<AnnoPosPair> expectedAnnos,
      List<AnnoPosPair> actualAnnos,
      ClassModel c,
      String diagnostic) {
    if (actualAnnos.size() != expectedAnnos.size()) {
      throw new ComparisonException(
          "Wrong number of annotations in " + c.thisClass().asInternalName() + "; " + diagnostic,
          expectedAnnos,
          actualAnnos);
    }

    // Each expected annotation must be matched by a different actual annotation.
    List<AnnoPosPair> unmatched = new ArrayList<>(actualAnnos);
    for (AnnoPosPair e : expectedAnnos) {
      String aName = e.first;
      Position expected = e.second;
      AnnoPosPair actual = findAnnotation(aName, expected, unmatched);
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
  public final List<AnnoPosPair> found;

  public ComparisonException(String message, List<AnnoPosPair> expected, List<AnnoPosPair> found) {
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
