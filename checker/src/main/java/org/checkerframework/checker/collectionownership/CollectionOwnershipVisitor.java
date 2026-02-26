package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

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

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    // Enforce additional policy for collection mutators.
    enforceCreatesCollectionObligationPolicy(tree);
    return super.visitMethodInvocation(tree, p);
  }

  /**
   * Enforces the "mutations on non-owning collections" policy for methods annotated
   * {@code @CreatesCollectionObligation}.
   *
   * <p>Strategy: (A) Owning receiver (OC or OCWO): the call is a transfer point. It should not be
   * used to insert a definitely-non-owning element. (B) NotOwning receiver (NOC): allow mutation
   * ONLY if the inserted thing is definitely non-owning. This prevents "smuggling" an owning /
   * obligation-carrying element into a non-owning collection.
   *
   * <p>Note: we intentionally do not add an index property to @CreatesCollectionObligation yet. We
   * use a heuristic: the "inserted thing" is the last argument at the call site. TODO: Maybe later
   * remove this heuristic and require and index on the CreatesCollectionObligation annotation to
   * determine the inserted element's index.
   */
  private void enforceCreatesCollectionObligationPolicy(MethodInvocationTree tree) {
    ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
    boolean isCreates =
        atypeFactory.getDeclAnnotation(
                methodElt,
                org.checkerframework.checker.collectionownership.qual.CreatesCollectionObligation
                    .class)
            != null;
    if (!isCreates) {
      return;
    }
    ExpressionTree receiverTree = TreeUtils.getReceiverTree(tree);
    if (receiverTree == null) {
      // Static call or no explicit receiver; not a collection mutator on an instance receiver.
      return;
    }
    if (!atypeFactory.isResourceCollection(receiverTree)) {
      return;
    }
    if (tree.getArguments().isEmpty()) {
      // No "inserted thing" to validate.
      return;
    }
    CollectionOwnershipStore storeBefore = atypeFactory.getStoreBefore(tree);
    CollectionOwnershipAnnotatedTypeFactory.CollectionOwnershipType recvType =
        getCoTypeAtTree(receiverTree, storeBefore);
    if (recvType == null) {
      return;
    }
    // Heuristic: inserted resource is at the last index of call
    ExpressionTree insertedTree = tree.getArguments().get(tree.getArguments().size() - 1);
    RLCCalledMethodsAnnotatedTypeFactory rlAtf =
        ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(atypeFactory);
    boolean insertedDefinitelyNonOwning = isInsertedThingDefinitelyNonOwning(insertedTree, rlAtf);
    String methodName = methodElt.getSimpleName().toString();
    switch (recvType) {
      case NotOwningCollection:
        if (!insertedDefinitelyNonOwning) {
          checker.reportError(
              insertedTree,
              "illegal.collection.mutator.owning.insert.into.notowning",
              methodName,
              TreeUtils.toStringTruncated(insertedTree, 60));
        }
        break;
      case OwningCollection:
      case OwningCollectionWithoutObligation:
        // disallow inserting something that is definitely non-owning into an owning collection.
        if (insertedDefinitelyNonOwning) {
          checker.reportError(
              insertedTree,
              "illegal.collection.mutator.nonowning.insert.into.owning",
              methodName,
              TreeUtils.toStringTruncated(insertedTree, 60));
        }
        break;
      default:
        // bottom : ignore
    }
  }

  /** Gets the flow-sensitive collection-ownership qualifier for {@code expr}, if available. */
  private CollectionOwnershipAnnotatedTypeFactory.CollectionOwnershipType getCoTypeAtTree(
      ExpressionTree expr, CollectionOwnershipStore storeBefore) {
    if (storeBefore != null) {
      JavaExpression je = JavaExpression.fromTree(expr);
      CFValue v = storeBefore.getValue(je);
      if (v != null) {
        return atypeFactory.getCoType(v.getAnnotations());
      }
    }
    return null;
  }

  /**
   * Returns true iff the "inserted thing" is definitely non-owning at this call site.
   *
   * <p>For non-collection values: - annotation-first: explicit @NotOwning on the
   * expression/return/element, if present - fallback: empty @MustCall (no obligation exists)
   *
   * <p>For resource-collection values (e.g., addAll/putAll where the "inserted thing" is a
   * collection): - annotation-first: the argument must be @NotOwningCollection at the call site -
   * we intentionally avoid inspecting element obligations here without extra machinery.
   */
  private boolean isInsertedThingDefinitelyNonOwning(
      ExpressionTree insertedTree, RLCCalledMethodsAnnotatedTypeFactory rlAtf) {

    // If the inserted thing is itself a resource collection (bulk ops e.g. addAll)
    if (atypeFactory.isResourceCollection(insertedTree)) {
      return false;
    }

    // annotation-first: explicit @NotOwning
    // 1) On the element/variable symbol (locals/params/fields)
    Element e = TreeUtils.elementFromTree(insertedTree);
    if (e != null && e.getAnnotation(NotOwning.class) != null) {
      return true;
    }
    // 2) On the return type of a method invocation
    if (insertedTree instanceof MethodInvocationTree) {
      ExecutableElement callee = TreeUtils.elementFromUse((MethodInvocationTree) insertedTree);
      if (rlAtf.hasNotOwning(callee)) {
        return true;
      }
    }
    // 3) On the type at the tree (covers return-type annotations in many cases)
    MustCallAnnotatedTypeFactory mcAtf = rlAtf.getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mcType = mcAtf.getAnnotatedType(insertedTree);
    if (mcType != null && mcType.hasPrimaryAnnotation(NotOwning.class)) {
      return true;
    }

    // fallback: empty must-call => no obligation to "smuggle" ---
    TypeMirror tm = TreeUtils.typeOf(insertedTree);
    TypeElement typeElt = TypesUtils.getTypeElement(tm);
    if (typeElt == null) {
      return false;
    }
    // If the type can never have a must-call obligation, treat as definitely safe.
    return rlAtf.hasEmptyMustCallValue(typeElt);
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
   * Change the default for exception parameter lower bounds to bottom, to prevent false positives.
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
