package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.interning.qual.*;
*/

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

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
    protected final AnnotationMirror LOCKHELD, LOCKPOSSIBLYHELD, SIDEEFFECTFREE, GUARDEDBY, JCIPGUARDEDBY, JAVAXGUARDEDBY, GUARDSATISFIED;

    private final Class<? extends Annotation> checkerHoldingClass = Holding.class;

    // Note that Javax and JCIP @GuardedBy is used on both methods and objects. For methods they are
    // equivalent to the Checker Framework @Holding annotation.
    private final Class<? extends Annotation> javaxGuardedByClass = javax.annotation.concurrent.GuardedBy.class;
    private final Class<? extends Annotation> jcipGuardedByClass = net.jcip.annotations.GuardedBy.class;

    private final Class<? extends Annotation> checkerLockingFreeClass = LockingFree.class;
    private final Class<? extends Annotation> checkerReleasesNoLocksClass = ReleasesNoLocks.class;
    private final Class<? extends Annotation> checkerMayReleaseLocksClass = MayReleaseLocks.class;
    private final Class<? extends Annotation> sideEffectFreeClass = SideEffectFree.class;
    private final Class<? extends Annotation> pureClass = Pure.class;

    public LockAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker, useFlow);

        LOCKHELD = AnnotationUtils.fromClass(elements, LockHeld.class);
        LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(elements, LockPossiblyHeld.class);
        SIDEEFFECTFREE = AnnotationUtils.fromClass(elements, SideEffectFree.class);
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        JCIPGUARDEDBY = AnnotationUtils.fromClass(elements, net.jcip.annotations.GuardedBy.class);
        JAVAXGUARDEDBY = AnnotationUtils.fromClass(elements, javax.annotation.concurrent.GuardedBy.class);
        GUARDSATISFIED = AnnotationUtils.fromClass(elements, GuardSatisfied.class);

        addAliasedAnnotation(javax.annotation.concurrent.GuardedBy.class, GUARDEDBY);
        addAliasedAnnotation(net.jcip.annotations.GuardedBy.class, GUARDEDBY);

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
    protected void postInit() {
        super.postInit();

        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                MayReleaseLocks.class));
        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                ReleasesNoLocks.class));
        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                LockingFree.class));
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

    protected AnnotationMirror getDeclAnnotationNoAliases(Element elt,
            Class<? extends Annotation> anno) {
        String annoName = anno.getCanonicalName().intern();
        return getDeclAnnotation(elt, annoName, false);
    }

    protected QualifierDefaults getQualifierDefaults() {
        return defaults;
    }

    class LockQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public LockQualifierHierarchy(MultiGraphFactory f) {
            super(f, LOCKHELD);
        }

        private boolean isGuardedBy(AnnotationMirror am) {
            return AnnotationUtils.areSameIgnoringValues(am, GUARDEDBY) ||
                   AnnotationUtils.areSameIgnoringValues(am, JAVAXGUARDEDBY) ||
                   AnnotationUtils.areSameIgnoringValues(am, JCIPGUARDEDBY);
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

            // Remove values from @GuardedBy annotations (and use the Checker Framework's GuardedBy annotation, not JCIP's or Javax's)
            // for further subtype checking.

            return super.isSubtype(rhsIsGuardedBy ? GUARDEDBY : rhs, lhsIsGuardedBy ? GUARDEDBY : lhs);
        }
    }

    // Returns the list of expressions specified in the @Holding (or equivalent) annotation on a method.
    // Returns an empty list if such an annotation is not present.
    protected List<String> methodHolding(ExecutableElement element) {
        AnnotationMirror holding = getDeclAnnotation(element, checkerHoldingClass);
        AnnotationMirror guardedBy
            = getDeclAnnotation(element, jcipGuardedByClass);
        AnnotationMirror guardedByJavax
            = getDeclAnnotation(element, javaxGuardedByClass);

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

    // The side effect annotations processed by the Lock Checker,
    // in order of increasingly strong guarantees.
    enum SideEffectAnnotation {
        MAYRELEASELOCKS,
        RELEASESNOLOCKS,
        LOCKINGFREE,
        SIDEEFFECTFREE,
        PURE
    }

    // Indicates which side effect annotation is present on the given method.
    // If more than one annotation is present, this method issues an error (if issueErrorIfMoreThanOnePresent is true)
    // and returns the annotation providing the weakest guarantee.
    // If no annotation is present, return RELEASESNOLOCKS as the default, and MAYRELEASELOCKS
    // as the default for unannotated code.
    SideEffectAnnotation methodSideEffectAnnotation(Element element, boolean issueErrorIfMoreThanOnePresent) {
        if (element != null) {
            final int countSideEffectAnnotations = SideEffectAnnotation.values().length;

            boolean[] sideEffectAnnotationPresent = new boolean[countSideEffectAnnotations];

            sideEffectAnnotationPresent[0] = getDeclAnnotationNoAliases(element, checkerMayReleaseLocksClass) != null;
            sideEffectAnnotationPresent[1] = getDeclAnnotationNoAliases(element, checkerReleasesNoLocksClass) != null;
            sideEffectAnnotationPresent[2] = getDeclAnnotationNoAliases(element, checkerLockingFreeClass) != null;
            sideEffectAnnotationPresent[3] = getDeclAnnotationNoAliases(element, sideEffectFreeClass) != null;
            sideEffectAnnotationPresent[4] = getDeclAnnotationNoAliases(element, pureClass) != null;
            assert(countSideEffectAnnotations == 5); // If this assertion fails, the assignments above need to be updated.

            int count = 0;

            for(int i = 0; i < countSideEffectAnnotations; i++) {
                if (sideEffectAnnotationPresent[i]) {
                    count++;
                }
            }

            if (count == 0) {
                return getQualifierDefaults().applyUnannotatedDefaults(element) ?
                    SideEffectAnnotation.MAYRELEASELOCKS :
                    SideEffectAnnotation.RELEASESNOLOCKS;
            }

            if (count > 1 && issueErrorIfMoreThanOnePresent) {
                // TODO: Turn on after figuring out how this interacts with inherited annotations.
                // checker.report(Result.failure("multiple.sideeffect.annotations"), element);
            }

            // If at least one side effect annotation was found, return the weakest.
            for(int i = 0; i < countSideEffectAnnotations; i++) {
                if (sideEffectAnnotationPresent[i]) {
                    return SideEffectAnnotation.values()[i];
                }
            }
        }

        // When there is not enough information to determine the correct side effect annotation,
        // return the weakest one.
        return SideEffectAnnotation.MAYRELEASELOCKS;
    }

}
