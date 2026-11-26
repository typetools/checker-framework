package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
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
import org.checkerframework.checker.collectionownership.qual.CreatesCollectionObligation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.OverrideChecker;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
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

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    // Enforce source-level mutator restrictions before base type-checking.
    enforceCreatesCollectionObligationPolicy(tree);
    return super.visitMethodInvocation(tree, p);
  }

  /**
   * Enforces the mutator policy for methods annotated {@code @CreatesCollectionObligation}.
   *
   * <p>A {@code @NotOwningCollection} receiver may only accept an inserted argument that is
   * definitely non-owning at the call site. Owning receivers are allowed; RLC models any collection
   * obligation they create.
   *
   * @param tree the method invocation tree
   */
  private void enforceCreatesCollectionObligationPolicy(MethodInvocationTree tree) {
    ExecutableElement methodElement = TreeUtils.elementFromUse(tree);
    if (!atypeFactory.isCreatesCollectionObligationMethod(methodElement)) {
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
      return;
    }
    // The receiver is borrowed if it is @NotOwningCollection either where it is evaluated or where
    // the call occurs.  The two can differ if an argument reassigns the receiver variable, as in
    // `x.add((x = y).size(), r)`.
    boolean receiverIsBorrowed =
        getCoTypeAtTree(receiverTree, atypeFactory.getStoreAfter(receiverTree))
                == CollectionOwnershipAnnotatedTypeFactory.CollectionOwnershipType
                    .NotOwningCollection
            || getCoTypeAtTree(receiverTree, atypeFactory.getStoreBefore(tree))
                == CollectionOwnershipAnnotatedTypeFactory.CollectionOwnershipType
                    .NotOwningCollection;
    // Owning receivers are handled by CO analysis.
    if (!receiverIsBorrowed) {
      return;
    }
    ExpressionTree insertedArgumentTree = atypeFactory.getInsertedArgumentTree(tree);
    if (insertedArgumentTree == null) {
      return;
    }
    if (atypeFactory.getCollectionMutatorArgumentKind(methodElement, insertedArgumentTree)
        != CollectionOwnershipAnnotatedTypeFactory.CollectionMutatorArgumentKind
            .DEFINITELY_NON_OWNING) {
      checker.reportError(
          insertedArgumentTree,
          "illegal.collection.mutator.owning.insert.into.notowning",
          methodElement.getSimpleName().toString(),
          TreeUtils.toStringTruncated(insertedArgumentTree, 60));
    }
  }

  /**
   * Returns the flow-sensitive collection-ownership qualifier that the given expression has in the
   * given store, or its declared qualifier if the store has no value for it.
   *
   * @param expr an expression tree
   * @param store a store at a program point where {@code expr} is in scope, or null if none is
   *     available
   * @return the collection-ownership qualifier for {@code expr}, or {@code null} if it has none
   */
  private CollectionOwnershipAnnotatedTypeFactory.@Nullable CollectionOwnershipType getCoTypeAtTree(
      ExpressionTree expr, @Nullable CollectionOwnershipStore store) {
    if (store != null) {
      JavaExpression je = JavaExpression.fromTree(expr);
      if (CFAbstractStore.canInsertJavaExpression(je)) {
        CFValue v = store.getValue(je);
        if (v != null) {
          return atypeFactory.getCoType(v.getAnnotations());
        }
      }
    }
    // The store has no value, for example for a field after a method call: use its declared type.
    return atypeFactory.getCoType(atypeFactory.getAnnotatedType(expr).getPrimaryAnnotations());
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
    TypeElement enclosingType = ElementUtils.enclosingTypeElement(constructorElement);
    if (enclosingType == null) {
      throw new BugInCF(
          "Every constructor has an enclosing type, but this one does not: " + constructorElement);
    }
    AnnotatedTypeMirror defaultType = atypeFactory.getAnnotatedType(enclosingType);
    AnnotationMirror defaultAnno = defaultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    AnnotatedTypeMirror resultType = constructorType.getReturnType();
    AnnotationMirror resultAnno = resultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    if (defaultAnno == null || resultAnno == null) {
      return;
    }
    if (!qualHierarchy.isSubtypeShallow(
        defaultAnno, defaultType.getUnderlyingType(), resultAnno, resultType.getUnderlyingType())) {
      checker.reportError(
          constructorElement, "inconsistent.constructor.type", resultAnno, defaultAnno);
    }
  }

  @Override
  protected OverrideChecker createOverrideChecker(
      Tree overriderTree,
      AnnotatedExecutableType overriderMethodType,
      AnnotatedTypeMirror overriderType,
      AnnotatedTypeMirror overriderReturnType,
      AnnotatedExecutableType overriddenMethodType,
      AnnotatedDeclaredType overriddenType,
      AnnotatedTypeMirror overriddenReturnType) {
    return new CollectionOwnershipOverrideChecker(
        overriderTree,
        overriderMethodType,
        overriderType,
        overriderReturnType,
        overriddenMethodType,
        overriddenType,
        overriddenReturnType);
  }

  /**
   * An override checker that does not check the receiver of an overriding method whose receiver
   * type is not a collection (see {@link ResourceLeakUtils#isCollection}), such as a user-defined
   * class that overrides {@code toString()}. No value of such a type is a resource collection, so
   * the collection ownership qualifier of the overridden method's receiver, such as the
   * {@code @NotOwningCollection} receiver of {@link Object#toString()}, does not constrain the
   * overrider.
   */
  private class CollectionOwnershipOverrideChecker extends OverrideChecker {

    /**
     * Creates a CollectionOwnershipOverrideChecker.
     *
     * @param overriderTree the AST node of the overriding method or method reference
     * @param overrider the type of the overriding method
     * @param overriderType the type enclosing the overrider method, usually an
     *     AnnotatedDeclaredType; for method references, it might be something else
     * @param overriderReturnType the return type of the overriding method
     * @param overridden the type of the overridden method
     * @param overriddenType the declared type enclosing the overridden method
     * @param overriddenReturnType the return type of the overridden method
     */
    CollectionOwnershipOverrideChecker(
        Tree overriderTree,
        AnnotatedExecutableType overrider,
        AnnotatedTypeMirror overriderType,
        AnnotatedTypeMirror overriderReturnType,
        AnnotatedExecutableType overridden,
        AnnotatedDeclaredType overriddenType,
        AnnotatedTypeMirror overriddenReturnType) {
      super(
          CollectionOwnershipVisitor.this,
          overriderTree,
          overrider,
          overriderType,
          overriderReturnType,
          overridden,
          overriddenType,
          overriddenReturnType);
    }

    @Override
    protected boolean checkReceiverOverride() {
      AnnotatedDeclaredType overriderReceiver = overrider.getReceiverType();
      if (overriderReceiver != null
          && !ResourceLeakUtils.isCollection(
              overriderReceiver.getUnderlyingType(), atypeFactory.getProcessingEnv())) {
        return true;
      }
      return super.checkReceiverOverride();
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
  public void processMethodTree(String className, MethodTree tree) {
    ExecutableElement method = TreeUtils.elementFromDeclaration(tree);
    if (atypeFactory.getDeclAnnotation(method, CreatesCollectionObligation.class) != null) {
      checkInsertedParameter(tree, method);
    }
    super.processMethodTree(className, tree);
  }

  /**
   * Checks that the parameter that a {@code @CreatesCollectionObligation} method inserts has the
   * collection's element type, or is a collection, for a method that inserts all the elements of
   * another collection. See {@link CreatesCollectionObligation#element}.
   *
   * @param tree the declaration of a {@code @CreatesCollectionObligation} method
   * @param method the method's element
   */
  private void checkInsertedParameter(MethodTree tree, ExecutableElement method) {
    TypeElement collectionElt = ElementUtils.enclosingTypeElement(method);
    TypeMirror elementType =
        collectionElt == null
            ? null
            : ResourceLeakUtils.getCollectionElementType(
                collectionElt.asType(), atypeFactory.getProcessingEnv());
    if (elementType == null) {
      return;
    }
    int index = atypeFactory.getInsertedParameterIndex(method);
    if (index < 0) {
      String reference = atypeFactory.getInsertedParameterReference(method);
      checker.reportError(
          tree,
          "creates.collection.obligation.element.missing",
          method.getSimpleName(),
          reference.isEmpty() ? "its last parameter" : "parameter " + reference);
      return;
    }
    VariableElement param = method.getParameters().get(index);
    if (!types.isSubtype(param.asType(), elementType)
        && !ResourceLeakUtils.isCollection(param.asType(), atypeFactory.getProcessingEnv())) {
      checker.reportError(
          tree,
          "creates.collection.obligation.element",
          method.getSimpleName(),
          param.getSimpleName(),
          elementType);
    }
  }

  /**
   * Also checks that the {@code @CollectionFieldDestructor} postcondition of a destructor, that is,
   * a must-call method of its class, holds when the destructor throws an exception. Calling the
   * destructor fulfills the caller's obligation even if the destructor throws, so nothing else
   * disposes of the fields.
   */
  @Override
  protected void checkPostcondition(
      MethodTree methodTree, AnnotationMirror annotation, JavaExpression expression) {
    super.checkPostcondition(methodTree, annotation, expression);
    ExecutableElement method = TreeUtils.elementFromDeclaration(methodTree);
    if (!ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(atypeFactory)
        .isMustCallMethod(method)) {
      return;
    }
    CFAbstractStore<?, ?> exitStore = atypeFactory.getExceptionalExitStore(methodTree);
    if (exitStore == null) {
      return;
    }
    CFAbstractValue<?> value = exitStore.getValue(expression);
    AnnotationMirror inferredAnno =
        value == null
            ? null
            : qualHierarchy.findAnnotationInSameHierarchy(value.getAnnotations(), annotation);
    if (!checkContract(expression, annotation, inferredAnno, exitStore)) {
      String expressionString = expression.toString();
      checker.reportError(
          methodTree,
          "contracts.exceptional.postcondition",
          methodTree.getName(),
          contractExpressionAndType(expressionString, inferredAnno),
          contractExpressionAndType(expressionString, annotation));
    }
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
    if (fieldElement == null) {
      return;
    }
    List<String> mustCallValues =
        atypeFactory.getMustCallValuesOfResourceCollectionComponent(fieldTree);

    if (mustCallValues == null || mustCallValues.isEmpty()) {
      return;
    }

    RLCCalledMethodsAnnotatedTypeFactory rlAtf =
        ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(atypeFactory);
    TypeElement enclosingElement = (TypeElement) fieldElement.getEnclosingElement();
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
      // A @MustCall value is a method name, and a call to any overload with that name satisfies
      // the obligation.  So a must-call method destructs the field only if every overload with
      // its name in the class, declared or inherited, is annotated @CollectionFieldDestructor for
      // the field.  getAllMembers omits a method that the class overrides.
      List<? extends Element> membersOfEnclosingClass = elements.getAllMembers(enclosingElement);
      for (String mustCallMethodName : enclosingMustCallValues) {
        boolean hasOverload = false;
        boolean allOverloadsDestructField = true;
        for (Element member : membersOfEnclosingClass) {
          if (member.getKind() == ElementKind.METHOD
              && member.getSimpleName().contentEquals(mustCallMethodName)) {
            hasOverload = true;
            if (!destructsField((ExecutableElement) member, fieldElement)) {
              allOverloadsDestructField = false;
              break;
            }
          }
        }
        if (hasOverload && allOverloadsDestructField) {
          return;
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

  /**
   * Returns true if the given method is annotated {@code @CollectionFieldDestructor} for the given
   * field of {@code this}.
   *
   * @param method a method
   * @param field a field
   * @return true if {@code method} is a {@code @CollectionFieldDestructor} for {@code field}
   */
  private boolean destructsField(ExecutableElement method, VariableElement field) {
    for (String destructedFieldName : atypeFactory.getCollectionFieldDestructorAnnoFields(method)) {
      if (atypeFactory.expressionIsFieldAccess(destructedFieldName, field)) {
        return true;
      }
    }
    return false;
  }
}
