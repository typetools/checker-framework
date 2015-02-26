package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByTop;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;

/**
 * The LockVisitor enforces the subtyping rules of LockHeld and LockPossiblyHeld
 * (via BaseTypeVisitor). It also manually verifies that @Holding and @HoldingOnEntry
 * annotations are properly used on overridden methods. It handles @GuardedBy annotations
 * on method receivers. Finally, it ensures that we avoid doing any lock checking
 * when visiting initializers.
 */

public class LockVisitor extends BaseTypeVisitor<LockAnnotatedTypeFactory> {
    private final Class<? extends Annotation> checkerGuardedByClass = org.checkerframework.checker.lock.qual.GuardedBy.class;
    private final Class<? extends Annotation> checkerHoldingClass = org.checkerframework.checker.lock.qual.Holding.class;
    private final Class<? extends Annotation> checkerHoldingOnEntryClass = org.checkerframework.checker.lock.qual.HoldingOnEntry.class;
    private final Class<? extends Annotation> checkerLockHeldClass = org.checkerframework.checker.lock.qual.LockHeld.class;

    // Note that Javax and JCIP @GuardedBy is used on both methods and objects. For methods they are
    // equivalent to the Checker Framework @Holding annotation.
    private final Class<? extends Annotation> javaxGuardedByClass = javax.annotation.concurrent.GuardedBy.class;
    private final Class<? extends Annotation> jcipGuardedByClass = net.jcip.annotations.GuardedBy.class;

    /** Annotation constants */
    protected final AnnotationMirror GUARDEDBY, GUARDEDBYTOP;

    public LockVisitor(BaseTypeChecker checker) {
        super(checker);

        GUARDEDBYTOP = AnnotationUtils.fromClass(elements, GuardedByTop.class);
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);

        checkForAnnotatedJdk();
    }

    @Override
    public LockAnnotatedTypeFactory createTypeFactory() {
        // We need to directly access useFlow from the checker, because this method gets called
        // by the superclass constructor and a field in this class would not be initialized
        // yet. Oh the pain.
        return new LockAnnotatedTypeFactory(checker, true);
    }

    @Override
    protected void checkAccess(IdentifierTree node, Void p) {
        // This method is called by visitIdentifier (and only visitIdentifier).

        // Unless the identifier is a primitive or syntactic sugar for another expression, do not check preconditions.
        // Preconditions in the Lock Checker for reference types must not be
        // checked by visitIdentifier, since we want only dereferences of variables
        // to have their Preconditions enforced, but not every instance of the variable
        // (due to by-value instead of by-variable semantics for the Lock Checker).
        // The exception to this will be visitSynchronized, but that will be handled separately.
        // See the Lock Checker manual chapter definitions of dereferencing a value/variable
        // for more information.

        Node nodeNode = atypeFactory.getNodeForTree(node);

        // TODO: A check such as the following should determine whether the identifier
        // evaluates to a primitive type even when it looks like a reference type (e.g.
        // unboxing of a boxed type).
        // (nodeNode != null && nodeNode.getInSource() == false)
        // This doesn't work as expected, however, because the correct inSource information
        // is stored in ControlFlowGraph.convertedTreeLookup, whereas at this point
        // only ControlFlowGraph.treeLookup is available (via atypeFactory.getNodeForTree).
        // The precise point in the code where this information is lost (i.e. a reference
        // to convertedTreeLookup is not copied to the analysis result) is in the following
        // two lines in GenericAnnotatedTypeFactory.analyze :

        // analyses.getFirst().performAnalysis(cfg);
        // AnalysisResult<Value, Store> result = analyses.getFirst().getResult();

        // convertedTreeLookup is available in cfg, but a reference to it is not copied
        // over in getResult(). This should be fixed in a future release. At the present
        // time, such a change would unduly introduce risk to the release.

        // As a temporary workaround, boxed types are conservatively always treated
        // as if they are being converted to a primitive, even when they are being used
        // as a reference (since we can't tell which one is the case). This will conservatively
        // result in more errors visible to the user.

        boolean doCheckPreconditions = (nodeNode != null && TypesUtils.isBoxedPrimitive(nodeNode.getType())) ||
            (node instanceof JCTree && TypesUtils.isPrimitive(((JCTree) node).type));

        super.checkAccess(node, p, doCheckPreconditions);
    }

    @Override
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp,
            String errorKey) {
        // If the RHS is known for sure to be a primitive type, skip the check.
        // Dereferences of primitives require the appropriate locks to be held,
        // but it does not require the annotations in the types involved in the
        // operation to match.
        // For example, given:
        // @GuardedBy("foo") int a;
        // @GuardedBy("bar") int b;
        // @GuardedBy({}) int c;
        // The expressions a = b, a = c, and a = b + c are legal from a
        // type-checking perspective, whereas none of them would be legal
        // if a, b and c were not primitives.

        if (!(valueExp instanceof JCTree && ((JCTree) valueExp).type.getKind().isPrimitive())) {
            super.commonAssignmentCheck(varTree, valueExp, errorKey);
        }
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<? extends AnnotationMirror> tops = atypeFactory.getQualifierHierarchy().getTopAnnotations();
        Set<AnnotationMirror> annotationSet = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : tops) {
            if (anno.equals(GUARDEDBYTOP)) {
                annotationSet.add(GUARDEDBY);
            }
            else {
                annotationSet.add(anno);
            }
        }
        return annotationSet;
    }

    private Set<Pair<String, String>> getPreconditions(AnnotatedTypeMirror atm) {
        return getPreconditions(atm.getAnnotations());
    }

    // Given a set of AnnotationMirrors, returns the list of lock expression preconditions
    // specified in all the @GuardedBy annotations in the set.
    // Returns an empty set if no such expressions are found.
    private Set<Pair<String, String>> getPreconditions(Set<AnnotationMirror> amList) {
        Set<Pair<String, String>> preconditions = new HashSet<>();

        if (amList != null) {
            for (AnnotationMirror annotationMirror : amList) {

                if (AnnotationUtils.areSameByClass( annotationMirror, checkerGuardedByClass) ||
                    AnnotationUtils.areSameByClass( annotationMirror, javaxGuardedByClass) ||
                    AnnotationUtils.areSameByClass( annotationMirror, jcipGuardedByClass)) {
                    if (AnnotationUtils.hasElementValue(annotationMirror, "value")) {
                        List<String> guardedByValue = AnnotationUtils.getElementValueArray(annotationMirror, "value", String.class, false);

                        for(String lockExpression : guardedByValue) {
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
                // Avoid issuing warnings when: @GuardedBy("this") Object guardedThis = new Object();
                // TODO: This is too broad and should be fixed to work the way it will work in the hacks repo.
                return;
            case INT_LITERAL:
            case LONG_LITERAL:
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
            case STRING_LITERAL:
            case NULL_LITERAL:
                // Avoid issuing warnings when: guardedThis = "m";
                // TODO: This is too broad and should be fixed to work the way it will work in the hacks repo.
                return;
            default:
        }

        // Assigning a value with a @GuardedBy annotation to a variable with a @GuardedByTop annotation is always
        // legal. However as a precaution we verify that the locks specified in the @GuardedBy annotation are held,
        // since this is our last chance to check anything before the @GuardedBy information is lost in the
        // assignment to the variable annotated with @GuardedByTop. See the Lock Checker manual chapter discussion
        // on the @GuardedByTop annotation for more details.
        if (AnnotationUtils.areSameIgnoringValues(varType.getAnnotationInHierarchy(GUARDEDBYTOP), GUARDEDBYTOP)) {
            if (AnnotationUtils.areSameIgnoringValues(valueType.getAnnotationInHierarchy(GUARDEDBYTOP), GUARDEDBY)) {
                ExpressionTree tree = (ExpressionTree) valueTree;

                checkPreconditions(tree,
                        TreeUtils.elementFromUse(tree),
                        tree.getKind() == Tree.Kind.METHOD_INVOCATION,
                        getPreconditions(valueType));
            }
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignement);
    }

    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        // Neither the GuardedBy nor the LockHeld hierarchies need to be checked for method invocability.
        // The Lock Checker defines whether a method invocation is legal based on what locks are held
        // rather than the subtyping relationship between the receiver expression and the receiver declaration.

        // Whether the refined type of the receiver is @LockPossiblyHeld / @LockHeld is irrelevant since
        // the Lock Checker only needs to check that the appropriate locks are held if the receiver has
        // a @GuardedBy annotation.

        // If the refined type of the receiver is @GuardedBy (and not @GuardedByTop or @GuardedByBottom),
        // then it is important to verify that the expressions in the @GuardedBy annotation are held.
        // This is done in the call to checkPreconditions in BaseTypeVisitor.visitMethodInvocation
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        // Just check the precondition on the expression of the member select tree.
        // It doesn't matter if the identifier is a method or field.
        // Here, we are checking that the lock must be held on the
        // expression.

        // Keep in mind, the expression itself may or may not be a
        // method call. Simple examples of expression.identifier :
        // myObject.field
        // myMethod().field
        // myObject.method()
        // myMethod().method()

        // by-value semantics require preconditions to be checked
        // on all value dereferences, including dereferences of method
        // return values.

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

        List<String> overriderLocks = methodHolding(TreeUtils.elementFromDeclaration(overriderTree));
        List<String> overriddenLocks = methodHolding(overridden.getElement());

        List<String> overriderHoldingOnEntryLocks = methodHoldingOnEntry(TreeUtils.elementFromDeclaration(overriderTree));
        List<String> overriddenHoldingOnEntryLocks = methodHoldingOnEntry(overridden.getElement());

        /*
         *  @Holding is a stronger requirement than @HoldingOnEntry, since it has both pre- and postconditions. Therefore:
         *
         *  If the overridden method uses @HoldingOnEntry, the overrider method can only use @HoldingOnEntry.
         *  If the overridden method uses @Holding, the overrider method can use either @Holding or @HoldingOnEntry.
         *
         */

        boolean isValid = true;

        if (!overriddenHoldingOnEntryLocks.isEmpty()) {
            if (!overriderLocks.isEmpty()) {
                isValid = false;
                reportFailure("override.holding.invalid.holdingonentry", overriderTree, enclosingType, overridden, overriddenType, null, null);
            } else if (!overriddenHoldingOnEntryLocks.containsAll(overriderHoldingOnEntryLocks)) {
                isValid = false;
                reportFailure("override.holding.invalid", overriderTree, enclosingType, overridden, overriddenType, overriderHoldingOnEntryLocks, overriddenHoldingOnEntryLocks);
            }
        } else {
            if (!overriderLocks.isEmpty()) {
                if (!overriddenLocks.containsAll(overriderLocks)) {
                    isValid = false;
                    reportFailure("override.holding.invalid", overriderTree, enclosingType, overridden, overriddenType, overriderLocks, overriddenLocks);
                }
            } else if (!overriddenLocks.containsAll(overriderHoldingOnEntryLocks)) {
                isValid = false;
                reportFailure("override.holding.invalid", overriderTree, enclosingType, overridden, overriddenType, overriderHoldingOnEntryLocks, overriddenLocks);
            }
        }

        return super.checkOverride(overriderTree, enclosingType, overridden, overriddenType, p) && isValid;
    }

    protected List<String> methodHolding(ExecutableElement element) {
        AnnotationMirror holding = atypeFactory.getDeclAnnotation(element, checkerHoldingClass);
        AnnotationMirror guardedBy
            = atypeFactory.getDeclAnnotation(element, jcipGuardedByClass);
        AnnotationMirror guardedByJavax
            = atypeFactory.getDeclAnnotation(element, javaxGuardedByClass);

        if (holding == null && guardedBy == null && guardedByJavax == null)
            return Collections.emptyList();

        List<String> locks = new ArrayList<String>();

        if (holding != null) {
            List<String> holdingValue = AnnotationUtils.getElementValueArray(holding, "value", String.class, false);
            locks.addAll(holdingValue);
        }
        if (guardedBy != null) {
            String guardedByValue = AnnotationUtils.getElementValue(guardedBy, "value", String.class, false);
            locks.add(guardedByValue);
        }
        if (guardedByJavax != null) {
            String guardedByValue = AnnotationUtils.getElementValue(guardedByJavax, "value", String.class, false);
            locks.add(guardedByValue);
        }

        return locks;
    }

    protected List<String> methodHoldingOnEntry(ExecutableElement element) {
        AnnotationMirror holdingOnEntry = atypeFactory.getDeclAnnotation(element, checkerHoldingOnEntryClass);

        if (holdingOnEntry == null)
            return Collections.emptyList();

        List<String> locks = new ArrayList<String>();

        List<String> holdingOnEntryValue = AnnotationUtils.getElementValueArray(holdingOnEntry, "value", String.class, false);
        locks.addAll(holdingOnEntryValue);

        return locks;
    }

    /**
     * Checks all the preconditions of the method invocation or variable access {@code tree} with
     * element {@code invokedElement}.
     */
    @Override
    protected void checkPreconditions(Tree tree,
            Element invokedElement, boolean methodCall, Set<Pair<String, String>> additionalPreconditions) {

        if (additionalPreconditions == null) {
            additionalPreconditions = new HashSet<>();
        }

        // Retrieve the @GuardedBy annotation on the receiver of the enclosing method
        // and add it to the preconditions set if the receiver on the enclosing method is
        // the same as the receiver on the field/method we are checking preconditions for.
        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(tree));

        if (enclosingMethod != null) {
            ExecutableElement methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);

            if (methodElement != null) {
                TypeMirror rt = methodElement.getReceiverType();

                if (rt != null) {
                    List<? extends AnnotationMirror> amList = rt.getAnnotationMirrors();

                    for (AnnotationMirror annotationMirror : amList) {

                        if (AnnotationUtils.areSameByClass( annotationMirror, checkerGuardedByClass) ||
                            AnnotationUtils.areSameByClass( annotationMirror, javaxGuardedByClass) ||
                            AnnotationUtils.areSameByClass( annotationMirror, jcipGuardedByClass)) {
                            List<String> guardedByValue = AnnotationUtils.getElementValueArray(annotationMirror, "value", String.class, false);

                            // A GuardedBy annotation on the receiver of the enclosing method was found.
                            // Now check if the receiver on the enclosing method is the same as the receiver
                            // on the field/method we are checking preconditions for.

                            if (guardedByValue != null) {
                                Node nodeNode = atypeFactory.getNodeForTree(tree);
                                Node receiverNode = null;

                                if (nodeNode instanceof FieldAccessNode) {
                                    receiverNode = ((FieldAccessNode) nodeNode).getReceiver();
                                }
                                else if (nodeNode instanceof MethodInvocationNode)
                                {
                                    MethodAccessNode man = ((MethodInvocationNode) nodeNode).getTarget();

                                    if (!man.getMethod().getModifiers().contains(Modifier.STATIC)) {
                                        receiverNode = man.getReceiver();
                                    }
                                }

                                if (receiverNode instanceof ExplicitThisLiteralNode ||
                                    receiverNode instanceof ImplicitThisLiteralNode) {

                                    // The receivers match. Add to the preconditions set.
                                    for(String lockExpression : guardedByValue) {

                                        if (lockExpression.equals("itself")) {
                                            // This is critical. That's because right now we know that, since
                                            // we are dealing with the receiver of the method, "itself" corresponds
                                            // to "this". However once super.checkPreconditions is called, that
                                            // knowledge is lost and it will think that "itself" is referring to
                                            // the variable the precondition we are about to add is attached to.
                                            lockExpression = "this";
                                        }

                                        additionalPreconditions.add(Pair.of(lockExpression, checkerLockHeldClass.toString().substring(10 /* "interface " */)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        super.checkPreconditions(tree, invokedElement, methodCall, additionalPreconditions);
    }

    // We check the access of the expression of an ArrayAccessTree or
    // a MemberSelectTree, both of which happen to implement ExpressionTree.
    // The 'Expression' in checkAccessOfExpression is not the same as that in
    // 'Expression'Tree - the naming is a coincidence.
    protected void checkAccessOfExpression(ExpressionTree tree) {
        Kind treeKind = tree.getKind();
        assert(treeKind == Kind.ARRAY_ACCESS ||
               treeKind == Kind.MEMBER_SELECT);

        ExpressionTree expr = treeKind == Kind.ARRAY_ACCESS ?
            ((ArrayAccessTree) tree).getExpression() :
            ((MemberSelectTree) tree).getExpression();

        Element invokedElement = TreeUtils.elementFromUse(expr);

        AnnotatedTypeMirror receiverAtm = atypeFactory.getReceiverType(tree);

        if (expr != null && invokedElement != null && receiverAtm != null) {
            checkPreconditions(expr, invokedElement, expr.getKind() == Tree.Kind.METHOD_INVOCATION, getPreconditions(receiverAtm));
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
     * if the the current path is within the expression
     * of a synchronized block (e.g. bar in
     * synchronized(bar){ ... }
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
                        // void foo(@GuardedBy("bar") myClass this){ synchronized(bar){ ... }}

                        // Also avoid issuing a warning in this scenario:
                        // @GuardedBy("bar") Object bar;
                        // ...
                        // synchronized(bar){ ... }

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
            // errors are reported at declaration site
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

}
