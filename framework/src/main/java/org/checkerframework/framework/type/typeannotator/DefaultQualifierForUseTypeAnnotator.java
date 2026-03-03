package org.checkerframework.framework.type.typeannotator;

import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.qual.DefaultQualifierForUse;
import org.checkerframework.framework.qual.NoDefaultQualifierForUse;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.MapsP;

/**
 * Implements support for {@link DefaultQualifierForUse} and {@link NoDefaultQualifierForUse}. Adds
 * default annotations on types that have no annotation.
 */
public class DefaultQualifierForUseTypeAnnotator extends TypeAnnotator {

  /** The DefaultQualifierForUse.value field/element. */
  private final ExecutableElement defaultQualifierForUseValueElement;

  /** The NoDefaultQualifierForUse.value field/element. */
  private final ExecutableElement noDefaultQualifierForUseValueElement;

  /**
   * Creates an DefaultQualifierForUseTypeAnnotator for {@code typeFactory}.
   *
   * @param typeFactory the type factory
   */
  public DefaultQualifierForUseTypeAnnotator(AnnotatedTypeFactory typeFactory) {
    super(typeFactory);
    ProcessingEnvironment processingEnv = typeFactory.getProcessingEnv();
    defaultQualifierForUseValueElement =
        TreeUtils.getMethod(DefaultQualifierForUse.class, "value", 0, processingEnv);
    noDefaultQualifierForUseValueElement =
        TreeUtils.getMethod(NoDefaultQualifierForUse.class, "value", 0, processingEnv);
  }

  // There is no `visitPrimitive()` because `@DefaultQualifierForUse` is an annotation the goes on
  // a type declaration. Defaults for primitives are add via the meta-annotation @DefaultFor,
  // which is handled elsewhere.

  @Override
  public Void visitDeclared(AnnotatedDeclaredType type, Void aVoid) {
    Element element = type.getUnderlyingType().asElement();
    AnnotationMirrorSet annosToApply = getDefaultAnnosForUses(element);
    type.addMissingAnnotations(annosToApply);
    return super.visitDeclared(type, aVoid);
  }

  /**
   * Cache of elements to the set of annotations that should be applied to unannotated uses of the
   * element.
   */
  protected final Map<Element, AnnotationMirrorSet> elementToDefaults = MapsP.createLruCache(100);

  /** Clears all caches. */
  public void clearCache() {
    elementToDefaults.clear();
  }

  /**
   * Returns the set of qualifiers that should be applied to unannotated uses of the given element.
   *
   * @param element the element for which to determine default qualifiers
   * @return the set of qualifiers that should be applied to unannotated uses of {@code element}
   */
  protected AnnotationMirrorSet getDefaultAnnosForUses(Element element) {
    if (atypeFactory.shouldCache && elementToDefaults.containsKey(element)) {
      return elementToDefaults.get(element);
    }
    AnnotationMirrorSet explictAnnos = getExplicitAnnos(element);
    AnnotationMirrorSet defaultAnnos = getDefaultQualifierForUses(element);
    AnnotationMirrorSet noDefaultAnnos = getHierarchiesNoDefault(element);
    AnnotationMirrorSet annosToApply = new AnnotationMirrorSet();

    for (AnnotationMirror top : atypeFactory.getQualifierHierarchy().getTopAnnotations()) {
      if (AnnotationUtils.containsSame(noDefaultAnnos, top)) {
        continue;
      }
      AnnotationMirror defaultAnno =
          atypeFactory.getQualifierHierarchy().findAnnotationInHierarchy(defaultAnnos, top);
      if (defaultAnno != null) {
        annosToApply.add(defaultAnno);
      } else {
        AnnotationMirror explict =
            atypeFactory.getQualifierHierarchy().findAnnotationInHierarchy(explictAnnos, top);
        if (explict != null) {
          annosToApply.add(explict);
        }
      }
    }
    // If parsing stub files, then the annosToApply is incomplete, so don't cache them.
    if (atypeFactory.shouldCache
        && !atypeFactory.stubTypes.isParsing()
        && !atypeFactory.ajavaTypes.isParsing()) {
      elementToDefaults.put(element, annosToApply);
    }
    return annosToApply;
  }

  /**
   * Returns the annotations explicitly written on the element.
   *
   * @param element an element
   * @return the annotations explicitly written on the element
   */
  protected AnnotationMirrorSet getExplicitAnnos(Element element) {
    AnnotatedTypeMirror explicitAnnoOnDecl = atypeFactory.fromElement(element);
    return explicitAnnoOnDecl.getPrimaryAnnotations();
  }

  /**
   * Returns the default qualifiers for uses of {@code element} as specified by a {@link
   * DefaultQualifierForUse} annotation.
   *
   * <p>Subclasses may override to use an annotation other than {@link DefaultQualifierForUse}.
   *
   * @param element an element
   * @return the default qualifiers for uses of {@code element}
   */
  protected AnnotationMirrorSet getDefaultQualifierForUses(Element element) {
    AnnotationMirror defaultQualifier =
        atypeFactory.getDeclAnnotation(element, DefaultQualifierForUse.class);
    if (defaultQualifier == null) {
      return AnnotationMirrorSet.emptySet();
    }
    return supportedAnnosFromAnnotationMirror(
        AnnotationUtils.getElementValueClassNames(
            defaultQualifier, defaultQualifierForUseValueElement));
  }

  /**
   * Returns top annotations in hierarchies for which no default for use qualifier should be added.
   *
   * @param element an element
   * @return top annotations in hierarchies for which no default for use qualifier should be added
   */
  protected AnnotationMirrorSet getHierarchiesNoDefault(Element element) {
    AnnotationMirror noDefaultQualifier =
        atypeFactory.getDeclAnnotation(element, NoDefaultQualifierForUse.class);
    if (noDefaultQualifier == null) {
      return AnnotationMirrorSet.emptySet();
    }
    return supportedAnnosFromAnnotationMirror(
        AnnotationUtils.getElementValueClassNames(
            noDefaultQualifier, noDefaultQualifierForUseValueElement));
  }

  /**
   * Returns the set of qualifiers supported by this type system from the value element of {@code
   * annotationMirror}.
   *
   * @param annoClassNames a list of annotation class names
   * @return the set of qualifiers supported by this type system from the value element of {@code
   *     annotationMirror}
   */
  protected final AnnotationMirrorSet supportedAnnosFromAnnotationMirror(
      List<@CanonicalName Name> annoClassNames) {
    AnnotationMirrorSet supportAnnos = new AnnotationMirrorSet();
    for (Name annoName : annoClassNames) {
      AnnotationMirror anno = AnnotationBuilder.fromName(atypeFactory.getElementUtils(), annoName);
      if (atypeFactory.isSupportedQualifier(anno)) {
        supportAnnos.add(anno);
      }
    }
    return supportAnnos;
  }
}
