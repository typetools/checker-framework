package org.checkerframework.framework.type;

import java.lang.annotation.Annotation;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.qual.UpperBoundFor;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/** Class that computes and stores the qualifier upper bounds for type uses. */
public class QualifierUpperBounds {

  /** Map from {@link TypeKind} to annotations. */
  private final Map<TypeKind, Set<AnnotationMirror>> typeKinds;
  /** Map from canonical class name strings to annotations. */
  private final Map<@CanonicalName String, Set<AnnotationMirror>> types;

  /** {@link QualifierHierarchy} */
  private final QualifierHierarchy qualHierarchy;

  private final AnnotatedTypeFactory atypeFactory;

  /**
   * Creates a {@link QualifierUpperBounds} from the given checker, using that checker to determine
   * the annotations that are in the type hierarchy.
   */
  public QualifierUpperBounds(AnnotatedTypeFactory typeFactory) {
    this.atypeFactory = typeFactory;
    this.typeKinds = new EnumMap<>(TypeKind.class);
    this.types = new HashMap<>();

    this.qualHierarchy = typeFactory.getQualifierHierarchy();

    // Get type qualifiers from the checker.
    Set<Class<? extends Annotation>> quals = typeFactory.getSupportedTypeQualifiers();

    // For each qualifier, read the @UpperBoundFor annotation and put its type classes and kinds
    // into maps.
    for (Class<? extends Annotation> qual : quals) {
      UpperBoundFor upperBoundFor = qual.getAnnotation(UpperBoundFor.class);
      if (upperBoundFor == null) {
        continue;
      }

      AnnotationMirror theQual = AnnotationBuilder.fromClass(typeFactory.getElementUtils(), qual);
      for (org.checkerframework.framework.qual.TypeKind typeKind : upperBoundFor.typeKinds()) {
        TypeKind mappedTk = mapTypeKinds(typeKind);
        addTypeKind(mappedTk, theQual);
      }

      for (Class<?> typeName : upperBoundFor.types()) {
        addType(typeName, theQual);
      }
    }
  }

  /**
   * Map between {@link org.checkerframework.framework.qual.TypeKind} and {@link TypeKind}.
   *
   * @param typeKind the Checker Framework TypeKind
   * @return the javax TypeKind
   */
  private TypeKind mapTypeKinds(org.checkerframework.framework.qual.TypeKind typeKind) {
    return TypeKind.valueOf(typeKind.name());
  }

  /** Add default qualifier, {@code theQual}, for the given TypeKind. */
  public void addTypeKind(TypeKind typeKind, AnnotationMirror theQual) {
    boolean res = qualHierarchy.updateMappingToMutableSet(typeKinds, typeKind, theQual);
    if (!res) {
      throw new BugInCF(
          "QualifierUpperBounds: invalid update of typeKinds $s at %s with %s.",
          typeKinds, typeKind, theQual);
    }
  }

  /** Add default qualifier, {@code theQual}, for the given class. */
  public void addType(Class<?> type, AnnotationMirror theQual) {
    String typeNameString = type.getCanonicalName();
    boolean res = qualHierarchy.updateMappingToMutableSet(types, typeNameString, theQual);
    if (!res) {
      throw new BugInCF(
          "QualifierUpperBounds: invalid update of types $s at %s with %s.", types, type, theQual);
    }
  }

  /**
   * Returns the set of qualifiers that are the upper bounds for a use of the type.
   *
   * @param type the TypeMirror
   * @return the set of qualifiers that are the upper bounds for a use of the type
   */
  public Set<AnnotationMirror> getBoundQualifiers(TypeMirror type) {
    AnnotationMirrorSet bounds = new AnnotationMirrorSet();
    String qname;
    if (type.getKind() == TypeKind.DECLARED) {
      DeclaredType declaredType = (DeclaredType) type;
      bounds.addAll(getAnnotationFromElement(declaredType.asElement()));
      qname = TypesUtils.getQualifiedName(declaredType);
    } else if (type.getKind().isPrimitive()) {
      qname = type.toString();
    } else {
      qname = null;
    }

    if (qname != null && types.containsKey(qname)) {
      Set<AnnotationMirror> fnd = types.get(qname);
      addMissingAnnotations(bounds, fnd);
    }

    // If the type's kind is in the appropriate map, annotate the type.

    if (typeKinds.containsKey(type.getKind())) {
      Set<AnnotationMirror> fnd = typeKinds.get(type.getKind());
      addMissingAnnotations(bounds, fnd);
    }

    addMissingAnnotations(bounds, atypeFactory.getDefaultTypeDeclarationBounds());
    return bounds;
  }

  /**
   * Returns the explicit annotations on the element. Subclass can override this behavior to add
   * annotations.
   *
   * @param element element whose annotations to return
   * @return the explicit annotations on the element
   */
  protected Set<AnnotationMirror> getAnnotationFromElement(Element element) {
    return atypeFactory.fromElement(element).getAnnotations();
  }

  /**
   * Adds each annotation in {@code missing} to {@code annos}, for which no annotation from the same
   * qualifier hierarchy is present.
   *
   * @param annos an annotation set to side-effect
   * @param missing annotations to add to {@code annos}, if {@code annos} does not have an
   *     annotation from the same qualifier hierarchy
   */
  private void addMissingAnnotations(
      AnnotationMirrorSet annos, Set<? extends AnnotationMirror> missing) {
    for (AnnotationMirror miss : missing) {
      if (atypeFactory.getQualifierHierarchy().findAnnotationInSameHierarchy(annos, miss) == null) {
        annos.add(miss);
      }
    }
  }
}
