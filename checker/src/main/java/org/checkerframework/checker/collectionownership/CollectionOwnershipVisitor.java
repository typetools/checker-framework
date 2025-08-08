package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
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

  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    AnnotationMirror am = TreeUtils.annotationFromAnnotationTree(tree);
    if (AnnotationUtils.areSame(am, atypeFactory.BOTTOM)
        || AnnotationUtils.areSame(am, atypeFactory.OWNINGCOLLECTIONWITHOUTOBLIGATION)) {
      checker.reportError(tree, "illegal.type.annotation", tree);
    }
    return super.visitAnnotation(tree, p);
  }

  /**
   * This method checks that the result type of a constructor is a supertype of the declared type on
   * the class, if one exists.
   *
   * <p>This method typically issues a warning if the result type of the constructor is not top,
   * because in top-default type systems that indicates a potential problem. For the Collection
   * Ownership Checker, this does not apply, since the default return type is bottom. This is
   * justified by examining all return types and setting them to a safe default in {@code
   * CollectionOwnershipTypeAnnotator#visitExecutable}.
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
    Element elt = TreeUtils.elementFromDeclaration(tree);
    if (elt != null && atypeFactory.isResourceCollectionField(elt)) {
      if (elt.getModifiers().contains(Modifier.STATIC)) {
        // error: static resource collection fields not supported
        checker.reportError(tree, "static.resource.collection.field", tree);
      }
      if (atypeFactory.isOwningCollectionField(elt)) {
        checkOwningCollectionField(tree);
      }
    }
    return super.visitVariable(tree, p);
  }

  /**
   * Checks validity of an {@code OwningCollection} field {@code field}. Say the element type {@code
   * field} is {@code @MustCall("m"}}. This method checks that the enclosing class of {@code field}
   * has a type {@code @MustCall("m2")} for some method {@code m2}, and that {@code m2} has an
   * annotation {@code @CollectionFieldDestructor("field")}, guaranteeing that the {@code @MustCall}
   * obligation of the field will be satisfied.
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

    RLCCalledMethodsAnnotatedTypeFactory rlAtf =
        ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(atypeFactory);
    Element enclosingElement = fieldElement.getEnclosingElement();
    List<String> enclosingMustCallValues = rlAtf.getMustCallValues(enclosingElement);

    String error;
    if (enclosingMustCallValues == null) {
      error =
          " The enclosing element "
              + ElementUtils.getQualifiedName(enclosingElement)
              + " has no @MustCall annotation";
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

          List<String> destructedFields =
              atypeFactory.getCollectionFieldDestructorAnnoFields(siblingMethod);
          for (String destructedFieldName : destructedFields) {
            if (atypeFactory.expressionIsFieldAccess(destructedFieldName, fieldElement)) {
              return;
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
        "unfulfilled.field.obligations",
        fieldTree.getName().toString(),
        MustCallConsistencyAnalyzer.formatMissingMustCallMethods(mustCallValues),
        error);
  }
}
