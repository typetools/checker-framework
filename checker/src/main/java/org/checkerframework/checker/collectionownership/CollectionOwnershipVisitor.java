package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.collectionownership.qual.CollectionFieldDestructor;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Collection Ownership Checker. This visitor is similar to BaseTypeVisitor, but
 * overrides methods that don't work well with the ownership type hierarchy because it doesn't use
 * the top type as the default type.
 */
public class CollectionOwnershipVisitor
    extends BaseTypeVisitor<CollectionOwnershipAnnotatedTypeFactory> {

  /**
   * Creates a new CollectionOwnershipVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public CollectionOwnershipVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * This method typically issues a warning if the result type of the constructor is not top,
   * because in top-default type systems that indicates a potential problem. The Must Call Checker
   * does not need this warning, because it expects the type of all constructors to be {@code
   * OwningCollectionBottom} (by default).
   *
   * <p>Instead, this method checks that the result type of a constructor is a supertype of the
   * declared type on the class, if one exists.
   *
   * @param constructorType an AnnotatedExecutableType for the constructor
   * @param constructorElement element that declares the constructor
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    AnnotatedTypeMirror defaultType =
        atypeFactory.getAnnotatedType(ElementUtils.enclosingTypeElement(constructorElement));
    AnnotationMirror defaultAnno = defaultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    AnnotatedTypeMirror resultType = constructorType.getReturnType();
    AnnotationMirror resultAnno = resultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    if (!qualHierarchy.isSubtypeShallow(
        defaultAnno, defaultType.getUnderlyingType(), resultAnno, resultType.getUnderlyingType())) {
      checker.reportError(
          constructorElement, "inconsistent.constructor.type", resultAnno, defaultAnno);
    }
  }

  /**
   * Change the default for exception parameter lower bounds to bottom (the default), to prevent
   * false positives.
   *
   * @return a set containing only the Bottom annotation
   */
  @Override
  protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
    return new AnnotationMirrorSet(atypeFactory.BOTTOM);
  }

  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    if (atypeFactory.isOwningCollectionField(tree)) {
      checkOwningCollectionField(tree);
    }
    return super.visitVariable(tree, p);
  }

  /**
   * Checks validity of an {@code OwningCollection} field {@code field}. Say the type of the
   * elements of {@code field} is {@code @MustCall("m"}}. This method checks that the enclosing
   * class of {@code field} has a type {@code @MustCall("m2")} for some method {@code m2}, and that
   * {@code m2} has an annotation {@code @CollectionFieldDestructor("field")}, guaranteeing that the
   * {@code @MustCall} obligation of the field will be satisfied.
   *
   * @param fieldTree the declaration of the field to check
   */
  private void checkOwningCollectionField(VariableTree fieldTree) {
    VariableElement fieldElement = TreeUtils.elementFromDeclaration(fieldTree);
    List<String> mustCallValues =
        atypeFactory.getMustCallValuesOfResourceCollectionComponent(fieldTree);

    if (mustCallValues == null || mustCallValues.isEmpty()) {
      return;
    }

    String error;
    RLCCalledMethodsAnnotatedTypeFactory rlAtf =
        ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(atypeFactory);
    Element enclosingElement = fieldElement.getEnclosingElement();
    List<String> enclosingMustCallValues = rlAtf.getMustCallValues(enclosingElement);

    if (enclosingMustCallValues == null) {
      error =
          " The enclosing element "
              + ElementUtils.getQualifiedName(enclosingElement)
              + " doesn't have a @MustCall annotation";
    } else if (enclosingMustCallValues.isEmpty()) {
      error =
          " The enclosing element "
              + ElementUtils.getQualifiedName(enclosingElement)
              + " has an empty @MustCall annotation";
    } else {
      List<? extends Element> siblingsOfOwningField = enclosingElement.getEnclosedElements();
      for (Element siblingElement : siblingsOfOwningField) {
        if (siblingElement.getKind() == ElementKind.METHOD
            && enclosingMustCallValues.contains(siblingElement.getSimpleName().toString())) {

          ExecutableElement siblingMethod = (ExecutableElement) siblingElement;

          AnnotationMirror collectionFieldDestructorAnno =
              atypeFactory.getDeclAnnotation(siblingMethod, CollectionFieldDestructor.class);
          if (collectionFieldDestructorAnno != null) {
            List<String> destructedFields =
                AnnotationUtils.getElementValueArray(
                    collectionFieldDestructorAnno,
                    atypeFactory.collectionFieldDestructorValueElement,
                    String.class);
            for (String destructedFieldName : destructedFields) {
              if (expressionEqualsField(destructedFieldName, fieldElement)) {
                return;
              }
            }
          }
        }
      }
      error =
          " No Destructor Method annotated @CollectionFieldDestructor("
              + fieldTree.getName().toString()
              + ") found.";
    }
    checker.reportError(
        fieldTree,
        "unfulfilled.collection.obligations",
        mustCallValues.get(0).equals(CollectionOwnershipAnnotatedTypeFactory.UNKNOWN_METHOD_NAME)
            ? "Unknown"
            : mustCallValues.get(0),
        "field " + fieldTree.getName().toString(),
        error);
  }

  /**
   * Determine if the given expression <code>e</code> refers to <code>this.field</code>.
   *
   * @param e the expression
   * @param field the field
   * @return true if <code>e</code> refers to <code>this.field</code>
   */
  private boolean expressionEqualsField(String e, VariableElement field) {
    try {
      JavaExpression je = StringToJavaExpression.atFieldDecl(e, field, this.checker);
      return je instanceof FieldAccess && ((FieldAccess) je).getField().equals(field);
    } catch (JavaExpressionParseUtil.JavaExpressionParseException ex) {
      // The parsing error will be reported elsewhere, assuming e was derived from an
      // annotation.
      return false;
    }
  }
}
