package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;

/**
 * The LockVisitor enforces the special type-checking rules described in the Lock Checker manual chapter.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */

// TODO: Enforce that lock expressions are final or effectively final.

public class LockVisitor extends BaseTypeVisitor<LockAnnotatedTypeFactory> {
    private final Class<? extends Annotation> checkerGuardedByClass = GuardedBy.class;
    private final Class<? extends Annotation> checkerGuardSatisfiedClass = GuardSatisfied.class;

    /** Annotation constants */
    protected final AnnotationMirror GUARDEDBY, GUARDEDBYUNKNOWN, GUARDSATISFIED, GUARDEDBYBOTTOM;

    public LockVisitor(BaseTypeChecker checker) {
        super(checker);

        GUARDEDBYUNKNOWN = AnnotationUtils.fromClass(elements, GuardedByUnknown.class);
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        GUARDSATISFIED = AnnotationUtils.fromClass(elements, GuardSatisfied.class);
        GUARDEDBYBOTTOM = AnnotationUtils.fromClass(elements, GuardedByBottom.class);

        checkForAnnotatedJdk();
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) { // visit a variable declaration
        // A user may not annotate a primitive type, a boxed primitive type or a String
        // with any qualifier from the @GuardedBy hierarchy.

        AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
        TypeMirror tm = atm.getUnderlyingType();
        if ((TypesUtils.isBoxedPrimitive(tm) ||
             TypesUtils.isPrimitive(tm) ||
             TypesUtils.isString(tm)) &&
            (atm.hasExplicitAnnotationRelaxed(GUARDSATISFIED) ||
             atm.hasExplicitAnnotationRelaxed(GUARDEDBY) ||
             atm.hasExplicitAnnotation(GUARDEDBYUNKNOWN) ||
             atm.hasExplicitAnnotation(GUARDEDBYBOTTOM))){
            checker.report(Result.failure("primitive.type.guardedby"), node);
        }
        return super.visitVariable(node, p);
    }

    @Override
    public LockAnnotatedTypeFactory createTypeFactory() {
        return new LockAnnotatedTypeFactory(checker);
    }

    /***
     * Issues an error if a method (explicitly or implicitly) annotated with @MayReleaseLocks has a formal parameter
     * or receiver (explicitly or implicitly) annotated with @GuardSatisfied. Also issues an error if a synchronized
     * method has a @LockingFree, @SideEffectFree or @Pure annotation.
     *
     * @param node the MethodTree of the method definition to visit.
     */
    @Override
    public Void visitMethod(MethodTree node, Void p) {
        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);

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
                for(VariableTree vt : node.getParameters()) {
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

    /***
     * When visiting a method call, if the receiver formal parameter has type @GuardSatisfied
     * and the receiver actual parameter has type @GuardedBy(...), this method verifies that
     * the guard is satisfied, and it returns true, indicating that the receiver subtype check should be skipped.
     * If the receiver actual parameter has type @GuardSatisfied, this method simply returns true without
     * performing any other actions. The method returns false otherwise.
     *
     * @param node the MethodInvocationTree of the method being called.
     * @param methodDefinitionReceiver the ATM of the formal receiver parameter of the method being called.
     * @param methodCallReceiver the ATM of the receiver argument of the method call.
     * @return whether the caller can skip the receiver subtype check.
     */
    @Override
    protected boolean skipReceiverSubtypeCheck(MethodInvocationTree node,
            AnnotatedTypeMirror methodDefinitionReceiver,
            AnnotatedTypeMirror methodCallReceiver) {

        AnnotationMirror primaryGb = methodCallReceiver.getAnnotationInHierarchy(GUARDEDBYUNKNOWN);
        AnnotationMirror effectiveGb = methodCallReceiver.getEffectiveAnnotationInHierarchy(GUARDEDBYUNKNOWN);

        // If the receiver actual parameter has type @GuardSatisfied, skip the subtype check.
        // Consider only a @GuardSatisfied primary annotation - hence use primaryGb instead of effectiveGb.
        if (primaryGb != null && AnnotationUtils.areSameByClass(primaryGb, checkerGuardSatisfiedClass)){
            AnnotationMirror primaryGbOnMethodDefinition = methodDefinitionReceiver.getAnnotationInHierarchy(GUARDEDBYUNKNOWN);
            if (primaryGbOnMethodDefinition != null && AnnotationUtils.areSameByClass(primaryGbOnMethodDefinition, checkerGuardSatisfiedClass)) {
                return true;
            }
        }

        if (AnnotationUtils.areSameByClass(effectiveGb, checkerGuardedByClass)) {
            Set<AnnotationMirror> annos = methodDefinitionReceiver.getAnnotations();
            for(AnnotationMirror anno : annos) {
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
            if (anno.equals(GUARDEDBYUNKNOWN)) {
                annotationSet.add(GUARDEDBY);
            } else {
                annotationSet.add(anno);
            }
        }
        return annotationSet;
    }

    /***
     * Given an AnnotatedTypeMirror containing a @GuardedBy annotation, returns the set of lock expression preconditions
     * specified in the @GuardedBy annotation.
     * Returns an empty set if no such expressions are found.
     *
     * @param atm the AnnotatedTypeMirror containing the @GuardedBy annotation with the lock expression preconditions.
     * @return a set of lock expression preconditions that can be processed by checkPreconditions.
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

                            preconditions.add(Pair.of(lockExpression, LockHeld.class.toString().substring(10 /* "interface " */)));
                        }
                    }
                }
            }
        }

        return preconditions;
    }

    @Override
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey,
            boolean isLocalVariableAssignment) {

        Kind valueTreeKind = valueTree.getKind();

        switch(valueTreeKind) {
            case NEW_CLASS:
            case NEW_ARRAY:
                // Avoid issuing warnings for: @GuardedBy(<something>) Object o = new Object();
                // Do NOT do this if the LHS is @GuardedByBottom.
                if (!varType.hasAnnotation(GuardedByBottom.class))
                    return;
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
                if (!varType.hasAnnotation(GuardedByBottom.class))
                    return;
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
                varType2.replaceAnnotation(GUARDEDBY);
                if (atypeFactory.getTypeHierarchy().isSubtype(valueType, varType2)) {
                    return;
                }
            }
        } else if (errorKey.equals("compound.assignment.type.incompatible") &&
            TypesUtils.isString(varType.getUnderlyingType()) &&
            valueType.hasAnnotation(GuardSatisfied.class)) {
            // TODO: Find a cleaner, non-abstraction-breaking way to know whether a string compound assignment is being visited,
            // i.e. one that does not check the value of errorKey.

            // This covers the case when the RHS in the string compound assignment
            // has type @GuardSatisfied(...) (the LHS has type @GuardedBy({}) since it is a String).
            // Such a string compound assignment is always legal.
            // This is the case when a @GS parameter is not de-sugared, e.g.:
            //     void StringCompoundAssignment(@GuardSatisfied MyClass param) {
            //         String s = "a";
            //         s += param;
            //     }

            return;
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignment);
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
     * @param treeToReportErrorAt Tree whose location is used to report the error
     * @param expressionNode Node of the field or array access's expression
     */
    private void checkFieldOrArrayAccess(ExpressionTree accessTree, Tree treeToReportErrorAt, Node expressionNode) {
        AnnotatedTypeMirror atmOfReceiver = atypeFactory.getReceiverType(accessTree);
        if (treeToReportErrorAt != null && atmOfReceiver != null) {
            AnnotationMirror gb = atmOfReceiver.getEffectiveAnnotationInHierarchy(GUARDEDBYUNKNOWN);
            if (gb == null) {
                ErrorReporter.errorAbort("LockVisitor.checkFieldOrArrayAccess: gb cannot be null");
            }

            if (AnnotationUtils.areSameByClass(gb, checkerGuardedByClass)) {
                Set<Pair<String, String>> preconditions = generatePreconditionsBasedOnGuards(atmOfReceiver);
                checkPreconditions(treeToReportErrorAt, expressionNode, preconditions);
            } else if (AnnotationUtils.areSameByClass(gb, checkerGuardSatisfiedClass)){
                // Can always dereference if type is @GuardSatisfied
            } else {
                // Can never dereference for any other types in the @GuardedBy hierarchy
                String annotationName = gb.toString();
                annotationName = annotationName.substring(annotationName.lastIndexOf('.') + 1 /* +1 to skip the last . as well */);
                checker.report(Result.failure(
                        "cannot.dereference",
                        accessTree.toString(),
                        "annotation @" + annotationName), accessTree);
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

    /***
     * Skips the call to super and returns true.
     */
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {

        // @GuardedBy({}) is the default type on class declarations, which is a subtype of the top annotation @GuardedByUnknown.
        // However, it is valid to declare an instance of a class with any annotation from the @GuardedBy hierarchy.
        // Hence, this method returns true for annotations in the @GuardedBy hierarchy.

        // Also returns true for annotations in the @LockPossiblyHeld hierarchy since the default for that hierarchy is the top type and
        // annotations from that hierarchy cannot be explicitly written in code.

        return true;
    }

    /***
     * When visiting a method invocation, issue an error if the side effect annotation
     * on the called method causes the side effect guarantee of the enclosing method
     * to be violated. For example, a method annotated with @ReleasesNoLocks may not
     * call a method annotated with @MayReleaseLocks.
     * Also check that matching @GuardSatisfied(index) on a method's formal receiver/parameters matches
     * those in corresponding locations on the method call site.
     *
     * @param node the MethodInvocationTree of the method call being visited.
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        SideEffectAnnotation seaOfInvokedMethod = atypeFactory.methodSideEffectAnnotation(TreeUtils.elementFromUse(node), false);

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(node));

        ExecutableElement methodElement = null;
        if (enclosingMethod != null) {
            methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
        }

        SideEffectAnnotation seaOfContainingMethod = atypeFactory.methodSideEffectAnnotation(methodElement, false);

        if (seaOfInvokedMethod.isWeakerThan(seaOfContainingMethod)) {
            checker.report(Result.failure(
                    "method.guarantee.violated",
                    seaOfContainingMethod.getNameOfSideEffectAnnotation(),
                    methodElement.toString(),
                    TreeUtils.elementFromUse(node).toString(),
                    seaOfInvokedMethod.getNameOfSideEffectAnnotation()), node);
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
        passedArgAnnotations.add(methodCallReceiver == null ? null : methodCallReceiver.getAnnotationInHierarchy(GUARDEDBYUNKNOWN));
        for(ExpressionTree tree : node.getArguments()) {
            passedArgAnnotations.add(atypeFactory.getAnnotatedType(tree).getAnnotationInHierarchy(GUARDEDBYUNKNOWN));
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
                                AnnotationUtils.areSameByClass(arg2Anno, checkerGuardSatisfiedClass)){
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

    /***
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
     * @param node the SynchronizedTree for the synchronized block being visited.
     */
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();

        javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

        // TODO: make a type declaration annotation for this rather than looking for Lock.class explicitly.
        TypeMirror lockInterfaceTypeMirror = TypesUtils.typeFromClass(types, processingEnvironment.getElementUtils(), Lock.class);

        TypeMirror expressionType = types.erasure(atypeFactory.getAnnotatedType(node.getExpression()).getUnderlyingType());

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

    /***
     * If expression is "itself", and the flow expression parser cannot find a variable,
     * class, etc. named "itself", a flow expression receiver for {@code node} is returned.
     * <p>
     * Returns the result of the super implementation otherwise.
     */
    @Override
    protected FlowExpressions.Receiver parseExpressionString(String expression,
            FlowExpressionContext flowExprContext,
            Node node) throws FlowExpressionParseException {
        expression = expression.trim();

        /** Matches 'itself' - it refers to the variable that is annotated, which is different from 'this' */
        Pattern itselfPattern = Pattern.compile("^itself$");
        Matcher itselfMatcher = itselfPattern.matcher(expression);

        if (itselfMatcher.matches()) {
            FlowExpressions.Receiver expr = FlowExpressionParseUtil.parseAllowingItself(expression, flowExprContext, getCurrentPath());

            if (expr == null) {
                // No variable, class, etc. named "itself" could be found.
                // Hence "itself" is interpreted to actually mean itself.
                expr = FlowExpressions.internalReprOf(atypeFactory,
                        node);
            }

            return expr;
        }

        return super.parseExpressionString(expression, flowExprContext, node);
    }
}
