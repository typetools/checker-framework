package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
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
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

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
            AnnotatedTypeMirror valueType, Tree valueTree, String errorKey,
            boolean isLocalVariableAssignement) {

        if (valueType instanceof AnnotatedNullType) {
            // Avoid issuing warnings about 'null' not matching the type of the variable.
            return;
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignement);
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
                checker.report(Result.failure("override.holding.invalid.holdingonentry",
                        TreeUtils.elementFromDeclaration(overriderTree),
                        overridden.getElement()), overriderTree);
            } else if (!overriddenHoldingOnEntryLocks.containsAll(overriderHoldingOnEntryLocks)) {
                isValid = false;
                checker.report(Result.failure("override.holding.invalid",
                        TreeUtils.elementFromDeclaration(overriderTree),
                        overridden.getElement(),
                        overriderHoldingOnEntryLocks, overriddenHoldingOnEntryLocks), overriderTree);
            }
        } else {
            if (!overriderLocks.isEmpty()) {
                if (!overriddenLocks.containsAll(overriderLocks)) {
                    isValid = false;
                    checker.report(Result.failure("override.holding.invalid",
                            TreeUtils.elementFromDeclaration(overriderTree),
                            overridden.getElement(),
                            overriderLocks, overriddenLocks), overriderTree);
                }
            } else if (!overriddenLocks.containsAll(overriderHoldingOnEntryLocks)) {
                isValid = false;
                checker.report(Result.failure("override.holding.invalid",
                        TreeUtils.elementFromDeclaration(overriderTree),
                        overridden.getElement(),
                        overriderHoldingOnEntryLocks, overriddenLocks), overriderTree);
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

}
