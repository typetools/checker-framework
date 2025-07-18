package org.checkerframework.framework.util.typeinference8.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * This is the super class for a qualifier, {@link Qualifier} or a qualifier variable, {@link
 * QualifierVar}. A {@link Qualifier} is a wrapper around {@code AnnotationMirror}. A {@code
 * QualifierVar} is a variable for a polymorphic qualifier that needs to be viewpoint adapted at a
 * call site.
 */
public abstract class AbstractQualifier {

  /** The (interned) name of the top qualifier in the same hierarchy as the qualifier. */
  protected final @Interned @CanonicalName String hierarchyName;

  /** The context. */
  protected final Java8InferenceContext context;

  /**
   * Creates an {@code AbstractQualifier}.
   *
   * @param anno an annotation mirror
   * @param context the context
   */
  AbstractQualifier(AnnotationMirror anno, Java8InferenceContext context) {
    AnnotationMirror top = context.typeFactory.getQualifierHierarchy().getTopAnnotation(anno);
    hierarchyName = AnnotationUtils.annotationName(top).intern();
    this.context = context;
  }

  /**
   * Returns whether {@code other} is in the same hierarchy as this.
   *
   * @param other another abstract qualifier
   * @return whether {@code other} is in the same hierarchy as this
   */
  public boolean sameHierarchy(AbstractQualifier other) {
    return this.hierarchyName == other.hierarchyName;
  }

  /**
   * Returns the instantiation of this.
   *
   * @return the instantiation of this
   */
  abstract AnnotationMirror getInstantiation();

  /**
   * Returns the least upper bounds of {@code quals}.
   *
   * @param quals a set of qualifiers; can contain multiple qualifiers for multiple hierarchies and
   *     multiple qualifiers for a hierarchy
   * @param context a context
   * @return the least upper bounds of {@code quals}
   */
  public static Set<AnnotationMirror> lub(
      Set<AbstractQualifier> quals, Java8InferenceContext context) {
    return combine(
        quals, context.typeFactory.getQualifierHierarchy()::leastUpperBoundQualifiersOnly);
  }

  /**
   * Returns the greatest lower bounds of {@code quals}.
   *
   * @param quals a set of qualifiers; can contain multiple qualifiers for multiple hierarchies and
   *     multiple qualifiers for a hierarchy
   * @param context a context
   * @return the greatest lowest bounds of {@code quals}
   */
  public static Set<AnnotationMirror> glb(
      Set<AbstractQualifier> quals, Java8InferenceContext context) {
    return combine(
        quals, context.typeFactory.getQualifierHierarchy()::greatestLowerBoundQualifiersOnly);
  }

  /**
   * Returns the result of applying the {@code combine} function on {@code quals}.
   *
   * @param quals a set of qualifiers; can contain multiple qualifiers for multiple hierarchies and
   *     multiple qualifiers for a hierarchy
   * @param combine a functions that combines two {@code AnnotationMirror}s and returns a single
   *     {@code AnnotationMirror}
   * @return the result of applying the {@code combine} function on {@code quals}
   */
  private static Set<AnnotationMirror> combine(
      Set<AbstractQualifier> quals, BinaryOperator<AnnotationMirror> combine) {
    Map<String, AnnotationMirror> m = new HashMap<>();

    for (AbstractQualifier qual : quals) {
      AnnotationMirror lub = m.get(qual.hierarchyName);
      if (lub != null) {
        lub = combine.apply(lub, qual.getInstantiation());
      } else {
        lub = qual.getInstantiation();
      }
      m.put(qual.hierarchyName, lub);
    }
    return new AnnotationMirrorSet(m.values());
  }

  /**
   * Creates an {@code AbstractQualifier} for each {@code AnnotationMirror} in {@code annos}. If an
   * annotation mirror is a polymorphic qualifier in {@code qualifierVars}, the {@code QualifierVar}
   * it maps to in {@code qualifierVars} is added to the returned set. Otherwise, a new {@code
   * Qualifier} is added.
   *
   * @param annos a set of annotation mirrors
   * @param qualifierVars a map from polymorphic qualifiers to {@link QualifierVar}
   * @param context a context
   * @return a set containing an {@code AbstractQualifier} for each annotation in {@code
   *     qualifierVars}
   */
  public static Set<AbstractQualifier> create(
      Set<AnnotationMirror> annos,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context) {
    if (qualifierVars.isEmpty()) {
      return create(annos, context);
    }

    Set<AbstractQualifier> quals = new HashSet<>();
    for (AnnotationMirror anno : annos) {
      if (qualifierVars.containsKey(anno)) {
        quals.add(qualifierVars.get(anno));
      } else {
        quals.add(new Qualifier(anno, context));
      }
    }
    return quals;
  }

  /**
   * Creates new {@code Qualifier} is added for each annotation in {@code annos}.
   *
   * @param annos a set of annotation mirrors
   * @param context a context
   * @return new {@code Qualifier} is added for each annotation in {@code annos}
   */
  private static Set<AbstractQualifier> create(
      Set<AnnotationMirror> annos, Java8InferenceContext context) {
    Set<AbstractQualifier> quals = new HashSet<>();
    for (AnnotationMirror anno : annos) {
      quals.add(new Qualifier(anno, context));
    }
    return quals;
  }
}
