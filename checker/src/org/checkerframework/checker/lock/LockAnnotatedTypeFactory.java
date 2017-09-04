package org.checkerframework.checker.lock;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;

/**
 * LockAnnotatedTypeFactory builds types with @LockHeld and @LockPossiblyHeld annotations. LockHeld
 * identifies that an object is being used as a lock and is being held when a given tree is
 * executed. LockPossiblyHeld is the default type qualifier for this hierarchy and applies to all
 * fields, local variables and parameters -- hence it does not convey any information other than
 * that it is not LockHeld.
 *
 * <p>However, there are a number of other annotations used in conjunction with these annotations to
 * enforce proper locking.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
public class LockAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<CFValue, LockStore, LockTransfer, LockAnalysis> {

    /** dependent type annotation error message for when the expression is not effectively final. */
    public static final String NOT_EFFECTIVELY_FINAL = "lock expression is not effectively final";

    /** Annotation constants */
    protected final AnnotationMirror LOCKHELD,
            LOCKPOSSIBLYHELD,
            SIDEEFFECTFREE,
            GUARDEDBYUNKNOWN,
            GUARDEDBY,
            GUARDEDBYBOTTOM,
            GUARDSATISFIED;

    public LockAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        LOCKHELD = AnnotationUtils.fromClass(elements, LockHeld.class);
        LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(elements, LockPossiblyHeld.class);
        SIDEEFFECTFREE = AnnotationUtils.fromClass(elements, SideEffectFree.class);
        GUARDEDBYUNKNOWN = AnnotationUtils.fromClass(elements, GuardedByUnknown.class);
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        GUARDEDBYBOTTOM = AnnotationUtils.fromClass(elements, GuardedByBottom.class);
        GUARDSATISFIED = AnnotationUtils.fromClass(elements, GuardSatisfied.class);

        // This alias is only true for the Lock Checker. All other checkers must
        // ignore the @LockingFree annotation.
        addAliasedDeclAnnotation(LockingFree.class, SideEffectFree.class, SIDEEFFECTFREE);

        // This alias is only true for the Lock Checker. All other checkers must
        // ignore the @ReleasesNoLocks annotation.  Note that ReleasesNoLocks is
        // not truly side-effect-free even as far as the Lock Checker is concerned,
        // so there is additional handling of this annotation in the Lock Checker.
        addAliasedDeclAnnotation(ReleasesNoLocks.class, SideEffectFree.class, SIDEEFFECTFREE);

        postInit();
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new DependentTypesHelper(this) {
            @Override
            protected void reportErrors(Tree errorTree, List<DependentTypesError> errors) {
                // If the error message is NOT_EFFECTIVELY_FINAL, then report lock.expression.not
                // .final instead of an expression.unparsable.type.invalid error.
                List<DependentTypesError> superErrors = new ArrayList<>();
                for (DependentTypesError error : errors) {
                    if (error.error.equals(NOT_EFFECTIVELY_FINAL)) {
                        checker.report(
                                Result.failure("lock.expression.not.final", error.expression),
                                errorTree);
                    } else {
                        superErrors.add(error);
                    }
                }
                super.reportErrors(errorTree, superErrors);
            }

            @Override
            protected String standardizeString(
                    String expression,
                    FlowExpressionContext context,
                    TreePath localScope,
                    boolean useLocalScope) {
                if (DependentTypesError.isExpressionError(expression)) {
                    return expression;
                }

                // Adds logic to parse <self> expression, which only the Lock Checker uses.
                if (LockVisitor.selfReceiverPattern.matcher(expression).matches()) {
                    return expression;
                }

                try {
                    FlowExpressions.Receiver result =
                            FlowExpressionParseUtil.parse(
                                    expression, context, localScope, useLocalScope);
                    if (result == null) {
                        return new DependentTypesError(expression, " ").toString();
                    }
                    if (!isExpressionEffectivelyFinal(result)) {
                        // If the expression isn't effectively final, then return the
                        // NOT_EFFECTIVELY_FINAL error string.
                        return new DependentTypesError(expression, NOT_EFFECTIVELY_FINAL)
                                .toString();
                    }
                    return result.toString();
                } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
                    return new DependentTypesError(expression, e).toString();
                }
            }
        };
    }

    /**
     * Returns whether or not the expression is effectively final.
     *
     * <p>This method returns true in the following cases when expr is:
     *
     * <p>1. a field access and the field is final and the field access expression is effectively
     * final as specified by this method.
     *
     * <p>2. an effectively final local variable.
     *
     * <p>3. a deterministic method call whose arguments and receiver expression are effectively
     * final as specified by this method.
     *
     * <p>4. a this reference or a class literal
     *
     * @param expr expression
     * @return whether or not the expression is effectively final
     */
    boolean isExpressionEffectivelyFinal(Receiver expr) {
        if (expr instanceof FieldAccess) {
            FieldAccess fieldAccess = (FieldAccess) expr;
            Receiver recv = fieldAccess.getReceiver();
            // Don't call fieldAccess
            return fieldAccess.isFinal() && isExpressionEffectivelyFinal(recv);
        } else if (expr instanceof LocalVariable) {
            return ElementUtils.isEffectivelyFinal(((LocalVariable) expr).getElement());
        } else if (expr instanceof MethodCall) {
            MethodCall methodCall = (MethodCall) expr;
            for (Receiver param : methodCall.getParameters()) {
                if (!isExpressionEffectivelyFinal(param)) {
                    return false;
                }
            }
            return PurityUtils.isDeterministic(this, methodCall.getElement())
                    && isExpressionEffectivelyFinal(methodCall.getReceiver());
        } else if (expr instanceof ThisReference || expr instanceof ClassName) {
            // this is always final. "ClassName" is actually a class literal (String.class), it's
            // final too.
            return true;
        } else { // type of 'expr' is not supported in @GuardedBy(...) lock expressions
            return false;
        }
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        LockHeld.class,
                        LockPossiblyHeld.class,
                        GuardedBy.class,
                        GuardedByUnknown.class,
                        GuardSatisfied.class,
                        GuardedByBottom.class));
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
    public LockTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, LockStore, LockTransfer> analysis) {
        return new LockTransfer((LockAnalysis) analysis, (LockChecker) this.checker);
    }

    class LockQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public LockQualifierHierarchy(MultiGraphFactory f) {
            super(f, LOCKHELD);
        }

        boolean isGuardedBy(AnnotationMirror am) {
            return AnnotationUtils.areSameIgnoringValues(am, GUARDEDBY);
        }

        boolean isGuardSatisfied(AnnotationMirror am) {
            return AnnotationUtils.areSameIgnoringValues(am, GUARDSATISFIED);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {

            boolean lhsIsGuardedBy = isGuardedBy(superAnno);
            boolean rhsIsGuardedBy = isGuardedBy(subAnno);

            if (lhsIsGuardedBy && rhsIsGuardedBy) {
                // Two @GuardedBy annotations are considered subtypes of each other if and only if their values match exactly.

                List<String> lhsValues =
                        AnnotationUtils.getElementValueArray(
                                superAnno, "value", String.class, true);
                List<String> rhsValues =
                        AnnotationUtils.getElementValueArray(subAnno, "value", String.class, true);

                return rhsValues.containsAll(lhsValues) && lhsValues.containsAll(rhsValues);
            }

            boolean lhsIsGuardSatisfied = isGuardSatisfied(superAnno);
            boolean rhsIsGuardSatisfied = isGuardSatisfied(subAnno);

            if (lhsIsGuardSatisfied && rhsIsGuardSatisfied) {
                // There are cases in which two expressions with identical @GuardSatisfied(...) annotations are not
                // assignable. Those are handled elsewhere.

                // Two expressions with @GuardSatisfied annotations (without an index) are sometimes not assignable.
                // For example, two method actual parameters with @GuardSatisfied annotations are assumed to refer to different guards.

                // This is largely handled in methodFromUse and in LockVisitor.visitMethodInvocation.
                // Related behavior is handled in LockVisitor.visitMethod (issuing an error if a non-constructor method
                // definition has a return type of @GuardSatisfied without an index).

                // Two expressions with @GuardSatisfied() annotations are assignable when comparing a formal receiver
                // to an actual receiver (see LockVisitor.skipReceiverSubtypeCheck) or a formal parameter to an
                // actual parameter (see LockVisitor.commonAssignmentCheck for the details on this rule).

                return AnnotationUtils.areSame(superAnno, subAnno);
            }

            // Remove values from @GuardedBy annotations for further subtype checking. Remove indices from @GuardSatisfied annotations.

            if (lhsIsGuardedBy) {
                superAnno = GUARDEDBY;
            } else if (lhsIsGuardSatisfied) {
                superAnno = GUARDSATISFIED;
            }

            if (rhsIsGuardedBy) {
                subAnno = GUARDEDBY;
            } else if (rhsIsGuardSatisfied) {
                subAnno = GUARDSATISFIED;
            }

            return super.isSubtype(subAnno, superAnno);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            AnnotationMirror a1top = getTopAnnotation(a1);
            AnnotationMirror a2top = getTopAnnotation(a2);

            if (AnnotationUtils.areSame(a1top, LOCKPOSSIBLYHELD)
                    && AnnotationUtils.areSame(a2top, LOCKPOSSIBLYHELD)) {
                return greatestLowerBoundInLockPossiblyHeldHierarchy(a1, a2);
            } else if (AnnotationUtils.areSame(a1top, GUARDEDBYUNKNOWN)
                    && AnnotationUtils.areSame(a2top, GUARDEDBYUNKNOWN)) {
                return greatestLowerBoundInGuardedByUnknownHierarchy(a1, a2);
            }

            return null;
        }

        private AnnotationMirror greatestLowerBoundInGuardedByUnknownHierarchy(
                AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, GUARDEDBYUNKNOWN)) {
                return a2;
            }

            if (AnnotationUtils.areSame(a2, GUARDEDBYUNKNOWN)) {
                return a1;
            }

            if ((isGuardedBy(a1) && isGuardedBy(a2))
                    || (isGuardSatisfied(a1) && isGuardSatisfied(a2))) {
                // isSubtype(a1, a2) is symmetrical to isSubtype(a2, a1) since two
                // @GuardedBy annotations are considered subtypes of each other
                // if and only if their values match exactly, and two @GuardSatisfied
                // annotations are considered subtypes of each other if and only if
                // their indices match exactly.

                if (isSubtype(a1, a2)) {
                    return a1;
                }
            }

            return GUARDEDBYBOTTOM;
        }

        private AnnotationMirror greatestLowerBoundInLockPossiblyHeldHierarchy(
                AnnotationMirror a1, AnnotationMirror a2) {
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
        final Class<? extends Annotation> annotationClass;

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
         * Returns true if the receiver side effect annotation is weaker than side effect annotation
         * 'other'.
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

    /**
     * Indicates which side effect annotation is present on the given method. If more than one
     * annotation is present, this method issues an error (if issueErrorIfMoreThanOnePresent is
     * true) and returns the annotation providing the weakest guarantee. Only call with
     * issueErrorIfMoreThanOnePresent == true when visiting a method definition. This prevents
     * multiple errors being issued for the same method (as would occur if
     * issueErrorIfMoreThanOnePresent were set to true when visiting method invocations). If no
     * annotation is present, return RELEASESNOLOCKS as the default, and MAYRELEASELOCKS as the
     * default for unchecked code.
     *
     * @param element the method element
     * @param issueErrorIfMoreThanOnePresent whether to issue an error if more than one side effect
     *     annotation is present on the method
     */
    // package-private
    SideEffectAnnotation methodSideEffectAnnotation(
            Element element, boolean issueErrorIfMoreThanOnePresent) {
        if (element != null) {
            List<SideEffectAnnotation> sideEffectAnnotationPresent = new ArrayList<>();
            for (SideEffectAnnotation sea : SideEffectAnnotation.values()) {
                if (getDeclAnnotationNoAliases(element, sea.getAnnotationClass()) != null) {
                    sideEffectAnnotationPresent.add(sea);
                }
            }

            int count = sideEffectAnnotationPresent.size();

            if (count == 0) {
                return defaults.applyUncheckedCodeDefaults(element)
                        ? SideEffectAnnotation.MAYRELEASELOCKS
                        : SideEffectAnnotation.RELEASESNOLOCKS;
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

    /**
     * Returns the index on the GuardSatisfied annotation in the given AnnotatedTypeMirror. Assumes
     * atm is non-null and contains a GuardSatisfied annotation.
     *
     * @param atm AnnotatedTypeMirror containing a GuardSatisfied annotation
     * @return the index on the GuardSatisfied annotation
     */
    // package-private
    int getGuardSatisfiedIndex(AnnotatedTypeMirror atm) {
        return getGuardSatisfiedIndex(atm.getAnnotation(GuardSatisfied.class));
    }

    /**
     * Returns the index on the given GuardSatisfied annotation. Assumes am is non-null and is a
     * GuardSatisfied annotation.
     *
     * @param am AnnotationMirror for a GuardSatisfied annotation
     * @return the index on the GuardSatisfied annotation
     */
    // package-private
    int getGuardSatisfiedIndex(AnnotationMirror am) {
        return AnnotationUtils.getElementValue(am, "value", Integer.class, true);
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =
                super.methodFromUse(tree, methodElt, receiverType);

        if (tree.getKind() != Kind.METHOD_INVOCATION) {
            return mfuPair;
        }

        // If a method's formal return type is annotated with @GuardSatisfied(index),
        // look for the first instance of @GuardSatisfied(index) in the method definition's receiver type or
        // formal parameters, retrieve the corresponding type of the actual parameter / receiver at the call site
        // (e.g. @GuardedBy("someLock") and replace the return type at the call site with this type.

        AnnotatedExecutableType invokedMethod = mfuPair.first;

        if (invokedMethod.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            return mfuPair;
        }

        AnnotatedTypeMirror methodDefinitionReturn = invokedMethod.getReturnType();

        if (methodDefinitionReturn == null
                || !methodDefinitionReturn.hasAnnotation(GuardSatisfied.class)) {
            return mfuPair;
        }

        int returnGuardSatisfiedIndex = getGuardSatisfiedIndex(methodDefinitionReturn);

        // @GuardSatisfied with no index defaults to index -1. Ignore instances of @GuardSatisfied with no index.
        // If a method is defined with a return type of @GuardSatisfied with no index, an error is reported by LockVisitor.visitMethod.

        if (returnGuardSatisfiedIndex == -1) {
            return mfuPair;
        }

        // Find the receiver or first parameter whose @GS index matches that of the return type.
        // Ensuring that the type annotations on distinct @GS parameters with the same index
        // match at the call site is handled in LockVisitor.visitMethodInvocation

        if (!ElementUtils.isStatic(invokedMethod.getElement())
                && replaceAnnotationInGuardedByHierarchyIfGuardSatisfiedIndexMatches(
                        methodDefinitionReturn,
                        invokedMethod.getReceiverType() /* the method definition receiver*/,
                        returnGuardSatisfiedIndex,
                        receiverType.getAnnotationInHierarchy(GUARDEDBYUNKNOWN))) {
            return mfuPair;
        }

        List<? extends ExpressionTree> methodInvocationTreeArguments =
                ((MethodInvocationTree) tree).getArguments();
        List<AnnotatedTypeMirror> requiredArgs =
                AnnotatedTypes.expandVarArgs(this, invokedMethod, methodInvocationTreeArguments);

        for (int i = 0; i < requiredArgs.size(); i++) {
            if (replaceAnnotationInGuardedByHierarchyIfGuardSatisfiedIndexMatches(
                    methodDefinitionReturn,
                    requiredArgs.get(i),
                    returnGuardSatisfiedIndex,
                    getAnnotatedType(methodInvocationTreeArguments.get(i))
                            .getEffectiveAnnotationInHierarchy(GUARDEDBYUNKNOWN))) {
                return mfuPair;
            }
        }

        return mfuPair;
    }

    /**
     * If {@code atm} is not null and contains a {@code @GuardSatisfied} annotation, and if the
     * index of this {@code @GuardSatisfied} annotation matches {@code matchingGuardSatisfiedIndex},
     * then {@code methodReturnAtm} will have its annotation in the {@code @GuardedBy} hierarchy
     * replaced with that in {@code atmWithAnnotationInGuardedByHierarchy}.
     *
     * @param methodReturnAtm the AnnotatedTypeMirror for the return type of a method that will
     *     potentially have its annotation in the {@code @GuardedBy} hierarchy replaced.
     * @param atm an AnnotatedTypeMirror that may contain a {@code @GuardSatisfied} annotation. May
     *     be null.
     * @param matchingGuardSatisfiedIndex the {code @GuardSatisfied} index that the
     *     {@code @GuardSatisfied} annotation in {@code atm} must have in order for the replacement
     *     to occur.
     * @param annotationInGuardedByHierarchy if the replacement occurs, the annotation in the
     *     {@code @GuardedBy} hierarchy in this parameter will be used for the replacement.
     * @return true if the replacement occurred, false otherwise
     */
    private boolean replaceAnnotationInGuardedByHierarchyIfGuardSatisfiedIndexMatches(
            AnnotatedTypeMirror methodReturnAtm,
            AnnotatedTypeMirror atm,
            int matchingGuardSatisfiedIndex,
            AnnotationMirror annotationInGuardedByHierarchy) {
        if (atm == null
                || !atm.hasAnnotation(GuardSatisfied.class)
                || getGuardSatisfiedIndex(atm) != matchingGuardSatisfiedIndex) {
            return false;
        }

        methodReturnAtm.replaceAnnotation(annotationInGuardedByHierarchy);

        return true;
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new LockTreeAnnotator(this), super.createTreeAnnotator());
    }

    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        translateJcipAndJavaxAnnotations(elt, type);

        super.addComputedTypeAnnotations(elt, type);
    }

    @Override
    public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
        if (tree.getKind() == Tree.Kind.VARIABLE) {
            translateJcipAndJavaxAnnotations(InternalUtils.symbol((VariableTree) tree), type);
        }

        super.addComputedTypeAnnotations(tree, type, useFlow);
    }

    /**
     * Given a field declaration with a {@code @net.jcip.annotations.GuardedBy} or {@code
     * javax.annotation.concurrent.GuardedBy} annotation and an AnnotatedTypeMirror for that field,
     * inserts the corresponding {@code @org.checkerframework.checker.lock.qual.GuardedBy} type
     * qualifier into that AnnotatedTypeMirror.
     *
     * @param element any Element (this method does nothing if the Element is not for a field
     *     declaration)
     * @param atm the AnnotatedTypeMirror for element - the {@code @GuardedBy} type qualifier will
     *     be inserted here
     */
    private void translateJcipAndJavaxAnnotations(Element element, AnnotatedTypeMirror atm) {
        if (!element.getKind().isField()) {
            return;
        }

        AnnotationMirror anno = getDeclAnnotation(element, net.jcip.annotations.GuardedBy.class);

        if (anno == null) {
            anno = getDeclAnnotation(element, javax.annotation.concurrent.GuardedBy.class);
        }

        if (anno == null) {
            return;
        }

        List<String> lockExpressions =
                AnnotationUtils.getElementValueArray(anno, "value", String.class, true);

        if (lockExpressions.isEmpty()) {
            atm.addAnnotation(GUARDEDBY);
        } else {
            atm.addAnnotation(createGuardedByAnnotationMirror(lockExpressions));
        }
    }

    /**
     * @param values a list of lock expressions
     * @return an AnnotationMirror corresponding to @GuardedBy(values)
     */
    private AnnotationMirror createGuardedByAnnotationMirror(List<String> values) {
        AnnotationBuilder builder = new AnnotationBuilder(getProcessingEnv(), GuardedBy.class);
        builder.setValue("value", values.toArray());

        // Return the resulting AnnotationMirror
        return builder.build();
    }
}
