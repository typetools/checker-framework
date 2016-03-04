package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ThisLiteralNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
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
import java.util.HashSet;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;

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

        // TODO: Note that there is currently no way to reliably retrieve user-written
        // qualifiers (i.e. there is no way to know whether the qualifier was defaulted by
        // the Checker Framework) without using the node.toString() hack below.

        TypeMirror nodeType = atypeFactory.getAnnotatedType(node).getUnderlyingType();
        if ((TypesUtils.isBoxedPrimitive(nodeType) ||
             TypesUtils.isPrimitive(nodeType) ||
             TypesUtils.isString(nodeType)) &&
            (node.toString().contains("GuardSatisfied") ||
             node.toString().contains("GuardedBy"))){ // HACK!!! TODO: Fix once there is a way to reliably retrieve user-written qualifiers.
            checker.report(Result.failure("primitive.type.guardedby"), node);
        }
        return super.visitVariable(node, p);
    }

    @Override
    public LockAnnotatedTypeFactory createTypeFactory() {
        return new LockAnnotatedTypeFactory(checker);
    }

    // Issue an error if a method (explicitly or implicitly) annotated with @MayReleaseLocks has a formal parameter
    // or receiver (explicitly or implicitly) annotated with @GuardSatisfied.
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

        return super.visitMethod(node, p);
    }

    // When visiting a method call, if the receiver formal parameter
    // has type @GuardSatisfied and the receiver actual parameter has type @GuardedBy(...),
    // skip the receiver subtype check and instead verify that the guard is satisfied.
    // If the receiver actual parameter has type @GuardSatisfied, skip the check altogether.
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
                    Element invokedElement = TreeUtils.elementFromUse(node);

                    boolean receiverIsThatOfEnclosingMethod = false;

                    MethodInvocationNode nodeNode = (MethodInvocationNode) atypeFactory.getNodeForTree(node);

                    Node receiverNode = nodeNode.getTarget().getReceiver();
                    if (receiverNode instanceof ThisLiteralNode) {
                        receiverIsThatOfEnclosingMethod = true;
                    }

                    if (invokedElement != null) {
                        checkPreconditions(node,
                                invokedElement,
                                true,
                                generatePreconditionsBasedOnGuards(methodCallReceiver, receiverIsThatOfEnclosingMethod),
                                true);
                    }

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
            }
            else {
                annotationSet.add(anno);
            }
        }
        return annotationSet;
    }

    private Set<Pair<String, String>> generatePreconditionsBasedOnGuards(AnnotatedTypeMirror atm, boolean translateItselfToThis) {
        return generatePreconditionsBasedOnGuards(atm.getAnnotations(), translateItselfToThis);
    }

    // Given a set of AnnotationMirrors, returns the list of lock expression preconditions
    // specified in all the @GuardedBy annotations in the set.
    // Returns an empty set if no such expressions are found.
    private Set<Pair<String, String>> generatePreconditionsBasedOnGuards(Set<AnnotationMirror> amList, boolean translateItselfToThis) {
        Set<Pair<String, String>> preconditions = new HashSet<>();

        if (amList != null) {
            for (AnnotationMirror annotationMirror : amList) {

                if (AnnotationUtils.areSameByClass( annotationMirror, checkerGuardedByClass)) {
                    if (AnnotationUtils.hasElementValue(annotationMirror, "value")) {
                        List<String> guardedByValue = AnnotationUtils.getElementValueArray(annotationMirror, "value", String.class, false);

                        for (String lockExpression : guardedByValue) {
                            if (translateItselfToThis && lockExpression.equals("itself")) {
                                lockExpression = "this";
                            }
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

        // Assigning a value with a @GuardedBy annotation to a variable with a @GuardSatisfied annotation is always
        // legal. However this is our last chance to check anything before the @GuardedBy information is lost in the
        // assignment to the variable annotated with @GuardSatisfied. See the Lock Checker manual chapter discussion
        // on the @GuardSatisfied annotation for more details.

        if (varType.hasAnnotation(GuardSatisfied.class)) {
            if (valueType.hasAnnotation(GuardedBy.class)) {
                ExpressionTree tree = (ExpressionTree) valueTree;

                checkPreconditions(tree,
                        TreeUtils.elementFromUse(tree),
                        tree.getKind() == Tree.Kind.METHOD_INVOCATION,
                        generatePreconditionsBasedOnGuards(valueType, false),
                        false);

                return;
            }
            else if (valueType.hasAnnotation(GuardSatisfied.class)) {
                // TODO: Find a cleaner, non-abstraction-breaking way to know whether method actual parameters are being assigned to formal parameters.

                if (errorKey != "argument.type.incompatible") {
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
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignment);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        checkAccessOfExpression(node);

        return super.visitMemberSelect(node, p);
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
        }
        else {
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

    // Check the access of the expression of an ArrayAccessTree or
    // a MemberSelectTree, both of which happen to implement ExpressionTree.
    // The 'Expression' in checkAccessOfExpression is not the same as that in
    // 'Expression'Tree - the naming is a coincidence.
    protected void checkAccessOfExpression(ExpressionTree tree) {
        Kind treeKind = tree.getKind();
        assert(treeKind == Kind.ARRAY_ACCESS ||
               treeKind == Kind.MEMBER_SELECT ||
               treeKind == Kind.IDENTIFIER);

        if (treeKind == Kind.MEMBER_SELECT) {
            Element treeElement = TreeUtils.elementFromUse(tree);

            if (treeElement != null && treeElement.getKind() == ElementKind.METHOD) { // Method calls are not dereferences.
                return;
            }
        }

        ExpressionTree expr = null;

        switch(treeKind) {
            case ARRAY_ACCESS:
                expr = ((ArrayAccessTree) tree).getExpression();
                break;
            case MEMBER_SELECT:
                expr = ((MemberSelectTree) tree).getExpression();
                break;
            default:
                expr = tree;
                break;
        }

        Element invokedElement = TreeUtils.elementFromUse(expr);

        AnnotatedTypeMirror atmOfReceiver = atypeFactory.getReceiverType(tree);

        Node node = atypeFactory.getNodeForTree(tree);

        if (expr != null && atmOfReceiver != null) {
            AnnotationMirror gb = atmOfReceiver.getEffectiveAnnotationInHierarchy(GUARDEDBYUNKNOWN);
            // IMPORTANT: The code that follows relies on getEffectiveAnnotationInHierarchy being sound in the sense that
            // it never returns null if an effective annotation in the @GuardedByUnknown hierarchy was present.
            // It is critical to the soundness of the Lock Checker that an effective primary @GuardedBy(...) annotation
            // never go unnoticed when checking the access of an expression.
            // However, gb is expected to be null if atmOfReceiver's underlying type is a package pseudo-type (e.g. java.lang.reflect),
            // in which case there is no need to check the access of the expression since a package cannot be protected by a lock.
            if (gb == null) {
                if (atmOfReceiver.getUnderlyingType().getKind() == TypeKind.PACKAGE) {
                    return;
                }

                ErrorReporter.errorAbort("LockVisitor.checkAccessOfExpression: gb cannot be null");
            }

            if (AnnotationUtils.areSameByClass( gb, checkerGuardedByClass)) {
                if (invokedElement != null) {
                    // Is the receiver of the expression being accessed the same as the receiver of the enclosing method?
                    boolean receiverIsThatOfEnclosingMethod = false;

                    if (node instanceof FieldAccessNode || node instanceof MethodAccessNode) {
                        Node receiverNode = node instanceof FieldAccessNode ? ((FieldAccessNode) node).getReceiver() : ((MethodAccessNode) node).getReceiver();
                        if (receiverNode instanceof ThisLiteralNode) {
                            receiverIsThatOfEnclosingMethod = true;
                        }
                    }

                    // It is critical that if receiverIsThatOfEnclosingMethod is true,
                    // generatePreconditionsBasedOnGuards translate the expression
                    // "itself" to "this". That's because right now we know that, since
                    // we are dealing with the receiver of the method, "itself" corresponds
                    // to "this". However once checkPreconditions is called, that
                    // knowledge is lost and it will regard "itself" as referring to
                    // the variable the precondition we are about to add is attached to.
                    checkPreconditions(expr, invokedElement, expr.getKind() == Tree.Kind.METHOD_INVOCATION,
                        generatePreconditionsBasedOnGuards(atmOfReceiver,
                                receiverIsThatOfEnclosingMethod /* See comment above. This corresponds to formal parameter translateItselfToThis. */),
                                false);
                }
            } else if (AnnotationUtils.areSameByClass(gb, checkerGuardSatisfiedClass)){
                // Can always dereference if type is @GuardSatisfied
            } else {
                // Can never dereference for any other types in the @GuardedBy hierarchy
                String annotationName = gb.toString();
                annotationName = annotationName.substring(annotationName.lastIndexOf('.') + 1 /* +1 to skip the last . as well */);
                checker.report(Result.failure(
                        "cannot.dereference",
                        tree.toString(),
                        "annotation @" + annotationName), tree);
            }
        }
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        checkAccessOfExpression(node);

        return super.visitArrayAccess(node, p);
    }

    /**
     * Whether to skip a contract check based on whether the @GuardedBy
     * expression {@code expr} is valid for the tree {@code tree}
     * under the context {@code flowExprContext}
     * if the current path is within the expression
     * of a synchronized block (e.g. bar in
     * synchronized(bar) { ... }
     *
     *  @param tree The tree that is @GuardedBy.
     *  @param expr The expression of the @GuardedBy annotation.
     *  @param flowExprContext The current context.
     *
     *  @return Whether to skip the contract check.
     */
    @Override
    protected boolean skipContractCheck(Tree tree, FlowExpressions.Receiver expr, FlowExpressionContext flowExprContext) {
        String fieldName = null;

        try {

            Node nodeNode = atypeFactory.getNodeForTree(tree);

            if (nodeNode instanceof FieldAccessNode) {

                fieldName = ((FieldAccessNode) nodeNode).getFieldName();

                if (fieldName != null) {
                    FlowExpressions.Receiver fieldExpr = FlowExpressionParseUtil.parse(fieldName,
                            flowExprContext, getCurrentPath());

                    if (fieldExpr.equals(expr)) {
                        // Avoid issuing warnings when accessing the field that is guarding the receiver.
                        // e.g. avoid issuing a warning when accessing bar below:
                        // void foo(@GuardedBy("bar") myClass this) { synchronized(bar) { ... }}

                        // Cover only the most common case: synchronized(variableName).
                        // If the expression in the synchronized statement is more complex,
                        // we do want a warning to be issued so the user can take a closer look
                        // and see if the variable is safe to be used this way.

                        TreePath path = getCurrentPath().getParentPath();

                        if (path != null) {
                            path = path.getParentPath();

                            if (path != null && path.getLeaf().getKind() == Tree.Kind.SYNCHRONIZED) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (FlowExpressionParseException e) {
            checker.report(e.getResult(), tree);
        }

        return false;
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {
        declarationType.replaceAnnotation(GUARDEDBY);
        useType.replaceAnnotation(GUARDEDBY);

        return super.isValidUse(declarationType, useType, tree);
    }

    // When visiting a method invocation, issue an error if the side effect annotation
    // on the called method causes the side effect guarantee of the enclosing method
    // to be violated. For example, a method annotated with @ReleasesNoLocks may not
    // call a method annotated with @MayReleaseLocks.
    // Also check that matching @GuardSatisfied(index) on a method's formal receiver/parameters matches
    // those in corresponding locations on the method call site.
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

            // TODO: issue method.guarantee.violated if a @LockingFree method is synchronized or
            // contains synchronized blocks.
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
            !TreeUtils.isSuperCall(node) &&
            invokedMethod.getElement().getKind() != ElementKind.CONSTRUCTOR) {
            methodDefinitionReceiver = invokedMethod.getReceiverType().getErased();
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

    // When visiting a synchronized block, issue an error if the expression
    // has a type that implements the java.util.concurrent.locks.Lock interface.
    // TODO: make a type declaration annotation for this rather than looking for Lock.class explicitly.
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();

        javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

        TypeMirror lockInterfaceTypeMirror = TypesUtils.typeFromClass(types, processingEnvironment.getElementUtils(), Lock.class);

        TypeMirror expressionType = types.erasure(atypeFactory.getAnnotatedType(node.getExpression()).getUnderlyingType());

        if (types.isSubtype(expressionType, lockInterfaceTypeMirror)) {
            checker.report(Result.failure(
                    "explicit.lock.synchronized"), node);
        }

        return super.visitSynchronized(node, p);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {

        checkAccessOfExpression(node);

        return super.visitIdentifier(node, p);
    }

    @Override
    protected void checkPreconditions(Tree tree,
            Element invokedElement, boolean methodCall, Set<Pair<String, String>> additionalPreconditions) {
        checkPreconditions(tree, invokedElement, methodCall, additionalPreconditions, false);
    }

    // Same contents as BaseTypeVisitor.checkPreconditions except for the addition of the
    // else if (nodeNode instanceof ExplicitThisLiteralNode || ...
    // block and the special handling under the
    // if (itselfMatcher.matches()) {
    // block.
    protected void checkPreconditions(Tree tree,
            Element invokedElement, boolean methodCall, Set<Pair<String, String>> additionalPreconditions,
            boolean itselfIsTheReceiverNode) {
        Set<Pair<String, String>> preconditions = invokedElement == null ?
                new HashSet<Pair<String, String>>() :
                contractsUtils.getPreconditions(invokedElement);

        if (additionalPreconditions != null) {
            preconditions.addAll(additionalPreconditions);
        }

        FlowExpressionContext flowExprContext = null;

        for (Pair<String, String> p : preconditions) {
            String expression = p.first;
            AnnotationMirror anno = AnnotationUtils.fromName(elements, p.second);

            // Only check if the precondition concerns this checker
            if (!atypeFactory.isSupportedQualifier(anno)) {
                return;
            }

            Node nodeNode = atypeFactory.getNodeForTree(tree);

            if (flowExprContext == null) {
                if (methodCall) {
                    flowExprContext = FlowExpressionParseUtil
                            .buildFlowExprContextForUse(
                                    (MethodInvocationNode) nodeNode, checker.getContext());
                }
                else if (nodeNode instanceof FieldAccessNode) {
                    // Adapted from FlowExpressionParseUtil.buildFlowExprContextForUse

                    Receiver internalReceiver = FlowExpressions.internalReprOf(atypeFactory,
                        ((FieldAccessNode) nodeNode).getReceiver());

                    flowExprContext = new FlowExpressionContext(
                            internalReceiver, null, checker.getContext());
                }
                else if (nodeNode instanceof LocalVariableNode) {
                    // Adapted from org.checkerframework.dataflow.cfg.CFGBuilder.CFGTranslationPhaseOne.visitVariable

                    ClassTree enclosingClass = TreeUtils
                            .enclosingClass(getCurrentPath());
                    TypeElement classElem = TreeUtils
                            .elementFromDeclaration(enclosingClass);
                    Node receiver = new ImplicitThisLiteralNode(classElem.asType());

                    Receiver internalReceiver = FlowExpressions.internalReprOf(atypeFactory,
                            receiver);

                    flowExprContext = new FlowExpressionContext(
                            internalReceiver, null, checker.getContext());
                }
                else if (nodeNode instanceof ArrayAccessNode) {
                    // Adapted from FlowExpressionParseUtil.buildFlowExprContextForUse

                    Receiver internalReceiver = FlowExpressions.internalReprOfArrayAccess(atypeFactory,
                        (ArrayAccessNode) nodeNode);

                    flowExprContext = new FlowExpressionContext(
                            internalReceiver, null, checker.getContext());
                } else if (nodeNode instanceof ExplicitThisLiteralNode ||
                           nodeNode instanceof ImplicitThisLiteralNode ||
                           nodeNode instanceof ThisLiteralNode) {
                   Receiver internalReceiver = FlowExpressions.internalReprOf(atypeFactory, nodeNode, false);

                   flowExprContext = new FlowExpressionContext(
                           internalReceiver, null, checker.getContext());
                }
            }

            if (flowExprContext != null) {
                FlowExpressions.Receiver expr = null;
                try {
                    CFAbstractStore<?, ?> store = atypeFactory.getStoreBefore(tree);

                    String s = expression.trim();
                    Pattern selfPattern = Pattern.compile("^(this)$");
                    Matcher selfMatcher = selfPattern.matcher(s);
                    if (selfMatcher.matches()) {
                        s = flowExprContext.receiver.toString(); // it is possible that s == "this" after this call
                    }

                    expr = FlowExpressionParseUtil.parse(expression,
                            flowExprContext, getCurrentPath());

                    if (expr == null) {
                        // TODO: Wrap the following 'itself' handling logic into a method that calls FlowExpressionParseUtil.parse

                        /** Matches 'itself' - it refers to the variable that is annotated, which is different from 'this' */
                        Pattern itselfPattern = Pattern.compile("^itself$");
                        Matcher itselfMatcher = itselfPattern.matcher(expression.trim());

                        if (itselfMatcher.matches()) { // There is no variable, class, etc. named "itself"
                            if (itselfIsTheReceiverNode && methodCall) {
                                expr = flowExprContext.receiver;
                            } else {
                                expr = FlowExpressions.internalReprOf(atypeFactory,
                                        nodeNode);
                            }
                        }
                    }

                    CFAbstractValue<?> value = store.getValue(expr);

                    AnnotationMirror inferredAnno = value == null ? null : value
                            .getType().getAnnotationInHierarchy(anno);
                    if (!skipContractCheck(tree, expr, flowExprContext) &&
                        !checkContract(expr, anno, inferredAnno, store)) {

                        checker.report(Result.failure(
                                methodCall ? "contracts.precondition.not.satisfied" : "contracts.precondition.not.satisfied.field",
                                tree.toString(),
                                expr == null ? expression : expr.toString()), tree);
                    }
                } catch (FlowExpressionParseException e) {
                    // errors are reported at declaration site
                }
            }
        }
    }
}
