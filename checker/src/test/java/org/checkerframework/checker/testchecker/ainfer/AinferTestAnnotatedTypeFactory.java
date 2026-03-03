package org.checkerframework.checker.testchecker.ainfer;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferDefaultType;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferImplicitAnno;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSiblingWithFields;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTreatAsSibling1;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * AnnotatedTypeFactory to test whole-program inference using .jaif files.
 *
 * <p>The used qualifier hierarchy is only intended for test purposes. It is:
 *
 * <pre>{@code
 *                   AinferTop
 *                      |
 *               AinferDefaultType
 *                      |
 *                 AinferParent
 *               /      |       \
 *  AinferSibling AinferSibling2 AinferSiblingWithFields
 *               \      |       /
 *              AinferImplicitAnno
 *                      |
 *                 AinferBottom
 *
 * AinferTreatAsSibling1 : a declaration annotation
 * AinferToIgnore : unused
 * }</pre>
 */
public class AinferTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  private final AnnotationMirror PARENT =
      new AnnotationBuilder(processingEnv, AinferParent.class).build();
  private final AnnotationMirror BOTTOM =
      new AnnotationBuilder(processingEnv, AinferBottom.class).build();
  private final AnnotationMirror IMPLICIT_ANNO =
      new AnnotationBuilder(processingEnv, AinferImplicitAnno.class).build();

  private final AnnotationMirror SIBLING1 =
      new AnnotationBuilder(processingEnv, AinferSibling1.class).build();

  private final AnnotationMirror TREAT_AS_SIBLING1 =
      new AnnotationBuilder(processingEnv, AinferTreatAsSibling1.class).build();

  /** The AinferSiblingWithFields.value field/element. */
  private final ExecutableElement siblingWithFieldsValueElement =
      TreeUtils.getMethod(AinferSiblingWithFields.class, "value", 0, processingEnv);

  /** The AinferSiblingWithFields.value2 field/element. */
  private final ExecutableElement siblingWithFieldsValue2Element =
      TreeUtils.getMethod(AinferSiblingWithFields.class, "value2", 0, processingEnv);

  /**
   * Creates an AinferTestAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public AinferTestAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    // Support a declaration annotation that has the same meaning as @Sibling1, to test that the
    // WPI feature allowing inference of declaration annotations works as intended.
    addAliasedTypeAnnotation(AinferTreatAsSibling1.class, SIBLING1);
    postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<Class<? extends Annotation>>(
        Arrays.asList(
            AinferParent.class,
            AinferDefaultType.class,
            AinferTop.class,
            AinferSibling1.class,
            AinferSibling2.class,
            AinferBottom.class,
            AinferSiblingWithFields.class,
            AinferImplicitAnno.class));
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    LiteralTreeAnnotator literalTreeAnnotator = new LiteralTreeAnnotator(this);
    literalTreeAnnotator.addLiteralKind(LiteralKind.INT, BOTTOM);
    literalTreeAnnotator.addStandardLiteralQualifiers();

    return new ListTreeAnnotator(
        new PropagationTreeAnnotator(this),
        literalTreeAnnotator,
        new AinferTestTreeAnnotator(this));
  }

  protected class AinferTestTreeAnnotator extends TreeAnnotator {

    /**
     * Create a new AinferTestTreeAnnotator.
     *
     * @param atypeFactory the type factory
     */
    protected AinferTestTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitClass(ClassTree classTree, AnnotatedTypeMirror type) {
      WholeProgramInference wpi = atypeFactory.getWholeProgramInference();
      TypeElement classElt = TreeUtils.elementFromDeclaration(classTree);
      if (wpi != null && classElt.getSimpleName().contentEquals("IShouldBeSibling1")) {
        wpi.addClassDeclarationAnnotation(classElt, SIBLING1);
      }
      return super.visitClass(classTree, type);
    }

    @Override
    public Void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
      WholeProgramInference wpi = atypeFactory.getWholeProgramInference();
      VariableElement varElt = TreeUtils.elementFromDeclaration(variableTree);
      if (wpi != null && varElt.getSimpleName().contentEquals("iShouldBeTreatedAsSibling1")) {
        wpi.addFieldDeclarationAnnotation(varElt, TREAT_AS_SIBLING1);
      }
      return super.visitVariable(variableTree, type);
    }

    @Override
    public Void visitMethod(MethodTree methodTree, AnnotatedTypeMirror type) {
      WholeProgramInference wpi = atypeFactory.getWholeProgramInference();
      if (wpi != null) {
        ExecutableElement execElt = TreeUtils.elementFromDeclaration(methodTree);
        int numParams = execElt.getParameters().size();
        for (int i = 0; i < numParams; ++i) {
          VariableElement param = execElt.getParameters().get(i);
          if (param.getSimpleName().contentEquals("iShouldBeTreatedAsSibling1")) {
            wpi.addDeclarationAnnotationToFormalParameter(execElt, i + 1, TREAT_AS_SIBLING1);
          }
        }
      }
      return super.visitMethod(methodTree, type);
    }
  }

  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);
    // If an element has an @AinferTreatAsSibling1 annotation, replace its type with
    // @AinferSibling1.
    // This should be handled by the fact that @AinferTreatAsSibling1 and @AinferSibling1 are
    // aliases, but by default the CF does not look for declaration annotations
    // that are aliases of type annotations in annotation files.
    // TODO: is that a bug in the CF or expected behavior?
    if (getDeclAnnotation(elt, AinferTreatAsSibling1.class) != null) {
      type.replaceAnnotation(SIBLING1);
    }
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new AinferTestQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
  }

  /**
   * Using a MultiGraphQualifierHierarchy to enable tests with Annotations that contain fields.
   *
   * @see AinferSiblingWithFields
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
      super(qualifierClasses, elements, AinferTestAnnotatedTypeFactory.this);
      SIBLING_WITH_FIELDS_KIND = getQualifierKind(AinferSiblingWithFields.class.getCanonicalName());
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
      return BOTTOM;
    }

    @Override
    public AnnotationMirrorSet getBottomAnnotations() {
      return new AnnotationMirrorSet(BOTTOM);
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
