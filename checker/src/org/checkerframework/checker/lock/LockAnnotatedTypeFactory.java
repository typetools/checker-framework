package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.interning.qual.*;
*/

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByInaccessible;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

// TODO: Here is a list of nice-to-have features/tests but not critical to release the Lock Checker:

// Add an enhancement so that implicit .toString() calls are recognized, i.e. when a non-String object
// is pseudo-assigned to a String variable.

// Add a warning if a user annotates a static field with @GuardedBy("this") instead of @GuardedBy("<class name>.class")

// Add a test that @GuardedBy("<class name>.class") is never ambiguous given two classes with the same name in two different packages.

// Calling a method annotated with @MayReleaseLocks should not always cause local variables' refinement to be reset to @GuardedByInaccessible.
// The current workaround is to explicitly annotate the local variable with the appropriate annotation in the @GuardedBy hierarchy.

// Would it be a useful feature for the Lock Checker to warn about missing unlock calls when there is a call to .lock() in a method?
// Or is this a common pattern to lock in one method and unlock in a different one?

// Issue an error if @GuardSatisfied is written on a location other than a primary annotation.

// TODO: The following bugs are important to fix before the March 1 release of the Checker Framework:
// TODO: Issue a warning if a lock is not final or effectively final. Document in the manual.
//       Nice to have: consider whether @Pure methods make this warning unnecessary in some scenarios.
// TODO: Make sure "itself" is not handled by the flow expression parser for type hierarchies other than @GuardedBy.

/**
 * LockAnnotatedTypeFactory builds types with LockHeld and LockPossiblyHeld annotations.
 * LockHeld identifies that an object is being used as a lock and is being held when a
 * given tree is executed. LockPossiblyHeld is the default type qualifier for this
 * hierarchy and applies to all fields, local variables and parameters - hence it does
 * not convey any information other than that it is not LockHeld.
 *
 * However, there are a number of other annotations used in conjunction with these annotations
 * to enforce proper locking.
 * @checker_framework.manual #lock-checker Lock Checker
 */
public class LockAnnotatedTypeFactory
    extends GenericAnnotatedTypeFactory<CFValue, LockStore, LockTransfer, LockAnalysis> {

    /** Annotation constants */
    protected final AnnotationMirror LOCKHELD, LOCKPOSSIBLYHELD,
        SIDEEFFECTFREE, GUARDEDBYINACCESSIBLE, GUARDEDBY,
        GUARDEDBYBOTTOM, GUARDSATISFIED;

    public LockAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        LOCKHELD = AnnotationUtils.fromClass(elements, LockHeld.class);
        LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(elements, LockPossiblyHeld.class);
        SIDEEFFECTFREE = AnnotationUtils.fromClass(elements, SideEffectFree.class);
        GUARDEDBYINACCESSIBLE = AnnotationUtils.fromClass(elements, GuardedByInaccessible.class);
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        GUARDEDBYBOTTOM = AnnotationUtils.fromClass(elements, GuardedByBottom.class);
        GUARDSATISFIED = AnnotationUtils.fromClass(elements, GuardSatisfied.class);

        // This alias is only true for the Lock Checker. All other checkers must
        // ignore the @LockingFree annotation.
        addAliasedDeclAnnotation(LockingFree.class,
                SideEffectFree.class,
                AnnotationUtils.fromClass(elements, SideEffectFree.class));

        // This alias is only true for the Lock Checker. All other checkers must
        // ignore the @ReleasesNoLocks annotation.  Note that ReleasesNoLocks is
        // not truly side-effect-free even as far as the Lock Checker is concerned,
        // so there is additional handling of this annotation in the Lock Checker.
        addAliasedDeclAnnotation(ReleasesNoLocks.class,
                SideEffectFree.class,
                AnnotationUtils.fromClass(elements, SideEffectFree.class));

        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Collections.unmodifiableSet(
                new HashSet<Class<? extends Annotation>>(
                        Arrays.asList(LockHeld.class, LockPossiblyHeld.class,
                                GuardedBy.class, GuardedByInaccessible.class,
                                GuardSatisfied.class, GuardedByBottom.class)));
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new LockQualifierHierarchy(factory);
    }

    @Override
    protected LockAnalysis createFlowAnalysis(List<Pair<VariableElement, CFValue>> fieldValues) {
        return new LockAnalysis(checker, this, fieldValues);
    }

    @Override
    public LockTransfer createFlowTransferFunction(CFAbstractAnalysis<CFValue, LockStore, LockTransfer> analysis) {
        return new LockTransfer((LockAnalysis) analysis,(LockChecker)this.checker);
    }

    class LockQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public LockQualifierHierarchy(MultiGraphFactory f) {
            super(f, LOCKHELD);
        }

        boolean isGuardedBy(AnnotationMirror am) {
            return AnnotationUtils.areSameIgnoringValues(am, GUARDEDBY);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {

            boolean lhsIsGuardedBy = isGuardedBy(lhs);
            boolean rhsIsGuardedBy = isGuardedBy(rhs);

            if (lhsIsGuardedBy && rhsIsGuardedBy) {
                // Two @GuardedBy annotations are considered subtypes of each other if and only if their values match exactly.

                List<String> lhsValues =
                    AnnotationUtils.getElementValueArray(lhs, "value", String.class, true);
                List<String> rhsValues =
                    AnnotationUtils.getElementValueArray(rhs, "value", String.class, true);

                return rhsValues.containsAll(lhsValues) && lhsValues.containsAll(rhsValues);
            }

            boolean lhsIsGuardSatisfied = AnnotationUtils.areSameIgnoringValues(lhs, GUARDSATISFIED);
            boolean rhsIsGuardSatisfied = AnnotationUtils.areSameIgnoringValues(rhs, GUARDSATISFIED);

            if (lhsIsGuardSatisfied && rhsIsGuardSatisfied) {
                // Two @GuardSatisfied annotations are considered subtypes of each other if and only if their indices match exactly.
                // IMPORTANT: Two @GuardSatisfied annotations (without an index) are sometimes not subtypes of each other.
                // For example, two method actual parameters with @GuardSatisfied annotations are assumed to refer to different guards.

                // This is largely handled in methodFromUse and in LockVisitor.visitMethodInvocation.
                // Related behavior is handled in LockVisitor.visitMethod (issuing an error if a non-constructor method
                // definition has a return type of @GuardSatisfied without an index).

                // Two @GuardSatisfied() annotations are subtypes of each other when comparing a formal receiver
                // to an actual receiver (see LockVisitor.skipReceiverSubtypeCheck) or a formal parameter to an
                // actual parameter (see LockVisitor.commonAssignmentCheck for the details on this rule).

                return AnnotationUtils.areSame(lhs, rhs);
            }

            // Remove values from @GuardedBy annotations for further subtype checking. Remove indices from @GuardSatisfied annotations.

            if (lhsIsGuardedBy) {
                lhs = GUARDEDBY;
            }
            else if (AnnotationUtils.areSameIgnoringValues(lhs, GUARDSATISFIED)) {
                lhs = GUARDSATISFIED;
            }

            if (rhsIsGuardedBy) {
                rhs = GUARDEDBY;
            }
            else if (AnnotationUtils.areSameIgnoringValues(rhs, GUARDSATISFIED)) {
                rhs = GUARDSATISFIED;
            }

            return super.isSubtype(rhs, lhs);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            AnnotationMirror a1top = getTopAnnotation(a1);
            AnnotationMirror a2top = getTopAnnotation(a2);

            if (AnnotationUtils.areSame(a1top, LOCKPOSSIBLYHELD) &&
                AnnotationUtils.areSame(a2top, LOCKPOSSIBLYHELD)) {
                return greatestLowerBoundInLockPossiblyHeldHierarchy(a1, a2);
            } else if (AnnotationUtils.areSame(a1top, GUARDEDBYINACCESSIBLE) &&
                       AnnotationUtils.areSame(a2top, GUARDEDBYINACCESSIBLE)) {
                return greatestLowerBoundInGuardedByInaccessibleHierarchy(a1, a2);
            }

            return null;
        }

        private AnnotationMirror greatestLowerBoundInGuardedByInaccessibleHierarchy(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, GUARDEDBYINACCESSIBLE)) {
                return a2;
            }

            if (AnnotationUtils.areSame(a2, GUARDEDBYINACCESSIBLE)) {
                return a1;
            }

            if (isGuardedBy(a1) && isGuardedBy(a2)) {
                // Two @GuardedBy annotations are considered subtypes of each other if and only if their values match exactly.

                List<String> a1Values =
                    AnnotationUtils.getElementValueArray(a1, "value", String.class, true);
                List<String> a2Values =
                    AnnotationUtils.getElementValueArray(a2, "value", String.class, true);

                if (a2Values.containsAll(a1Values) && a1Values.containsAll(a2Values)) {
                    return a1;
                }
            } else if (AnnotationUtils.areSame(a1, a2)) {
                return a1;
            }

            return GUARDEDBYBOTTOM;
        }

        private AnnotationMirror greatestLowerBoundInLockPossiblyHeldHierarchy(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, LOCKPOSSIBLYHELD)) {
                return a2;
            }

            if (AnnotationUtils.areSame(a2, LOCKPOSSIBLYHELD)) {
                return a1;
            }

            return LOCKHELD;
        }
    }

    // The side effect annotations processed by the Lock Checker.
    enum SideEffectAnnotation {
        MAYRELEASELOCKS("@MayReleaseLocks", MayReleaseLocks.class),
        RELEASESNOLOCKS("@ReleasesNoLocks", ReleasesNoLocks.class),
        LOCKINGFREE("@LockingFree", LockingFree.class),
        SIDEEFFECTFREE("@SideEffectFree", SideEffectFree.class),
        PURE("@Pure", Pure.class);
        final String annotation;
        final  Class<? extends Annotation> annotationClass;

        SideEffectAnnotation(String annotation, Class<? extends Annotation> annotationClass) {
            this.annotation = annotation;
            this.annotationClass = annotationClass;
        }

        public String getNameOfSideEffectAnnotation() {
            return annotation;
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return annotationClass;
        }

        /**
         * Returns true if the receiver side effect annotation is weaker
         * than side effect annotation 'other'.
         */
        boolean isWeakerThan(SideEffectAnnotation other) {
            boolean weaker = false;

            switch (other) {
                case MAYRELEASELOCKS:
                    break;
                case RELEASESNOLOCKS:
                    if (this == SideEffectAnnotation.MAYRELEASELOCKS) {
                        weaker = true;
                    }
                    break;
                case LOCKINGFREE:
                    switch (this) {
                        case MAYRELEASELOCKS:
                        case RELEASESNOLOCKS:
                            weaker = true;
                            break;
                        default:
                    }
                    break;
                case SIDEEFFECTFREE:
                    switch (this) {
                        case MAYRELEASELOCKS:
                        case RELEASESNOLOCKS:
                        case LOCKINGFREE:
                            weaker = true;
                            break;
                        default:
                    }
                    break;
                case PURE:
                    switch (this) {
                        case MAYRELEASELOCKS:
                        case RELEASESNOLOCKS:
                        case LOCKINGFREE:
                        case SIDEEFFECTFREE:
                            weaker = true;
                            break;
                        default:
                    }
                    break;
            }

            return weaker;
        }

        static SideEffectAnnotation weakest = null;
        public static SideEffectAnnotation weakest() {
            if (weakest == null) {
                for (SideEffectAnnotation sea : SideEffectAnnotation.values()) {
                    if (weakest == null) {
                        weakest = sea;
                    }
                    if (sea.isWeakerThan(weakest)) {
                        weakest = sea;
                    }
                }
            }
            return weakest;
        }
    }

    // Indicates which side effect annotation is present on the given method.
    // If more than one annotation is present, this method issues an error (if issueErrorIfMoreThanOnePresent is true)
    // and returns the annotation providing the weakest guarantee.
    // Only call with issueErrorIfMoreThanOnePresent == true when visiting a method definition.
    // This prevents multiple errors being issued for the same method (as would occur if
    // issueErrorIfMoreThanOnePresent were set to true when visiting method invocations).
    // If no annotation is present, return RELEASESNOLOCKS as the default, and MAYRELEASELOCKS
    // as the default for unannotated code.

    // package-private
    SideEffectAnnotation methodSideEffectAnnotation(Element element, boolean issueErrorIfMoreThanOnePresent) {
        if (element != null) {
            List<SideEffectAnnotation> sideEffectAnnotationPresent = new ArrayList<>();
            for (SideEffectAnnotation sea : SideEffectAnnotation.values()){
                if (getDeclAnnotationNoAliases(element, sea.getAnnotationClass()) != null){
                    sideEffectAnnotationPresent.add(sea);
                }
            }

            int count = sideEffectAnnotationPresent.size();

            if (count == 0) {
                return defaults.applyUncheckedCodeDefaults(element) ?
                    SideEffectAnnotation.MAYRELEASELOCKS :
                    SideEffectAnnotation.RELEASESNOLOCKS;
            }

            if (count > 1 && issueErrorIfMoreThanOnePresent) {
                // TODO: Turn on after figuring out how this interacts with inherited annotations.
                // checker.report(Result.failure("multiple.sideeffect.annotations"), element);
            }

            SideEffectAnnotation weakest = sideEffectAnnotationPresent.get(0);
            // At least one side effect annotation was found. Return the weakest.
            for (SideEffectAnnotation sea : sideEffectAnnotationPresent) {
                if (sea.isWeakerThan(weakest)) {
                    weakest = sea;
                }
            }
            return weakest;
        }

        // When there is not enough information to determine the correct side effect annotation,
        // return the weakest one.
        return SideEffectAnnotation.weakest();
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            ExpressionTree tree, ExecutableElement methodElt,
            AnnotatedTypeMirror receiverType) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(tree, methodElt, receiverType);

        if (tree.getKind() == Kind.METHOD_INVOCATION) {
            // If a method's formal return type is annotated with @GuardSatisfied(index),
            // look for the first instance of @GuardSatisfied(index) in the method definition's receiver type or
            // formal parameters, retrieve the corresponding type of the actual parameter / receiver at the call site
            // (e.g. @GuardedBy("someLock") and replace the return type at the call site with this type.

            MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
            AnnotatedExecutableType invokedMethod = mfuPair.first;

            if (invokedMethod.getElement().getKind() != ElementKind.CONSTRUCTOR) {
                AnnotatedTypeMirror methodDefinitionReturn = invokedMethod.getReturnType().getErased();

                if (methodDefinitionReturn != null && methodDefinitionReturn.hasAnnotation(GuardSatisfied.class)) {
                    int returnGuardSatisfiedIndex = AnnotationUtils.
                            getElementValue(methodDefinitionReturn.getAnnotation(GuardSatisfied.class), "value", Integer.class, true);

                    // @GuardSatisfied with no index defaults to index -1. Ignore instances of @GuardSatisfied with no index.
                    // If a method is defined with a return type of @GuardSatisfied with no index, an error is reported by LockVisitor.visitMethod.
                    if (returnGuardSatisfiedIndex != -1) {

                        // Find the receiver or first parameter whose @GS index matches that of the return type.
                        // Ensuring that the type annotations on distinct @GS parameters with the same index match at the call site is handled in LockVisitor.visitMethodInvocation

                        ExecutableElement invokedMethodElement = invokedMethod.getElement();
                        if (!ElementUtils.isStatic(invokedMethodElement) && !TreeUtils.isSuperCall(methodInvocationTree)) {
                            AnnotatedTypeMirror methodDefinitionReceiver = invokedMethod.getReceiverType().getErased();
                            if (methodDefinitionReceiver != null && methodDefinitionReceiver.hasAnnotation(GuardSatisfied.class)) {
                                int receiverGuardSatisfiedIndex = AnnotationUtils.
                                        getElementValue(methodDefinitionReceiver.getAnnotation(GuardSatisfied.class), "value", Integer.class, true);

                                if (receiverGuardSatisfiedIndex == returnGuardSatisfiedIndex) {
                                    mfuPair.first.getReturnType().replaceAnnotation(receiverType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE));
                                    return mfuPair;
                                }
                            }
                        }

                        List<AnnotatedTypeMirror> requiredArgs = AnnotatedTypes.expandVarArgs(this, invokedMethod, methodInvocationTree.getArguments());

                        for (int i = 0; i < requiredArgs.size(); i++) {
                            AnnotatedTypeMirror arg = requiredArgs.get(i);

                            if (arg.hasAnnotation(GuardSatisfied.class)) {
                                int paramGuardSatisfiedIndex = AnnotationUtils.getElementValue(arg.getAnnotation(GuardSatisfied.class), "value", Integer.class, true);

                                if (paramGuardSatisfiedIndex == returnGuardSatisfiedIndex) {
                                    ExpressionTree argument = methodInvocationTree.getArguments().get(i);
                                    mfuPair.first.getReturnType().replaceAnnotation(
                                        getAnnotatedType(argument).getEffectiveAnnotationInHierarchy(GUARDEDBYINACCESSIBLE));
                                    return mfuPair;
                                }
                            }
                        }
                    }
                }
            }
        }

        return mfuPair;
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
               new LockPropagationTreeAnnotator(this),
               new ImplicitsTreeAnnotator(this)
        );
    }

    // Indicates that the result of the operation is a boolean value.
    boolean isBinaryComparisonOperator(Kind opKind) {
        switch(opKind){
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
                return true;
            default:
        }

        return false;
    }


}
