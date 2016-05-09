package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.lock.qual.EnsuresLockHeld;
import org.checkerframework.checker.lock.qual.EnsuresLockHeldIf;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

/**
 * The LockVisitor enforces the special type-checking rules described in the Lock Checker manual chapter.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */

public class LockVisitor extends BaseTypeVisitor<LockAnnotatedTypeFactory> {
    private final Class<? extends Annotation> checkerGuardedByClass = GuardedBy.class;
    private final Class<? extends Annotation> checkerGuardSatisfiedClass = GuardSatisfied.class;

    private static final Pattern itselfReceiverPattern = Pattern.compile("^itself(\\.(.*))?$");

    public LockVisitor(BaseTypeChecker checker) {
        super(checker);

        checkForAnnotatedJdk();
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) { // visit a variable declaration
        // A user may not annotate a primitive type, a boxed primitive type or a String
        // with any qualifier from the @GuardedBy hierarchy.

        TypeMirror tm = InternalUtils.typeOf(node);

        if (TypesUtils.isBoxedPrimitive(tm) ||
            TypesUtils.isPrimitive(tm) ||
            TypesUtils.isString(tm)) {
            AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
            if (atm.hasExplicitAnnotationRelaxed(atypeFactory.GUARDSATISFIED) ||
                atm.hasExplicitAnnotationRelaxed(atypeFactory.GUARDEDBY) ||
                atm.hasExplicitAnnotation(atypeFactory.GUARDEDBYUNKNOWN) ||
                atm.hasExplicitAnnotation(atypeFactory.GUARDEDBYBOTTOM)) {
                checker.report(Result.failure("primitive.type.guardedby"), node);
            }
        }

        issueErrorIfMoreThanOneGuardedByAnnotationPresent(node);

        return super.visitVariable(node, p);
    }

    /**
     * Issues an error if two or more of the following annotations are present on a variable declaration:<br>
     * {@code @org.checkerframework.checker.lock.qual.GuardedBy}<br>
     * {@code @net.jcip.annotations.GuardedBy}<br>
     * {@code @javax.annotation.concurrent.GuardedBy}
     *
     * @param variableTree the VariableTree for the variable declaration used to determine if
     * multiple @GuardedBy annotations are present and to report the error via checker.report.
     */
    private void issueErrorIfMoreThanOneGuardedByAnnotationPresent(VariableTree variableTree) {
        int guardedByAnnotationCount = 0;

        List<AnnotationMirror> annos = InternalUtils
                .annotationsFromTypeAnnotationTrees(variableTree.getModifiers().getAnnotations());
        for (AnnotationMirror anno : annos) {
            if (AnnotationUtils.areSameByClass(anno, GuardedBy.class) ||
                AnnotationUtils.areSameByClass(anno, net.jcip.annotations.GuardedBy.class) ||
                AnnotationUtils.areSameByClass(anno, javax.annotation.concurrent.GuardedBy.class)) {
                guardedByAnnotationCount++;
                if (guardedByAnnotationCount > 1) {
                    checker.report(Result.failure("multiple.guardedby.annotations"), variableTree);
                    return;
                }
            }
        }
    }


    @Override
    public LockAnnotatedTypeFactory createTypeFactory() {
        return new LockAnnotatedTypeFactory(checker);
    }

    /**
     * Issues an error if a method (explicitly or implicitly) annotated with @MayReleaseLocks has a formal parameter
     * or receiver (explicitly or implicitly) annotated with @GuardSatisfied. Also issues an error if a synchronized
     * method has a @LockingFree, @SideEffectFree or @Pure annotation.
     *
     * @param node the MethodTree of the method definition to visit
     */
    @Override
    public Void visitMethod(MethodTree node, Void p) {
        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);

        issueErrorIfMoreThanOneLockPreconditionMethodAnnotationPresent(methodElement, node);

        SideEffectAnnotation sea = atypeFactory.methodSideEffectAnnotation(methodElement, true);

        if (sea == SideEffectAnnotation.MAYRELEASELOCKS) {
            boolean issueGSwithMRLWarning = false;

            VariableTree receiver = node.getReceiverParameter();
            if (receiver != null) {
                if (atypeFactory.getAnnotatedType(receiver).hasAnnotation(checkerGuardSatisfiedClass)) {
                    issueGSwithMRLWarning = true;
                }
            }

            if (!issueGSwithMRLWarning) { // Skip this loop if it is already known that the warning must be issued.
                for (VariableTree vt : node.getParameters()) {
                    if (atypeFactory.getAnnotatedType(vt).hasAnnotation(checkerGuardSatisfiedClass)) {
                        issueGSwithMRLWarning = true;
                        break;
                    }
                }
            }

            if (issueGSwithMRLWarning) {
                checker.report(Result.failure("guardsatisfied.with.mayreleaselocks"), node);
            }
        }

        // Issue an error if a non-constructor method definition has a return type of @GuardSatisfied without an index.
        if (methodElement != null && methodElement.getKind() != ElementKind.CONSTRUCTOR) {
            AnnotatedTypeMirror returnTypeATM = atypeFactory.getAnnotatedType(node).getReturnType();

            if (returnTypeATM != null && returnTypeATM.hasAnnotation(GuardSatisfied.class)) {
                int returnGuardSatisfiedIndex = atypeFactory.getGuardSatisfiedIndex(returnTypeATM);

                if (returnGuardSatisfiedIndex == -1) {
                    checker.report(Result.failure("guardsatisfied.return.must.have.index"), node);
                }
            }
        }

        if (!sea.isWeakerThan(SideEffectAnnotation.LOCKINGFREE) &&
            methodElement.getModifiers().contains(Modifier.SYNCHRONIZED)) {
            checker.report(Result.failure("lockingfree.synchronized.method", sea), node);
        }

        return super.visitMethod(node, p);
    }

    /**
     * Issues an error if two or more of the following annotations are present on a method:<br>
     * {@code @Holding}<br>
     * {@code @net.jcip.annotations.GuardedBy}<br>
     * {@code @javax.annotation.concurrent.GuardedBy}
     *
     * @param methodElement the ExecutableElement for the method call referred to by {@code node}
     * @param treeForErrorReporting the MethodTree used to report the error via checker.report.
     */
    private void issueErrorIfMoreThanOneLockPreconditionMethodAnnotationPresent(ExecutableElement methodElement,
            MethodTree treeForErrorReporting) {
        int lockPreconditionAnnotationCount = 0;

        if (atypeFactory.getDeclAnnotation(methodElement, Holding.class) != null) {
            lockPreconditionAnnotationCount++;
        }

        if (atypeFactory.getDeclAnnotation(methodElement, net.jcip.annotations.GuardedBy.class) != null) {
            lockPreconditionAnnotationCount++;
        }

        if (lockPreconditionAnnotationCount < 2 &&
            atypeFactory.getDeclAnnotation(methodElement, javax.annotation.concurrent.GuardedBy.class) != null) {
            lockPreconditionAnnotationCount++;
        }

        if (lockPreconditionAnnotationCount > 1) {
            checker.report(Result.failure("multiple.lock.precondition.annotations"), treeForErrorReporting);
        }
    }

    /**
     * When visiting a method call, if the receiver formal parameter has type @GuardSatisfied
     * and the receiver actual parameter has type @GuardedBy(...), this method verifies that
     * the guard is satisfied, and it returns true, indicating that the receiver subtype check should be skipped.
     * If the receiver actual parameter has type @GuardSatisfied, this method simply returns true without
     * performing any other actions. The method returns false otherwise.
     *
     * @param node the MethodInvocationTree of the method being called
     * @param methodDefinitionReceiver the ATM of the formal receiver parameter of the method being called
     * @param methodCallReceiver the ATM of the receiver argument of the method call
     * @return whether the caller can skip the receiver subtype check
     */
    @Override
    protected boolean skipReceiverSubtypeCheck(MethodInvocationTree node,
            AnnotatedTypeMirror methodDefinitionReceiver,
            AnnotatedTypeMirror methodCallReceiver) {

        AnnotationMirror primaryGb = methodCallReceiver.getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
        AnnotationMirror effectiveGb = methodCallReceiver.getEffectiveAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);

        // If the receiver actual parameter has type @GuardSatisfied, skip the subtype check.
        // Consider only a @GuardSatisfied primary annotation - hence use primaryGb instead of effectiveGb.
        if (primaryGb != null && AnnotationUtils.areSameByClass(primaryGb, checkerGuardSatisfiedClass)) {
            AnnotationMirror primaryGbOnMethodDefinition = methodDefinitionReceiver.getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
            if (primaryGbOnMethodDefinition != null && AnnotationUtils.areSameByClass(primaryGbOnMethodDefinition, checkerGuardSatisfiedClass)) {
                return true;
            }
        }

        if (AnnotationUtils.areSameByClass(effectiveGb, checkerGuardedByClass)) {
            Set<AnnotationMirror> annos = methodDefinitionReceiver.getAnnotations();
            for (AnnotationMirror anno : annos) {
                if (AnnotationUtils.areSameByClass(anno, checkerGuardSatisfiedClass)) {
                    MethodInvocationNode methodInvocationNode = (MethodInvocationNode) atypeFactory.getNodeForTree(node);
                    Node receiverNode = methodInvocationNode.getTarget().getReceiver();
                    checkPreconditions(node, receiverNode,
                            generatePreconditionsBasedOnGuards(methodCallReceiver));

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<? extends AnnotationMirror> tops = atypeFactory.getQualifierHierarchy().getTopAnnotations();
        Set<AnnotationMirror> annotationSet = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : tops) {
            if (AnnotationUtils.areSame(anno, atypeFactory.GUARDEDBYUNKNOWN)) {
                annotationSet.add(atypeFactory.GUARDEDBY);
            } else {
                annotationSet.add(anno);
            }
        }
        return annotationSet;
    }

    /**
     * Given an AnnotatedTypeMirror containing a @GuardedBy annotation, returns the set of lock expression preconditions
     * specified in the @GuardedBy annotation.
     * Returns an empty set if no such expressions are found.
     *
     * @param atm the AnnotatedTypeMirror containing the @GuardedBy annotation with the lock expression preconditions.
     * @return a set of lock expression preconditions that can be processed by checkPreconditions
     */
    private Set<Pair<String, String>> generatePreconditionsBasedOnGuards(AnnotatedTypeMirror atm) {
        Set<AnnotationMirror> amList = atm.getAnnotations();
        Set<Pair<String, String>> preconditions = new LinkedHashSet<>();

        if (amList != null) {
            for (AnnotationMirror annotationMirror : amList) {

                if (AnnotationUtils.areSameByClass(annotationMirror, checkerGuardedByClass)) {
                    if (AnnotationUtils.hasElementValue(annotationMirror, "value")) {
                        List<String> guardedByValue = AnnotationUtils.getElementValueArray(annotationMirror, "value", String.class, false);

                        for (String lockExpression : guardedByValue) {
                            preconditions.add(Pair.of(lockExpression, LockHeld.class.getCanonicalName()));
                        }
                    }
                }
            }
        }

        return preconditions;
    }

    @Override
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey) {

        Kind valueTreeKind = valueTree.getKind();

        switch(valueTreeKind) {
            case NEW_CLASS:
            case NEW_ARRAY:
                // Avoid issuing warnings for: @GuardedBy(<something>) Object o = new Object();
                // Do NOT do this if the LHS is @GuardedByBottom.
                if (!varType.hasAnnotation(GuardedByBottom.class)) {
                    return;
                }
                break;
            case INT_LITERAL:
            case LONG_LITERAL:
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
            case STRING_LITERAL:
                // Avoid issuing warnings for: @GuardedBy(<something>) Object o; o = <some literal>;
                // Do NOT do this if the LHS is @GuardedByBottom.
                if (!varType.hasAnnotation(GuardedByBottom.class)) {
                    return;
                }
                break;
            default:
        }

        // In cases where assigning a value with a @GuardedBy annotation to a variable with a
        // @GuardSatisfied annotation is legal, this is our last chance to check that the
        // appropriate locks are held before the information in the @GuardedBy annotation is
        // lost in the assignment to the variable annotated with @GuardSatisfied. See the
        // discussion of @GuardSatisfied in the "Type-checking rules" section of the
        // Lock Checker manual chapter for more details.

        if (varType.hasAnnotation(GuardSatisfied.class)) {
            if (valueType.hasAnnotation(GuardedBy.class)) {
                checkPreconditions((ExpressionTree) valueTree,
                        generatePreconditionsBasedOnGuards(valueType));

                return;
            } else if (valueType.hasAnnotation(GuardSatisfied.class)) {
                // TODO: Find a cleaner, non-abstraction-breaking way to know whether method actual parameters are being assigned to formal parameters.

                if (!errorKey.equals("argument.type.incompatible")) {
                    // If both @GuardSatisfied have no index, the assignment is not allowed because the LHS and RHS expressions
                    // may be guarded by different lock expressions.  The assignment is allowed when matching a formal
                    // parameter to an actual parameter (see the if block above).

                    int varTypeGuardSatisfiedIndex = atypeFactory.getGuardSatisfiedIndex(varType);
                    int valueTypeGuardSatisfiedIndex = atypeFactory.getGuardSatisfiedIndex(valueType);

                    if (varTypeGuardSatisfiedIndex == -1 && valueTypeGuardSatisfiedIndex == -1) {
                        checker.report(Result.failure(
                                "guardsatisfied.assignment.disallowed",
                                varType, valueType), valueTree);
                    }
                } else {
                    // The RHS can be @GuardSatisfied with a different index when matching method formal parameters to actual parameters.
                    // The actual matching is done in LockVisitor.visitMethodInvocation and a guardsatisfied.parameters.must.match error
                    // is issued if the parameters do not match exactly.
                    // Do nothing here, since there is no precondition to be checked on a @GuardSatisfied parameter.
                    // Note: this matching of a @GS(index) to a @GS(differentIndex) is *only* allowed when matching method formal parameters to actual parameters.

                    return;
                }
            } else if (!atypeFactory.getTypeHierarchy().isSubtype(valueType, varType)) {
                // Special case: replace the @GuardSatisfied primary annotation on the LHS with @GuardedBy({}) and see if it type checks.

                AnnotatedTypeMirror varType2 = varType.deepCopy(); // TODO: Would shallowCopy be sufficient?
                varType2.replaceAnnotation(atypeFactory.GUARDEDBY);
                if (atypeFactory.getTypeHierarchy().isSubtype(valueType, varType2)) {
                    return;
                }
            }
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, Void p) {
        if (atypeFactory.getNodeForTree(tree) instanceof FieldAccessNode) {
            Tree treeOfExpression = tree.getExpression();
            Node nodeOfExpression = atypeFactory.getNodeForTree(treeOfExpression);
            checkFieldOrArrayAccess(tree, treeOfExpression, nodeOfExpression);
        }

        return super.visitMemberSelect(tree, p);
    }

    private void reportFailure(/*@CompilerMessageKey*/ String messageKey,
            MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            List<String> overriderLocks,
            List<String> overriddenLocks
            ) {
        // Get the type of the overriding method.
        AnnotatedExecutableType overrider =
            atypeFactory.getAnnotatedType(overriderTree);

        if (overrider.getTypeVariables().isEmpty()
                && !overridden.getTypeVariables().isEmpty()) {
            overridden = overridden.getErased();
        }
        String overriderMeth = overrider.toString();
        String overriderTyp = enclosingType.getUnderlyingType().asElement().toString();
        String overriddenMeth = overridden.toString();
        String overriddenTyp = overriddenType.getUnderlyingType().asElement().toString();

        if (overriderLocks == null || overriddenLocks == null) {
            checker.report(Result.failure(messageKey,
                    overriderMeth, overriderTyp,
                    overriddenMeth, overriddenTyp), overriderTree);
        } else {
            checker.report(Result.failure(messageKey,
                    overriderMeth, overriderTyp,
                    overriddenMeth, overriddenTyp,
                    overriderLocks, overriddenLocks), overriderTree);
        }
    }

    /**
     *  Ensures that subclass methods are annotated with a stronger or equally strong side effect annotation
     *  than the parent class method.
     */
    @Override
    protected boolean checkOverride(MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            Void p) {

        boolean isValid = true;

        SideEffectAnnotation seaOfOverriderMethod = atypeFactory.methodSideEffectAnnotation(TreeUtils.elementFromDeclaration(overriderTree), false);
        SideEffectAnnotation seaOfOverridenMethod = atypeFactory.methodSideEffectAnnotation(overridden.getElement(), false);

        if (seaOfOverriderMethod.isWeakerThan(seaOfOverridenMethod)) {
            isValid = false;
            reportFailure("override.sideeffect.invalid", overriderTree, enclosingType, overridden, overriddenType, null, null);
        }

        return super.checkOverride(overriderTree, enclosingType, overridden, overriddenType, p) && isValid;
    }



    /**
     * Checks that the field or array access is legal by checking that the locks
     * in the access's expression are held.
     *
     * @param accessTree field or array access tree to check (may be an identifier tree of a field)
     * @param treeToReportErrorAt tree whose location is used to report the error
     * @param expressionNode node of the field or array access's expression
     */
    private void checkFieldOrArrayAccess(ExpressionTree accessTree, Tree treeToReportErrorAt, Node expressionNode) {
        AnnotatedTypeMirror atmOfReceiver = atypeFactory.getReceiverType(accessTree);
        if (treeToReportErrorAt != null && atmOfReceiver != null) {
            AnnotationMirror gb = atmOfReceiver.getEffectiveAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
            if (gb == null) {
                ErrorReporter.errorAbort("LockVisitor.checkFieldOrArrayAccess: gb cannot be null");
            }

            if (AnnotationUtils.areSameByClass(gb, checkerGuardedByClass)) {
                Set<Pair<String, String>> preconditions = generatePreconditionsBasedOnGuards(atmOfReceiver);
                checkPreconditions(treeToReportErrorAt, expressionNode, preconditions);
            } else if (AnnotationUtils.areSameByClass(gb, checkerGuardSatisfiedClass)) {
                // Can always dereference if type is @GuardSatisfied
            } else {
                // Can never dereference for any other types in the @GuardedBy hierarchy
                checker.report(Result.failure(
                        "cannot.dereference",
                        accessTree.toString(),
                        AnnotationUtils.annotationSimpleName(gb)),accessTree);
            }
        }
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void p) {
        Tree treeOfExpression = tree.getExpression();
        Node nodeOfExpression = atypeFactory.getNodeForTree(treeOfExpression);
        checkFieldOrArrayAccess(tree, treeOfExpression, nodeOfExpression);
        return super.visitArrayAccess(tree, p);
    }

    /**
     * Skips the call to super and returns true.
     * <p>
     *
     * {@code GuardedBy({})} is the default type on class declarations, which is a subtype of the top annotation {@code @GuardedByUnknown}.
     * However, it is valid to declare an instance of a class with any annotation from the {@code @GuardedBy} hierarchy.
     * Hence, this method returns true for annotations in the {@code @GuardedBy} hierarchy.
     * <p>
     *
     * Also returns true for annotations in the {@code @LockPossiblyHeld} hierarchy since the default for that hierarchy is the top type and
     * annotations from that hierarchy cannot be explicitly written in code.
     */
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {
        return true;
    }

    /**
     * When visiting a method invocation, issue an error if the side effect annotation
     * on the called method causes the side effect guarantee of the enclosing method
     * to be violated. For example, a method annotated with @ReleasesNoLocks may not
     * call a method annotated with @MayReleaseLocks.
     * Also check that matching @GuardSatisfied(index) on a method's formal receiver/parameters matches
     * those in corresponding locations on the method call site.
     *
     * @param node the MethodInvocationTree of the method call being visited
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        ExecutableElement methodElement = TreeUtils.elementFromUse(node);

        SideEffectAnnotation seaOfInvokedMethod = atypeFactory.methodSideEffectAnnotation(methodElement, false);

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(node));

        ExecutableElement enclosingMethodElement = null;
        if (enclosingMethod != null) {
            enclosingMethodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
        }

        if (enclosingMethodElement != null) {
            SideEffectAnnotation seaOfContainingMethod = atypeFactory.methodSideEffectAnnotation(enclosingMethodElement, false);

            if (seaOfInvokedMethod.isWeakerThan(seaOfContainingMethod)) {
                checker.report(Result.failure(
                        "method.guarantee.violated",
                        seaOfContainingMethod.getNameOfSideEffectAnnotation(),
                        enclosingMethodElement.toString(),
                        methodElement.toString(),
                        seaOfInvokedMethod.getNameOfSideEffectAnnotation()), node);
            }
        }

        if (methodElement != null) {
            // Handle releasing of explicit locks. Verify that the lock expression is effectively final.
            ExpressionTree recvTree = TreeUtils.getReceiverTree(node);

            ensureReceiverOfExplicitUnlockCallIsEffectivelyFinal(node, methodElement, recvTree);

            // Handle acquiring of explicit locks. Verify that the lock expression is effectively final.

            // If the method causes expression "this" or "#1" to be locked, verify that those expressions are effectively final.
            // TODO: generalize to any expression. This is currently designed only to support methods in ReentrantLock
            // and ReentrantReadWriteLock (which use the "this" expression), as well as Thread.holdsLock (which uses
            // the "#1" expression).

            AnnotationMirror ensuresLockHeldAnno = atypeFactory.getDeclAnnotation(methodElement, EnsuresLockHeld.class);
            List<String> expressions = new ArrayList<String>();

            if (ensuresLockHeldAnno != null) {
                expressions.addAll(AnnotationUtils.getElementValueArray(ensuresLockHeldAnno, "value", String.class, false));
            }

            AnnotationMirror ensuresLockHeldIfAnno = atypeFactory.getDeclAnnotation(methodElement, EnsuresLockHeldIf.class);

            if (ensuresLockHeldIfAnno != null) {
                expressions.addAll(AnnotationUtils.getElementValueArray(ensuresLockHeldIfAnno, "expression", String.class, false));
            }

            for (String expr : expressions) {
                if (expr.equals("this")) {
                    // recvTree will be null for implicit this, or class name receivers. But they are also final. So nothing to be checked for them.
                    if (recvTree != null) {
                        ensureExpressionIsEffectivelyFinal(recvTree);
                    }
                } else if (expr.equals("#1")) {
                    ExpressionTree firstParameter = node.getArguments().get(0);
                    if (firstParameter != null) {
                        ensureExpressionIsEffectivelyFinal(firstParameter);
                    }
                }

            }
        }

        // Check that matching @GuardSatisfied(index) on a method's formal receiver/parameters matches
        // those in corresponding locations on the method call site.

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.first;

        List<AnnotatedTypeMirror> requiredArgs =
            AnnotatedTypes.expandVarArgs(atypeFactory, invokedMethod, node.getArguments());

        // Index on @GuardSatisfied at each location. -1 when no @GuardSatisfied annotation was present.
        // Note that @GuardSatisfied with no index is normally represented as having index -1.
        // We would like to ignore a @GuardSatisfied with no index for these purposes, so if it is encountered we leave its index as -1.
        // The first element of the array is reserved for the receiver.
        int guardSatisfiedIndex[] = new int[requiredArgs.size() + 1]; // + 1 for the receiver parameter type

        // Retrieve receiver types from method definition and method call

        guardSatisfiedIndex[0] = -1;

        AnnotatedTypeMirror methodDefinitionReceiver = null;
        AnnotatedTypeMirror methodCallReceiver = null;

        ExecutableElement invokedMethodElement = invokedMethod.getElement();
        if (!ElementUtils.isStatic(invokedMethodElement) &&
            invokedMethod.getElement().getKind() != ElementKind.CONSTRUCTOR) {
            methodDefinitionReceiver = invokedMethod.getReceiverType();
            if (methodDefinitionReceiver != null && methodDefinitionReceiver.hasAnnotation(checkerGuardSatisfiedClass)) {
                guardSatisfiedIndex[0] = atypeFactory.getGuardSatisfiedIndex(methodDefinitionReceiver);
                methodCallReceiver = atypeFactory.getReceiverType(node);
            }
        }

        // Retrieve formal parameter types from the method definition

        for (int i = 0; i < requiredArgs.size(); i++) {
            guardSatisfiedIndex[i+1] = -1;

            AnnotatedTypeMirror arg = requiredArgs.get(i);

            if (arg.hasAnnotation(checkerGuardSatisfiedClass)) {
                guardSatisfiedIndex[i+1] = atypeFactory.getGuardSatisfiedIndex(arg);
            }
        }

        // Combine all of the actual parameters into one list of AnnotationMirrors

        ArrayList<AnnotationMirror> passedArgAnnotations = new ArrayList<AnnotationMirror>(guardSatisfiedIndex.length);
        passedArgAnnotations.add(methodCallReceiver == null ? null : methodCallReceiver.getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN));
        for (ExpressionTree tree : node.getArguments()) {
            passedArgAnnotations.add(atypeFactory.getAnnotatedType(tree).getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN));
        }

        // Perform the validity check and issue an error if not valid.

        for (int i = 0; i < guardSatisfiedIndex.length; i++) {
            if (guardSatisfiedIndex[i] != -1) {
                for (int j = i + 1; j < guardSatisfiedIndex.length; j++) {
                    if (guardSatisfiedIndex[i] == guardSatisfiedIndex[j]) {
                        // The @GuardedBy/@GuardSatisfied/@GuardedByUnknown/@GuardedByBottom annotations
                        // must be identical on the corresponding actual parameters.
                        AnnotationMirror arg1Anno = passedArgAnnotations.get(i);
                        AnnotationMirror arg2Anno = passedArgAnnotations.get(j);
                        if (arg1Anno != null && arg2Anno != null) {
                            boolean bothAreGSwithNoIndex = false;

                            if (AnnotationUtils.areSameByClass(arg1Anno, checkerGuardSatisfiedClass) &&
                                AnnotationUtils.areSameByClass(arg2Anno, checkerGuardSatisfiedClass)) {
                                if (atypeFactory.getGuardSatisfiedIndex(arg1Anno) == -1 &&
                                    atypeFactory.getGuardSatisfiedIndex(arg2Anno) == -1) {
                                    // Generally speaking, two @GuardSatisfied annotations with no index are incomparable.
                                    // TODO: If they come from the same variable, they are comparable.  Fix and add a test case.
                                    bothAreGSwithNoIndex = true;
                                }
                            }

                            if (bothAreGSwithNoIndex ||
                              !(atypeFactory.getQualifierHierarchy().isSubtype(arg1Anno, arg2Anno) ||
                                atypeFactory.getQualifierHierarchy().isSubtype(arg2Anno, arg1Anno))) {
                                // TODO: allow these strings to be localized

                                String formalParam1 = null;

                                if (i == 0) {
                                    formalParam1 = "The receiver type";
                                } else {
                                    formalParam1 = "Parameter #" + i; // i, not i-1, so the index is 1-based
                                }

                                String formalParam2 = "parameter #" + j; // j, not j-1, so the index is 1-based

                                checker.report(Result.failure(
                                        "guardsatisfied.parameters.must.match",
                                        formalParam1, formalParam2, invokedMethod.toString(), guardSatisfiedIndex[i], arg1Anno, arg2Anno), node);
                            }
                        }
                    }
                }
            }
        }

        return super.visitMethodInvocation(node, p);
    }

    /**
     * Issues an error if the receiver of an unlock() call is not effectively final.
     *
     * @param node the MethodInvocationTree for any method call
     * @param methodElement the ExecutableElement for the method call referred to by {@code node}
     * @param lockExpression the receiver tree of {@code node}. Can be null.
     */
    private void ensureReceiverOfExplicitUnlockCallIsEffectivelyFinal(MethodInvocationTree node, ExecutableElement methodElement,
            ExpressionTree lockExpression) {
        if (lockExpression == null) {
            // Implicit this, or class name receivers, are null. But they are also final. So nothing to be checked for them.
            return;
        }

        if (!methodElement.getSimpleName().contentEquals("unlock")) {
            return;
        }

        TypeMirror lockExpressionType = InternalUtils.typeOf(lockExpression);

        ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();

        javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

        // TODO: make a type declaration annotation for this rather than looking for the Lock.unlock() method explicitly.
        TypeMirror lockInterfaceTypeMirror = TypesUtils.typeFromClass(types, processingEnvironment.getElementUtils(), Lock.class);

        if (types.isSubtype(types.erasure(lockExpressionType), lockInterfaceTypeMirror)) {
            ensureExpressionIsEffectivelyFinal(lockExpression);
        }
    }

    /**
     * When visiting a synchronized block, issue an error if the expression
     * has a type that implements the java.util.concurrent.locks.Lock interface.
     * This prevents explicit locks from being accidentally used as built-in (monitor) locks.
     * This is important because the Lock Checker does not have a mechanism to separately
     * keep track of the explicit lock and the monitor lock of an expression that implements
     * the Lock interface (i.e. there is a @LockHeld annotation used in dataflow, but there are
     * not distinct @MonitorLockHeld and @ExplicitLockHeld annotations). It is assumed that
     * both kinds of locks will never be held for any expression that implements Lock.
     *
     * Additionally, a synchronized block may not be present in a method that has a @LockingFree
     * guarantee or stronger. An error is issued in this case.
     *
     * @param node the SynchronizedTree for the synchronized block being visited
     */
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();

        javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

        // TODO: make a type declaration annotation for this rather than looking for Lock.class explicitly.
        TypeMirror lockInterfaceTypeMirror = TypesUtils.typeFromClass(types, processingEnvironment.getElementUtils(), Lock.class);

        ExpressionTree synchronizedExpression = node.getExpression();

        ensureExpressionIsEffectivelyFinal(synchronizedExpression);

        TypeMirror expressionType = types.erasure(atypeFactory.getAnnotatedType(synchronizedExpression).getUnderlyingType());

        if (types.isSubtype(expressionType, lockInterfaceTypeMirror)) {
            checker.report(Result.failure(
                    "explicit.lock.synchronized"), node);
        }

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(node));

        ExecutableElement methodElement = null;
        if (enclosingMethod != null) {
            methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);

            SideEffectAnnotation seaOfContainingMethod = atypeFactory.methodSideEffectAnnotation(methodElement, false);

            if (!seaOfContainingMethod.isWeakerThan(SideEffectAnnotation.LOCKINGFREE)) {
                checker.report(Result.failure("synchronized.block.in.lockingfree.method", seaOfContainingMethod), node);
            }
        }

        return super.visitSynchronized(node, p);
    }

    /**
     * Ensures that each variable accessed in an expression is final or effectively final and
     * that each called method in the expression is @Deterministic.
     * Issues an error otherwise. Recursively performs this check on method arguments.
     * Only intended to be used on the expression of a synchronized block.
     *
     * Example: given the expression var1.field1.method1(var2.method2()).field2,
     * var1, var2, field1 and field2 are enforced to be final or effectively final, and
     * method1 and method2 are enforced to be @Deterministic.
     *
     * @param lockExpressionTree the expression tree of a synchronized block
     */
    private void ensureExpressionIsEffectivelyFinal(final ExpressionTree lockExpressionTree) {
        // This functionality could be implemented using a visitor instead,
        // however with this design, it is easier to be certain that an error
        // will always be issued if a tree kind is not recognized.
        // Only the most common tree kinds for synchronized expressions are supported.

        // Traverse the expression using 'tree', as 'lockExpressionTree' is used for error reporting.
        ExpressionTree tree = lockExpressionTree;

        while (true) {
            tree = TreeUtils.skipParens(tree);

            switch(tree.getKind()) {
                case MEMBER_SELECT:
                    if (!isTreeSymbolEffectivelyFinalOrUnmodifiable(tree)) {
                        checker.report(Result.failure("lock.expression.not.final", lockExpressionTree), tree);
                        return;
                    }
                    tree = ((MemberSelectTree) tree).getExpression();
                    break;
                case IDENTIFIER:
                    if (!isTreeSymbolEffectivelyFinalOrUnmodifiable(tree)) {
                        checker.report(Result.failure("lock.expression.not.final", lockExpressionTree), tree);
                    }
                    return;
                case METHOD_INVOCATION:
                    Element elem = TreeUtils.elementFromUse(tree);
                    if (atypeFactory.getDeclAnnotationNoAliases(elem, Deterministic.class) == null &&
                        atypeFactory.getDeclAnnotationNoAliases(elem, Pure.class) == null) {
                        checker.report(Result.failure("lock.expression.not.final", lockExpressionTree), tree);
                        return;
                    }

                    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;

                    for (ExpressionTree argTree : methodInvocationTree.getArguments()) {
                        ensureExpressionIsEffectivelyFinal(argTree);
                    }

                    tree = methodInvocationTree.getMethodSelect();
                    break;
                default:
                    checker.report(Result.failure("lock.expression.possibly.not.final", lockExpressionTree), tree);
                    return;
            }
        }
    }

    private void ensureExpressionIsEffectivelyFinal(final Receiver lockExpr, String expressionForErrorReporting, Tree treeForErrorReporting) {
        // Keep the 'lockExpr' parameter intact for debugging purposes, and traverse the overall expression using 'expr' instead.
        Receiver expr = lockExpr;

        while (true) {
            if (expr instanceof FieldAccess) {
                FieldAccess fieldAccess = (FieldAccess) expr;
                Receiver recv = fieldAccess.getReceiver();

                // Do NOT call fieldAccess.isUnmodifiableByOtherCode if the receiver is a method call, since it also checks if the receiver
                // is unmodifiable and does so incorrectly in that case. The present
                // method will determine whether or not a method call receiver is effectively final
                // (see the "if (expr instanceof MethodCall)" block below).
                if (!(fieldAccess.isUnmodifiableByOtherCode() ||
                     (fieldAccess.isFinal() && recv instanceof MethodCall))) {
                    checker.report(Result.failure("lock.expression.not.final", expressionForErrorReporting), treeForErrorReporting);
                    return;
                }
                expr = recv;
            } else if (expr instanceof LocalVariable) {
                if (!ElementUtils.isEffectivelyFinal(((LocalVariable) expr).getElement())) {
                    checker.report(Result.failure("lock.expression.not.final", expressionForErrorReporting), treeForErrorReporting);
                }
                return;
            } else if (expr instanceof MethodCall) {
                MethodCall methodCall = (MethodCall) expr;
                for (Receiver param : methodCall.getParameters()) {
                    ensureExpressionIsEffectivelyFinal(param, expressionForErrorReporting, treeForErrorReporting);
                }
                if (!PurityUtils.isDeterministic(atypeFactory, methodCall.getElement())) {
                    checker.report(Result.failure("lock.expression.not.final", expressionForErrorReporting), treeForErrorReporting);
                    return;
                }
                expr = methodCall.getReceiver();
            } else if (expr instanceof ThisReference || // The current object is always final.
                       expr instanceof ClassName) { // Class names are always final.
                // Neither ThisReference nor ClassName instances have a receiver,
                // so exit the loop.
                return;
            } else { // type of 'expr' is not supported in @GuardedBy(...) lock expressions
                checker.report(Result.failure("lock.expression.possibly.not.final", expressionForErrorReporting), treeForErrorReporting);
                return;
            }
        }
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {
        ArrayList<AnnotationTree> annotationTreeList = new ArrayList<AnnotationTree>(1);
        annotationTreeList.add(tree);
        List<AnnotationMirror> amList = InternalUtils.annotationsFromTypeAnnotationTrees(annotationTreeList);

        if (amList != null) {
            for (AnnotationMirror annotationMirror : amList) {
                if (AnnotationUtils.areSameByClass(annotationMirror, checkerGuardedByClass)) {
                    checkLockExpressionInGuardedByAnnotation(tree, annotationMirror);
                } else if (AnnotationUtils.areSameByClass(annotationMirror, checkerGuardSatisfiedClass)) {
                    issueErrorIfGuardSatisfiedAnnotationInUnsupportedLocation(tree);
                }
            }
        }

        return super.visitAnnotation(tree, p);
    }

    /**
     * Check that the lock expression in a GuardedBy annotation is a valid flow expression
     * and is effectively final
     * @param tree AnnotationTree used for context and error reporting
     * @param guardedByAnnotation GuardedBy AnnotationMirror
     */
    private void checkLockExpressionInGuardedByAnnotation(AnnotationTree tree, AnnotationMirror guardedByAnnotation) {
            List<String> guardedByValue = AnnotationUtils.getElementValueArray(guardedByAnnotation, "value", String.class, true);
        if (guardedByValue.isEmpty()) {
            // getting the FlowExpressionContext could be costly,
            // so don't do it if there isn't a lock expression to check
            return;
        }

        TreePath path = getCurrentPath();
        MethodTree enclMethod = TreeUtils.enclosingMethod(path);
        FlowExpressionContext flowExprContext;
        if (enclMethod != null) {
            flowExprContext = FlowExpressionParseUtil.buildFlowExprContextForDeclaration(enclMethod, path, checker.getContext());
        } else {
            ClassTree enclosingClass = TreeUtils.enclosingClass(path);
            flowExprContext = FlowExpressionParseUtil.buildFlowExprContextForDeclaration(enclosingClass, path, checker.getContext());
        }

        // Adapted from BaseTypeVisitor.checkPreconditions

        if (flowExprContext == null) {
            // The expressions cannot be parsed. Issue an error for the whole list of @GuardedBy expressions.
            checker.report(Result.failure("lock.expression.possibly.not.final", guardedByValue), tree);
            return;
        }

        TreePath pathForLocalVariableRetrieval = getPathForLocalVariableRetrieval(path);

        if (pathForLocalVariableRetrieval == null) {
            // The expressions cannot be parsed. Issue an error for the whole list of @GuardedBy expressions.
            checker.report(Result.failure("lock.expression.possibly.not.final", guardedByValue), tree);
            return;
        }

        for (String lockExpression : guardedByValue) {
            try {
                // Attempt to parse the lock expression.
                // This will also issue errors if the lock expressions are not final
                parseExpressionString(lockExpression, flowExprContext,
                                      pathForLocalVariableRetrieval, null, tree);
            } catch (FlowExpressionParseException e) {
                checker.report(e.getResult(), tree);
            }
        }
    }

    /**
     * Issues an error if a GuardSatisfied annotation is found in a location other than a method return type, receiver or parameter.
     * @param annotationTree AnnotationTree used for error reporting and to help determine that an array parameter has no GuardSatisfied
     * annotations except on the array type
     */
    // TODO: Remove this method once @TargetLocations are enforced (i.e. once
    // issue https://github.com/typetools/checker-framework/issues/515 is closed).
    private void issueErrorIfGuardSatisfiedAnnotationInUnsupportedLocation(AnnotationTree annotationTree) {
        TreePath currentPath = getCurrentPath();
        TreePath path = getPathForLocalVariableRetrieval(currentPath);
        if (path != null) {
            Tree tree = path.getLeaf();
            Tree.Kind kind = tree.getKind();

            if (kind == Tree.Kind.METHOD) {
                // The @GuardSatisfied annotation is on the return type.
                return;
            } else if (kind == Tree.Kind.VARIABLE) {
                VariableTree varTree = (VariableTree) tree;
                Tree varTypeTree = varTree.getType();
                if (varTypeTree != null) {
                    TreePath parentPath = path.getParentPath();
                    if (parentPath != null && parentPath.getLeaf().getKind() == Tree.Kind.METHOD) {
                        Tree.Kind varTypeTreeKind = varTypeTree.getKind();
                        if (varTypeTreeKind == Tree.Kind.ANNOTATED_TYPE) {
                            AnnotatedTypeTree annotatedTypeTree = (AnnotatedTypeTree) varTypeTree;

                            if (annotatedTypeTree.getUnderlyingType().getKind() != Tree.Kind.ARRAY_TYPE ||
                                annotatedTypeTree.getAnnotations().contains(annotationTree)) {
                                // Method parameter
                                return;
                            }
                        } else if (varTypeTreeKind != Tree.Kind.ARRAY_TYPE) {
                            // Method parameter or receiver
                            return;
                        }
                    }
                }
            }
        }

        checker.report(Result.failure("guardsatisfied.location.disallowed"), annotationTree);
    }

    /**
     * The flow expression parser requires a path for retrieving the scope that will be used
     * to resolve local variables. One would expect that simply providing the
     * path to an AnnotationTree would work, since the compiler (as called by the
     * org.checkerframework.javacutil.Resolver class) could walk up the path from the AnnotationTree
     * to determine the scope. Unfortunately this is not how the compiler works. One must provide
     * the path at the right level (not so deep that it results in a symbol not being found, but not so high up
     * that it is out of the scope at hand). This is a problem when trying to retrieve local
     * variables, since one could silently miss a local variable in scope and accidentally retrieve
     * a field with the same name. This method returns the correct path for this purpose,
     * given a path to an AnnotationTree.
     *
     * Note: this is definitely necessary for local variable retrieval. It has not been tested whether
     * this is strictly necessary for fields or other identifiers.
     *
     * Only call this method from visitAnnotation.
     *
     * @param path the TreePath whose leaf is an AnnotationTree
     * @return a TreePath that can be passed to methods in the Resolver class to locate local variables
     */
    private TreePath getPathForLocalVariableRetrieval(TreePath path) {
        assert path.getLeaf() instanceof AnnotationTree;

        // TODO: handle annotations in trees of kind NEW_CLASS (and add test coverage for this scenario).
        // Currently an annotation in such a tree, such as "new @GuardedBy("foo") Object()",
        // results in a constructor.invocation.invalid error. This must be fixed first.

        path = path.getParentPath();

        if (path == null) {
            return null;
        }

        // A MODIFIERS tree for a VARIABLE or METHOD parent tree would be available at this level,
        // but it is not directly handled. Instead, its parent tree (one level higher) is handled.
        // Other tree kinds are also handled one level higher.

        path = path.getParentPath();

        if (path == null) {
            return null;
        }

        Tree tree = path.getLeaf();
        Tree.Kind kind = tree.getKind();

        switch(kind) {
            case ARRAY_TYPE:
            case VARIABLE:
            case TYPE_CAST:
            case INSTANCE_OF:
            case METHOD:
            case NEW_ARRAY:
            case TYPE_PARAMETER:
            // TODO: visitAnnotation does not currently visit annotations on wildcard bounds.
            // Address this for the Lock Checker somehow and enable these, as well as the corresponding test cases in ChapterExamples.java
            // case EXTENDS_WILDCARD:
            // case SUPER_WILDCARD:
                return path;
            default:
                return null;
        }
    }

    /**
     * Returns true if the symbol for the given tree is final or effectively final.
     * Package, class and method symbols are unmodifiable and therefore considered final.
     */
    private boolean isTreeSymbolEffectivelyFinalOrUnmodifiable(Tree tree) {
        Element elem = InternalUtils.symbol(tree);
        ElementKind ek = elem.getKind();
        return ek == ElementKind.PACKAGE ||
               ek == ElementKind.CLASS ||
               ek == ElementKind.METHOD ||
               ElementUtils.isEffectivelyFinal(elem);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void p) {
        Node node = atypeFactory.getNodeForTree(tree);
        if (node instanceof FieldAccessNode) {
            Node receiverNode = ((FieldAccessNode) node).getReceiver();
            if (receiverNode instanceof ImplicitThisLiteralNode) {
                // All other field access are handle via visitMemberSelect
                checkFieldOrArrayAccess(tree, tree, receiverNode);
            }
        }
        return super.visitIdentifier(tree, p);
    }

    /**
     * If expression is "itself", and the flow expression parser cannot find a variable,
     * class, etc. named "itself", a flow expression receiver for {@code node} is returned,
     * unless {@code node} is null, in which case null is returned.
     * Also checks that the flow expression is effectively final and issues an error if it is not.
     * <p>
     * Returns the result of the super implementation otherwise.
     */
    @Override
    protected FlowExpressions.Receiver parseExpressionString(String expression,
            FlowExpressionContext flowExprContext,
            TreePath path,
            Node node, Tree treeForErrorReporting) throws FlowExpressionParseException {
        FlowExpressions.Receiver expr = null;
        expression = expression.trim();

        Matcher itselfReceiverMatcher = itselfReceiverPattern.matcher(expression);

        if (itselfReceiverMatcher.matches()) {
            expr = FlowExpressionParseUtil.parseAllowingItself(expression, flowExprContext, path);

            if (expr == null) {
                // No variable, class, etc. named "itself(.*)" could be found.
                // Hence "itself" is interpreted to actually mean itself.

                if (node == null) {
                    // node is definitely null if this method was called by LockVisitor.visitAnnotation.
                    // In this case, we skip the check to ensure that the "itself" expression is
                    // effectively final at the site of the @GuardedBy("itself") annotation.

                    return null;
                }

                String remainingExpression = itselfReceiverMatcher.group(2);

                if (remainingExpression == null || remainingExpression.isEmpty()) {
                    expr = FlowExpressions.internalReprOf(atypeFactory,
                            node);
                } else {
                    // TODO: The proper way to do this is to call flowExprContext.changeReceiver to set the
                    // receiver to the itself expression, and then call FlowExpressionParseUtil.parse on the
                    // remaining expression string with the new flow expression context. However, this currently
                    // results in a FlowExpressions.Receiver that has a different hash code than if
                    // the following flow expression is parsed directly, which results in our inability
                    // to check that a lock expression is held as it does not match anything in the store
                    // due to the hash code mismatch.
                    // For now, convert the "itself" portion to the node's string representation, and parse
                    // the entire string:

                    expr = FlowExpressionParseUtil.parse(node.toString() + "." + remainingExpression, flowExprContext, path);
                }
            }
        } else {
            expr = super.parseExpressionString(expression, flowExprContext, path, node, treeForErrorReporting);
        }

        ensureExpressionIsEffectivelyFinal(expr, expression, treeForErrorReporting);

        return expr;
    }

    /**
     * Disallows annotations from the @GuardedBy hierarchy on class declarations (other than @GuardedBy({}).
     */
    @Override
    public Void visitClass(ClassTree node, Void p) {
        List<AnnotationMirror> annos = InternalUtils.annotationsFromTypeAnnotationTrees(node.getModifiers().getAnnotations());

        for (AnnotationMirror anno : annos) {
            if (!AnnotationUtils.areSame(anno, atypeFactory.GUARDEDBY) &&
                (AnnotationUtils.areSameIgnoringValues(anno, atypeFactory.GUARDEDBYUNKNOWN) ||
                 AnnotationUtils.areSameIgnoringValues(anno, atypeFactory.GUARDEDBY) ||
                 AnnotationUtils.areSameIgnoringValues(anno, atypeFactory.GUARDSATISFIED) ||
                 AnnotationUtils.areSameIgnoringValues(anno, atypeFactory.GUARDEDBYBOTTOM))) {
                checker.report(Result.failure("class.declaration.guardedby.annotation.invalid"), node);
            }
        }

        return super.visitClass(node, p);
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        if (node.getKind() == Tree.Kind.PLUS) {
            Tree leftTree = node.getLeftOperand();
            Tree rightTree = node.getRightOperand();

            boolean lhsIsString = TypesUtils.isString(InternalUtils.typeOf(leftTree));
            boolean rhsIsString = TypesUtils.isString(InternalUtils.typeOf(rightTree));
            if (!lhsIsString && rhsIsString) {
                checkPreconditionsForImplicitToStringCall(leftTree);
            } else if (lhsIsString && !rhsIsString) {
                checkPreconditionsForImplicitToStringCall(rightTree);
            }
        }

        return super.visitBinary(node, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        if (node.getKind() == Tree.Kind.PLUS_ASSIGNMENT) {
            ExpressionTree rightTree = node.getExpression();

            if (TypesUtils.isString(InternalUtils.typeOf(node.getVariable())) &&
                !TypesUtils.isString(InternalUtils.typeOf(rightTree))) {
                checkPreconditionsForImplicitToStringCall(rightTree);
            }
        }

        return super.visitCompoundAssignment(node, p);
    }

    /**
     * Checks precondition for {@code tree} that is known to be the receiver of an implicit toString() call.
     * The receiver of toString() is defined in the annotated JDK to be @GuardSatisfied.
     * Therefore if the expression is guarded by a set of locks, the locks must be held prior
     * to this implicit call to toString().
     *
     * Only call this method from visitBinary and visitCompoundAssignment.
     *
     * @param tree the Tree corresponding to the expression that is known to be the receiver
     * of an implicit toString() call
     */
    // TODO: If and when the de-sugared .toString() tree is accessible from BaseTypeVisitor,
    // the toString() method call should be visited instead of doing this. This would result
    // in contracts.precondition.not.satisfied errors being issued instead of
    // contracts.precondition.not.satisfied.field, so it would be clear that
    // the error refers to an implicit method call, not a dereference (field access).
    private void checkPreconditionsForImplicitToStringCall(Tree tree) {
        checkPreconditions(tree,
                generatePreconditionsBasedOnGuards(atypeFactory.getAnnotatedType(tree)));
    }
}
