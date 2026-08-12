// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handle the same type qualifier appearing multiple times.

import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.EmptyTarget;
import java.lang.classfile.TypeAnnotation.FormalParameterTarget;
import java.lang.classfile.TypeAnnotation.SupertypeTarget;
import java.lang.classfile.TypeAnnotation.TargetInfo;
import java.lang.classfile.TypeAnnotation.ThrowsTarget;
import java.lang.classfile.TypeAnnotation.TypeParameterBoundTarget;
import java.lang.classfile.TypeAnnotation.TypeParameterTarget;
import java.util.ArrayList;
import java.util.List;

public class ReferenceInfoUtil {

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
      ClassModel cm, boolean ignoreConstructors) {
    ReferenceInfoUtil riu = new ReferenceInfoUtil(ignoreConstructors);
    List<TypeAnnotation> annos = new ArrayList<>();
    riu.findAnnotations(cm, annos);
    return annos;
  }

  // /////////////////// Extract type annotations //////////////////
  private void findAnnotations(ClassModel cm, List<TypeAnnotation> annos) {
    findAnnotations((AttributedElement) cm, annos);

    for (FieldModel f : cm.fields()) {
      findAnnotations(f, annos);
    }
    for (MethodModel m : cm.methods()) {
      if (ignoreConstructors && m.methodName().equalsString("<init>")) {
        continue;
      }

      findAnnotations(m, annos);
      m.code().ifPresent(code -> findAnnotations(code, annos));
    }
  }

  /**
   * Adds to {@code annos} the annotations in the element's {@code RuntimeVisibleTypeAnnotations}
   * and {@code RuntimeInvisibleTypeAnnotations} attributes.
   *
   * @param element a class, field, method, or code attribute
   * @param annos the list to which the type annotations are added
   */
  private static void findAnnotations(AttributedElement element, List<TypeAnnotation> annos) {
    element
        .findAttribute(Attributes.runtimeVisibleTypeAnnotations())
        .ifPresent(attr -> annos.addAll(attr.annotations()));
    element
        .findAttribute(Attributes.runtimeInvisibleTypeAnnotations())
        .ifPresent(attr -> annos.addAll(attr.annotations()));
  }

  // /////////////////////// Equality testing /////////////////////

  /**
   * Returns true if the given type annotation appears at the given position.
   *
   * @param expected the expected position
   * @param actual a type annotation read from a class file
   * @return true if {@code actual} appears at position {@code expected}
   */
  private static boolean isAtPosition(Position expected, TypeAnnotation actual) {
    TargetInfo target = actual.targetInfo();
    if (target.targetType() != expected.type || !expected.location.equals(actual.targetPath())) {
      return false;
    }
    return switch (target) {
      case TypeParameterTarget t -> expected.parameterIndex == t.typeParameterIndex();
      case TypeParameterBoundTarget t ->
          expected.parameterIndex == t.typeParameterIndex()
              && expected.boundIndex == t.boundIndex();
      case FormalParameterTarget t -> expected.parameterIndex == t.formalParameterIndex();
      case ThrowsTarget t -> expected.typeIndex == t.throwsTargetIndex();
      case SupertypeTarget t -> expected.typeIndex == t.supertypeIndex();
      case EmptyTarget unused -> true;
      // A target that Position cannot express, such as a local variable or a cast.
      default -> false;
    };
  }

  private static TypeAnnotation findAnnotation(
      String name, Position expected, List<TypeAnnotation> annotations) {
    String properName = "L" + name + ";";
    for (TypeAnnotation anno : annotations) {
      String actualName = anno.annotation().className().stringValue();

      if (properName.equals(actualName)) {
        System.out.println("For Anno: " + actualName);
      }

      if (properName.equals(actualName) && isAtPosition(expected, anno)) {
        return anno;
      }
    }
    return null;
  }

  public static boolean compare(
      List<AnnoPosPair> expectedAnnos, List<TypeAnnotation> actualAnnos, String diagnostic) {
    if (actualAnnos.size() != expectedAnnos.size()) {
      throw new ComparisonException(
          "Wrong number of annotations; " + diagnostic, expectedAnnos, actualAnnos);
    }

    for (AnnoPosPair e : expectedAnnos) {
      String aName = e.first;
      Position expected = e.second;
      TypeAnnotation actual = findAnnotation(aName, expected, actualAnnos);
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
