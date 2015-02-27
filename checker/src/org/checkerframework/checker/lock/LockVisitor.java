package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

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

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

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

    public LockVisitor(BaseTypeChecker checker) {
        super(checker);

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
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey,
            boolean isLocalVariableAssignement) {

        if (valueType.getKind() == TypeKind.NULL) {
            // Avoid issuing warnings about 'null' not matching the type of the variable.
            return;
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignement);
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
}
