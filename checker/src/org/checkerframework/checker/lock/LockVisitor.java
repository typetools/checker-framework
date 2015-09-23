package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByInaccessible;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ThisLiteralNode;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.ArrayAccessTree;
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

public class LockVisitor extends BaseTypeVisitor<LockAnnotatedTypeFactory> {
    private final Class<? extends Annotation> checkerGuardedByClass = GuardedBy.class;
    private final Class<? extends Annotation> checkerGuardSatisfiedClass = GuardSatisfied.class;
    private final Class<? extends Annotation> checkerLockHeldClass = LockHeld.class;

    // Note that Javax and JCIP @GuardedBy is used on both methods and objects. For methods they are
    // equivalent to the Checker Framework @Holding annotation.
    private final Class<? extends Annotation> javaxGuardedByClass = javax.annotation.concurrent.GuardedBy.class;
    private final Class<? extends Annotation> jcipGuardedByClass = net.jcip.annotations.GuardedBy.class;

    /** Annotation constants */
    protected final AnnotationMirror GUARDEDBY, GUARDEDBYINACCESSIBLE, GUARDSATISFIED, GUARDEDBYBOTTOM;

    public LockVisitor(BaseTypeChecker checker) {
        super(checker);

        GUARDEDBYINACCESSIBLE = AnnotationUtils.fromClass(elements, GuardedByInaccessible.class);
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        GUARDSATISFIED = AnnotationUtils.fromClass(elements, GuardSatisfied.class);
        GUARDEDBYBOTTOM = AnnotationUtils.fromClass(elements, GuardedByBottom.class);

        checkForAnnotatedJdk();
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) { // visit a variable declaration
        // A user may not annotate a primitive type with any qualifier from the @GuardedBy hierarchy.
        if (node.getType().getKind() == Kind.PRIMITIVE_TYPE &&
            (node.toString().contains("GuardSatisfied") ||
             node.toString().contains("GuardedBy"))){ // HACK!!! TODO: Fix once there is a way to reliably retrieve user-written qualifiers.
            checker.report(Result.failure("primitive.type.guardedby"), node);
        }
        return super.visitVariable(node, p);
    }

    @Override
    public LockAnnotatedTypeFactory createTypeFactory() {
        // We need to directly access useFlow from the checker, because this method gets called
        // by the superclass constructor and a field in this class would not be initialized
        // yet. Oh the pain.
        return new LockAnnotatedTypeFactory(checker, true);
    }

    // Issue an error if a method (explicitly or implicitly) annotated with @MayReleaseLocks has a formal parameter
    // or receiver (explicitly or implicitly) annotated with @GuardSatisfied.
    @Override
    public Void visitMethod(MethodTree node, Void p) {

        SideEffectAnnotation sea = atypeFactory.methodSideEffectAnnotation(TreeUtils.elementFromDeclaration(node), true);

        if (sea == SideEffectAnnotation.MAYRELEASELOCKS) {
            boolean issueGSwithMRLWarning = false;

            VariableTree receiver = node.getReceiverParameter();
            if (receiver != null) {
                if (AnnotationUtils.areSameByClass(atypeFactory.getAnnotatedType(receiver).getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE),
                        checkerGuardSatisfiedClass)){
                    issueGSwithMRLWarning = true;
                }
            }

            if (!issueGSwithMRLWarning) { // Skip this loop if it is already known that the warning must be issued.
                for(VariableTree vt : node.getParameters()) {
                    if (AnnotationUtils.areSameByClass(atypeFactory.getAnnotatedType(vt).getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE),
                            checkerGuardSatisfiedClass)){
                        issueGSwithMRLWarning = true;
                        break;
                    }
                }
            }

            if (issueGSwithMRLWarning) {
                checker.report(Result.failure("guardsatisfied.with.mayreleaselocks"), node);
            }
        }

        return super.visitMethod(node, p);
    }

    // When visiting a method call, if the receiver formal parameter
    // has type @GuardSatisfied, skip the receiver subtype check and instead
    // verify that the guard is satisfied.
    @Override
    protected boolean skipReceiverSubtypeCheck(MethodInvocationTree node,
            AnnotatedTypeMirror methodDefinitionReceiver,
            AnnotatedTypeMirror methodCallReceiver) {
        Set<AnnotationMirror> annos = methodDefinitionReceiver.getAnnotations();
        for(AnnotationMirror anno : annos) {
            if (AnnotationUtils.areSameByClass(anno, checkerGuardSatisfiedClass)) {
                Element invokedElement = TreeUtils.elementFromUse(node);

                if (invokedElement != null) {
                    checkPreconditions(node,
                            invokedElement,
                            true,
                            generatePreconditionsBasedOnGuards(methodCallReceiver, false));
                }

                return true;
            }
        }

        return false;
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<? extends AnnotationMirror> tops = atypeFactory.getQualifierHierarchy().getTopAnnotations();
        Set<AnnotationMirror> annotationSet = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : tops) {
            if (anno.equals(GUARDEDBYINACCESSIBLE)) {
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

                if (AnnotationUtils.areSameByClass( annotationMirror, checkerGuardedByClass) ||
                    AnnotationUtils.areSameByClass( annotationMirror, javaxGuardedByClass) ||
                    AnnotationUtils.areSameByClass( annotationMirror, jcipGuardedByClass)) {
                    if (AnnotationUtils.hasElementValue(annotationMirror, "value")) {
                        List<String> guardedByValue = AnnotationUtils.getElementValueArray(annotationMirror, "value", String.class, false);

                        for(String lockExpression : guardedByValue) {
                            if (translateItselfToThis && lockExpression.equals("itself")) {
                                lockExpression = "this";
                            }
                            preconditions.add(Pair.of(lockExpression, checkerLockHeldClass.toString().substring(10 /* "interface " */)));
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
            boolean isLocalVariableAssignement) {

        if (valueType.getKind() == TypeKind.NULL) {
            // Avoid issuing warnings about 'null' not matching the type of the variable.
            return;
        }

        Kind valueTreeKind = valueTree.getKind();

        switch(valueTreeKind) {
            case NEW_CLASS:
            case NEW_ARRAY:
                // Avoid issuing warnings for: @GuardedBy(<something>) Object o = new Object();
                // Do NOT do this if the LHS is @GuardedByBottom.
                if (!AnnotationUtils.areSameIgnoringValues(varType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDEDBYBOTTOM))
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
                if (!AnnotationUtils.areSameIgnoringValues(varType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDEDBYBOTTOM))
                    return;
                return;
            case NULL_LITERAL: // Don't return in the case of NULL_LITERAL since the check must be made that the LHS is @GuardedByBottom.
            default:
        }

        boolean skipSubtypeCheck = false;

        // Assigning a value with a @GuardedBy annotation to a variable with a @GuardedByInaccessible annotation is always
        // legal. However as a precaution we verify that the locks specified in the @GuardedBy annotation are held,
        // since this is our last chance to check anything before the @GuardedBy information is lost in the
        // assignment to the variable annotated with @GuardedByInaccessible. See the Lock Checker manual chapter discussion
        // on the @GuardedByInaccessible annotation for more details.
        // The same behavior applies to @GuardSatisfied.
        if (AnnotationUtils.areSameIgnoringValues(varType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDEDBYINACCESSIBLE) ||
            AnnotationUtils.areSameIgnoringValues(varType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDSATISFIED)) {
            if (AnnotationUtils.areSameIgnoringValues(valueType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDEDBY)) {
                ExpressionTree tree = (ExpressionTree) valueTree;

                checkPreconditions(tree,
                        TreeUtils.elementFromUse(tree),
                        tree.getKind() == Tree.Kind.METHOD_INVOCATION,
                          generatePreconditionsBasedOnGuards(valueType, false));

                skipSubtypeCheck = true;
            }
        }

        if (!skipSubtypeCheck) {
            super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignement);
        }
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

    // Ensures that subclass methods require a subset of the locks that a parent class method requires
    // Additionally ensures that subclass methods use @HoldingOnEntry (and not @Holding) if the parent
    // class method uses @HoldingOnEntry.
    @Override
    protected boolean checkOverride(MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            Void p) {

        // Check @Holding overrides

        List<String> overriderLocks = atypeFactory.methodHolding(TreeUtils.elementFromDeclaration(overriderTree));
        List<String> overriddenLocks = atypeFactory.methodHolding(overridden.getElement());

        boolean isValid = true;

        if (!overriderLocks.isEmpty()) {
            if (!overriddenLocks.containsAll(overriderLocks)) {
                isValid = false;
                reportFailure("override.holding.invalid", overriderTree, enclosingType, overridden, overriddenType, overriderLocks, overriddenLocks);
            }
        }

        // Check side effect annotation overrides

        SideEffectAnnotation seaOfOverriderMethod = atypeFactory.methodSideEffectAnnotation(TreeUtils.elementFromDeclaration(overriderTree), false);
        SideEffectAnnotation seaOfOverridenMethod = atypeFactory.methodSideEffectAnnotation(overridden.getElement(), false);

        if (seaOfOverriderMethod.ordinal() < seaOfOverridenMethod.ordinal()) {
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
        boolean isMethodCall = false;

        Kind treeKind = tree.getKind();
        assert(treeKind == Kind.ARRAY_ACCESS ||
               treeKind == Kind.MEMBER_SELECT ||
               treeKind == Kind.IDENTIFIER);

        if (treeKind == Kind.MEMBER_SELECT) {
            Element treeElement = TreeUtils.elementFromUse(tree);

            if (treeElement != null && treeElement.getKind() == ElementKind.METHOD) { // Method calls are not dereferences.
                isMethodCall = true;
            }
        }

        if (!isMethodCall) {
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

            AnnotatedTypeMirror receiverAtm = atypeFactory.getReceiverType(tree);
            Node node = atypeFactory.getNodeForTree(tree);

            // Is the receiver of the expression being accessed the same as the receiver of the enclosing method?
            boolean receiverIsThatOfEnclosingMethod = false;

            if (node instanceof FieldAccessNode) {
                Node receiverNode = ((FieldAccessNode) node).getReceiver();
                if (receiverNode instanceof ExplicitThisLiteralNode ||
                    receiverNode instanceof ImplicitThisLiteralNode ||
                    receiverNode instanceof ThisLiteralNode) {
                    receiverIsThatOfEnclosingMethod = true;
                }
            }

            if (expr != null && invokedElement != null && receiverAtm != null) {
                boolean skipCheckPreconditions = false;

                AnnotationMirror gb = receiverAtm.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE);
                if (gb != null) {
                    if (AnnotationUtils.areSameByClass( gb, checkerGuardedByClass) ||
                        AnnotationUtils.areSameByClass( gb, javaxGuardedByClass) ||
                        AnnotationUtils.areSameByClass( gb, jcipGuardedByClass)) {
                        // Do nothing.
                    } else if (AnnotationUtils.areSameByClass(gb, checkerGuardSatisfiedClass)){
                        skipCheckPreconditions = true; // Can always dereference if type is @GuardSatisfied
                    } else {
                        // Can never dereference for any other types in the @GuardedBy hierarchy
                        String annotationName = gb.toString();
                        annotationName = annotationName.substring(annotationName.lastIndexOf('.') + 1 /* +1 to skip the last . as well */);
                        checker.report(Result.failure(
                                "cannot.dereference",
                                tree.toString(),
                                annotationName), tree);
                        return;
                    }
                }

                if (skipCheckPreconditions == false) {
                    // It is critical that if receiverIsThatOfEnclosingMethod is true,
                    // generatePreconditionsBasedOnGuards translate the expression
                    // "itself" to "this". That's because right now we know that, since
                    // we are dealing with the receiver of the method, "itself" corresponds
                    // to "this". However once checkPreconditions is called, that
                    // knowledge is lost and it will regards "itself" as referring to
                    // the variable the precondition we are about to add is attached to.

                    checkPreconditions(expr, invokedElement, expr.getKind() == Tree.Kind.METHOD_INVOCATION,
                    generatePreconditionsBasedOnGuards(receiverAtm,
                            receiverIsThatOfEnclosingMethod /* see comment above */));
                }
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
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        SideEffectAnnotation seaOfInvokedMethod = atypeFactory.methodSideEffectAnnotation(TreeUtils.elementFromUse(node), false);

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(node));

        ExecutableElement methodElement = null;
        if (enclosingMethod != null) {
            methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
        }

        SideEffectAnnotation seaOfContainingMethod = atypeFactory.methodSideEffectAnnotation(methodElement, false);

        if (seaOfInvokedMethod.ordinal() < seaOfContainingMethod.ordinal()) {
            checker.report(Result.failure(
                    "method.guarantee.violated",
                    methodElement.toString(),
                    TreeUtils.elementFromUse(node).toString()), node);
        }

        return super.visitMethodInvocation(node, p);
    }

    // When visiting a synchronized block, issue an error if the expression
    // has a type that implements the java.util.concurrent.locks.Lock inteface.
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        LockAnalysis analysis = ((LockChecker)checker).getAnalysis();

        javax.lang.model.util.Types types = analysis.getTypes();

        TypeMirror lockInterfaceTypeMirror = TypesUtils.typeFromClass(types, analysis.getEnv().getElementUtils(), Lock.class);

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
}
