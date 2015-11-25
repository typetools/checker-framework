package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.interning.qual.*;
*/

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
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
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
    protected final AnnotationMirror LOCKHELD, LOCKPOSSIBLYHELD, SIDEEFFECTFREE, GUARDEDBYINACCESSIBLE, GUARDEDBY, JCIPGUARDEDBY, JAVAXGUARDEDBY, GUARDSATISFIED;

    public LockAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        LOCKHELD = AnnotationUtils.fromClass(elements, LockHeld.class);
        LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(elements, LockPossiblyHeld.class);
        SIDEEFFECTFREE = AnnotationUtils.fromClass(elements, SideEffectFree.class);
        GUARDEDBYINACCESSIBLE = AnnotationUtils.fromClass(elements, GuardedByInaccessible.class);
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

            boolean lhsIsGuardSatisfied = AnnotationUtils.areSameIgnoringValues(lhs, GUARDSATISFIED);
            boolean rhsIsGuardSatisfied = AnnotationUtils.areSameIgnoringValues(rhs, GUARDSATISFIED);

            if (lhsIsGuardSatisfied && rhsIsGuardSatisfied) {
                // Two @GuardSatisfied annotations are considered subtypes of each other if and only if their indices match exactly.

                int lhsIndex =
                        AnnotationUtils.getElementValue(lhs, "value", Integer.class, true);
                int rhsIndex =
                    AnnotationUtils.getElementValue(rhs, "value", Integer.class, true);

                return lhsIndex == rhsIndex;
            }

            // Remove values from @GuardedBy annotations (and use the Checker Framework's GuardedBy annotation, not JCIP's or Javax's)
            // for further subtype checking. Remove indices from @GuardSatisfied annotations.

            if (lhsIsGuardedBy) {
                lhs = GUARDEDBY;
            }
            else if (lhsIsGuardSatisfied) {
                lhs = GUARDSATISFIED;
            }

            if (rhsIsGuardedBy) {
                rhs = GUARDEDBY;
            }
            else if (rhsIsGuardSatisfied) {
                rhs = GUARDSATISFIED;
            }

            return super.isSubtype(rhs, lhs);
        }

        // For caching results of glbs
        private Map<AnnotationPair, AnnotationMirror> glbs = null;

        // Same contents as in AnnotatedTypeFactory.java
        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.areSameIgnoringValues(a1, a2))
                return AnnotationUtils.areSame(a1, a2) ? a1 : getBottomAnnotation(a1);
            if (glbs == null) {
                glbs = calculateGlbs();
            }
            AnnotationPair pair = new AnnotationPair(a1, a2);
            return glbs.get(pair);
        }

        // Same contents as in AnnotatedTypeFactory.java
        private Map<AnnotationPair, AnnotationMirror>  calculateGlbs() {
            Map<AnnotationPair, AnnotationMirror> newglbs = new HashMap<AnnotationPair, AnnotationMirror>();
            for (AnnotationMirror a1 : supertypesGraph.keySet()) {
                for (AnnotationMirror a2 : supertypesGraph.keySet()) {
                    if (AnnotationUtils.areSameIgnoringValues(a1, a2))
                        continue;
                    if (!AnnotationUtils.areSame(getTopAnnotation(a1), getTopAnnotation(a2)))
                        continue;
                    AnnotationPair pair = new AnnotationPair(a1, a2);
                    if (newglbs.containsKey(pair))
                        continue;
                    AnnotationMirror glb = findGlb(a1, a2);
                    newglbs.put(pair, glb);
                }
            }
            return newglbs;
        }

        // Same contents as in AnnotatedTypeFactory.java except for this line:
        // if (isSubtype(a1Sub, a1) && !((isGuardedBy(a1Sub) && isGuardedBy(a1)) || a1Sub.equals(a1))) {
        private AnnotationMirror findGlb(AnnotationMirror a1, AnnotationMirror a2) {
            if (isSubtype(a1, a2))
                return a1;
            if (isSubtype(a2, a1))
                return a2;

            assert getTopAnnotation(a1) == getTopAnnotation(a2) :
                "MultiGraphQualifierHierarchy.findGlb: this method may only be called " +
                    "with qualifiers from the same hierarchy. Found a1: " + a1 + " [top: " + getTopAnnotation(a1) +
                    "], a2: " + a2 + " [top: " + getTopAnnotation(a2) + "]";

            Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
            for (AnnotationMirror a1Sub : supertypesGraph.keySet()) {
                if (isSubtype(a1Sub, a1) && !((isGuardedBy(a1Sub) && isGuardedBy(a1)) || a1Sub.equals(a1))) {
                    AnnotationMirror a1lb = findGlb(a1Sub, a2);
                    if (a1lb != null)
                        outset.add(a1lb);
                }
            }
            if (outset.size() == 1) {
                return outset.iterator().next();
            }
            if (outset.size() > 1) {
                outset = findGreatestTypes(outset);
                // TODO: more than one, incomparable subtypes. Pick the first one.
                // if (outset.size()>1) { System.out.println("Still more than one GLB!"); }
                return outset.iterator().next();
            }

            ErrorReporter.errorAbort("MultiGraphQualifierHierarchy could not determine GLB for " + a1 + " and " + a2 +
                    ". Please ensure that the checker knows about all type qualifiers.");
            return null;
        }

        // Same contents as in AnnotatedTypeFactory.java
        // remove all subtypes of elements contained in the set
        private Set<AnnotationMirror> findGreatestTypes(Set<AnnotationMirror> inset) {
            Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
            outset.addAll(inset);

            for (AnnotationMirror a1 : inset) {
                Iterator<AnnotationMirror> outit = outset.iterator();
                while (outit.hasNext()) {
                    AnnotationMirror a2 = outit.next();
                    if (a1 != a2 && isSubtype(a2, a1)) {
                        outit.remove();
                    }
                }
            }
            return outset;
        }

    }

    // The side effect annotations processed by the Lock Checker.
    enum SideEffectAnnotation {
        MAYRELEASELOCKS("@MayReleaseLocks", MayReleaseLocks.class),
        RELEASESNOLOCKS("@ReleasesNoLocks", ReleasesNoLocks.class),
        LOCKINGFREE("@LockingFree",LockingFree.class),
        SIDEEFFECTFREE("@SideEffectFree",SideEffectFree.class),
        PURE("@Pure",Pure.class);
        final String annotation;
        final  Class<? extends Annotation> annotationClass;

        SideEffectAnnotation(String annotation, Class<? extends Annotation> annotationClass) {
            this.annotation = annotation;
            this.annotationClass=annotationClass;
        }

        public String getNameOfSideEffectAnnotation() {
            return annotation;
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return annotationClass;
        }

        /**
         * Given side effect annotations a and b, returns true if a
         * is a strictly weaker side effect annotation than b.
         */
        boolean isWeaker(SideEffectAnnotation other) {
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
            if(weakest == null) {
                for (SideEffectAnnotation sea : SideEffectAnnotation.values()) {
                    if (weakest == null) weakest = sea;
                    if (sea.isWeaker(weakest)) {
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
    // If no annotation is present, return RELEASESNOLOCKS as the default, and MAYRELEASELOCKS
    // as the default for unannotated code.
    SideEffectAnnotation methodSideEffectAnnotation(Element element, boolean issueErrorIfMoreThanOnePresent) {
        if (element != null) {
            final int countSideEffectAnnotations = SideEffectAnnotation.values().length;

            List<SideEffectAnnotation> sideEffectAnnotationPresent = new ArrayList<>();
            for (SideEffectAnnotation sea:SideEffectAnnotation.values()){
                if(getDeclAnnotationNoAliases(element, sea.getAnnotationClass()) != null){
                    sideEffectAnnotationPresent.add(sea);
                }
            }

            int count = sideEffectAnnotationPresent.size();

            if (count == 0) {
                return defaults.applyUnannotatedDefaults(element) ?
                    SideEffectAnnotation.MAYRELEASELOCKS :
                    SideEffectAnnotation.RELEASESNOLOCKS;
            }

            if (count > 1 && issueErrorIfMoreThanOnePresent) {
                // TODO: Turn on after figuring out how this interacts with inherited annotations.
                // checker.report(Result.failure("multiple.sideeffect.annotations"), element);
            }

            SideEffectAnnotation weakest = sideEffectAnnotationPresent.get(0);
            // At least one side effect annotation was found. Return the weakest.
            for(SideEffectAnnotation sea :sideEffectAnnotationPresent) {
                if (sea.isWeaker(weakest)) {
                    weakest = sea;
                }
            }
            return weakest;
        }

        // When there is not enough information to determine the correct side effect annotation,
        // return the weakest one.
        return SideEffectAnnotation.weakest();
    }

    private static class AnnotationPair { // Same contents as in AnnotatedTypeFactory.java
        public final AnnotationMirror a1;
        public final AnnotationMirror a2;
        private int hashCode = -1;

        public AnnotationPair(AnnotationMirror a1, AnnotationMirror a2) {
            this.a1 = a1;
            this.a2 = a2;
        }

        @Pure
        @Override
        public int hashCode() {
            if (hashCode == -1) {
                hashCode = 31;
                if (a1 != null)
                    hashCode += 17 * AnnotationUtils.annotationName(a1).toString().hashCode();
                if (a2 != null)
                    hashCode += 17 * AnnotationUtils.annotationName(a2).toString().hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AnnotationPair))
                return false;
            AnnotationPair other = (AnnotationPair)o;
            if (AnnotationUtils.areSameIgnoringValues(a1, other.a1)
                    && AnnotationUtils.areSameIgnoringValues(a2, other.a2))
                return true;
            if (AnnotationUtils.areSameIgnoringValues(a2, other.a1)
                    && AnnotationUtils.areSameIgnoringValues(a1, other.a2))
                return true;
            return false;
        }

        @SideEffectFree
        @Override
        public String toString() {
            return "AnnotationPair(" + a1 + ", " + a2 + ")";
        }
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type,
            boolean iUseFlow) {
        super.annotateImplicit(tree, type, iUseFlow);
        
        if (tree.getKind() == Kind.METHOD_INVOCATION) {
            // Check that matching @GuardSatisfied(index) on a method's formal return type/parameters matches
            // those in corresponding locations on the method call site.

            MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = methodFromUse(methodInvocationTree);
            AnnotatedExecutableType invokedMethod = mfuPair.first;

            List<AnnotatedTypeMirror> requiredArgs =
                AnnotatedTypes.expandVarArgs(this, invokedMethod, methodInvocationTree.getArguments());

            AnnotatedTypeMirror methodDefinitionReturn = null;

            if (invokedMethod.getElement().getKind() != ElementKind.CONSTRUCTOR) {
                methodDefinitionReturn = invokedMethod.getReturnType().getErased();
                if (methodDefinitionReturn != null && methodDefinitionReturn.hasAnnotation(GuardSatisfied.class)) {
                    int returnGuardSatisfiedIndex = AnnotationUtils.
                            getElementValue(methodDefinitionReturn.getAnnotation(GuardSatisfied.class), "value", Integer.class, true);
                    
                    for (int i = 0; i < requiredArgs.size(); i++) { // TODO: Could need to match the receiver as well.
                        AnnotatedTypeMirror arg = requiredArgs.get(i);

                        if (arg.hasAnnotation(GuardSatisfied.class)) {
                            int paramGuardSatisfiedIndex = AnnotationUtils.getElementValue(arg.getAnnotation(GuardSatisfied.class), "value", Integer.class, true);
                            
                            // Find the first parameter whose @GS index matches that of the .
                            // Ensuring that the type annotations on distinct @GS parameters with the same index match at the call site is handled in LockVisitor.visitMethodInvocation 
                            if (paramGuardSatisfiedIndex == returnGuardSatisfiedIndex) {
                                ExpressionTree argument = methodInvocationTree.getArguments().get(i);
                                type.replaceAnnotation(getAnnotatedType(argument).getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE));
                                break;
                            }
                        }
                    }
                    
                }
            }
        }
    }
}
