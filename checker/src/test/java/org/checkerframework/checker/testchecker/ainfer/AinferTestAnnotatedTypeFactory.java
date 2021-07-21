package org.checkerframework.checker.testchecker.ainfer;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.DefaultType;
import org.checkerframework.checker.testchecker.ainfer.qual.ImplicitAnno;
import org.checkerframework.checker.testchecker.ainfer.qual.Parent;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.SiblingWithFields;
import org.checkerframework.checker.testchecker.ainfer.qual.Top;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * AnnotatedTypeFactory to test whole-program inference using .jaif files.
 *
 * <p>The used qualifier hierarchy is straightforward and only intended for test purposes.
 */
public class AinferTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  private final AnnotationMirror PARENT =
      new AnnotationBuilder(processingEnv, Parent.class).build();
  private final AnnotationMirror BOTTOM =
      new AnnotationBuilder(processingEnv, AinferBottom.class).build();
  private final AnnotationMirror IMPLICIT_ANNO =
      new AnnotationBuilder(processingEnv, ImplicitAnno.class).build();

  /** The SiblingWithFields.value field/element. */
  private final ExecutableElement siblingWithFieldsValueElement =
      TreeUtils.getMethod(SiblingWithFields.class, "value", 0, processingEnv);
  /** The SiblingWithFields.value2 field/element. */
  private final ExecutableElement siblingWithFieldsValue2Element =
      TreeUtils.getMethod(SiblingWithFields.class, "value2", 0, processingEnv);

  public AinferTestAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<Class<? extends Annotation>>(
        Arrays.asList(
            Parent.class,
            DefaultType.class,
            Top.class,
            Sibling1.class,
            Sibling2.class,
            AinferBottom.class,
            SiblingWithFields.class,
            ImplicitAnno.class));
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    LiteralTreeAnnotator literalTreeAnnotator = new LiteralTreeAnnotator(this);
    literalTreeAnnotator.addLiteralKind(LiteralKind.INT, BOTTOM);
    literalTreeAnnotator.addStandardLiteralQualifiers();

    return new ListTreeAnnotator(new PropagationTreeAnnotator(this), literalTreeAnnotator);
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new AinferTestQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
  }

  /**
   * Using a MultiGraphQualifierHierarchy to enable tests with Annotations that contain fields. @see
   * SiblingWithFields.
   */
  protected class AinferTestQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

    private final QualifierKind SIBLING_WITH_FIELDS_KIND;

    /**
     * Creates a AinferTestQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     */
    protected AinferTestQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
      super(qualifierClasses, elements);
      SIBLING_WITH_FIELDS_KIND = getQualifierKind(SiblingWithFields.class.getCanonicalName());
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
      return BOTTOM;
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
      return Collections.singleton(BOTTOM);
    }

    @Override
    protected AnnotationMirror greatestLowerBoundWithElements(
        AnnotationMirror a1,
        QualifierKind qualifierKind1,
        AnnotationMirror a2,
        QualifierKind qualifierKind2,
        QualifierKind glbKind) {
      if (qualifierKind1 == qualifierKind2 && qualifierKind1 == SIBLING_WITH_FIELDS_KIND) {
        if (isSubtypeWithElements(a1, qualifierKind1, a2, qualifierKind2)) {
          return a1;
        } else {
          return IMPLICIT_ANNO;
        }
      } else if (qualifierKind1 == SIBLING_WITH_FIELDS_KIND) {
        return a1;
      } else if (qualifierKind2 == SIBLING_WITH_FIELDS_KIND) {
        return a2;
      }
      throw new TypeSystemError("Unexpected qualifiers: %s %s", a1, a2);
    }

    @Override
    protected AnnotationMirror leastUpperBoundWithElements(
        AnnotationMirror a1,
        QualifierKind qualifierKind1,
        AnnotationMirror a2,
        QualifierKind qualifierKind2,
        QualifierKind lubKind) {
      if (qualifierKind1 == qualifierKind2 && qualifierKind1 == SIBLING_WITH_FIELDS_KIND) {
        if (isSubtypeWithElements(a1, qualifierKind1, a2, qualifierKind2)) {
          return a1;
        } else {
          return PARENT;
        }
      } else if (qualifierKind1 == SIBLING_WITH_FIELDS_KIND) {
        return a1;
      } else if (qualifierKind2 == SIBLING_WITH_FIELDS_KIND) {
        return a2;
      }
      throw new TypeSystemError("Unexpected qualifiers: %s %s", a1, a2);
    }

    @Override
    protected boolean isSubtypeWithElements(
        AnnotationMirror subAnno,
        QualifierKind subKind,
        AnnotationMirror superAnno,
        QualifierKind superKind) {
      if (subKind == SIBLING_WITH_FIELDS_KIND && superKind == SIBLING_WITH_FIELDS_KIND) {
        List<String> subVal1 =
            AnnotationUtils.getElementValueArray(
                subAnno, siblingWithFieldsValueElement, String.class, Collections.emptyList());
        List<String> supVal1 =
            AnnotationUtils.getElementValueArray(
                superAnno, siblingWithFieldsValueElement, String.class, Collections.emptyList());
        String subVal2 =
            AnnotationUtils.getElementValue(
                subAnno, siblingWithFieldsValue2Element, String.class, "");
        String supVal2 =
            AnnotationUtils.getElementValue(
                superAnno, siblingWithFieldsValue2Element, String.class, "");
        return subVal1.equals(supVal1) && subVal2.equals(supVal2);
      }
      throw new TypeSystemError("Unexpected qualifiers: %s %s", subAnno, superAnno);
    }
  }
}
