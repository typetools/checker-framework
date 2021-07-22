package org.checkerframework.checker.lock;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.lock.qual.EnsuresLockHeld;
import org.checkerframework.checker.lock.qual.EnsuresLockHeldIf;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.CollectionsPlume;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

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

    /** The @{@link LockHeld} annotation. */
    protected final AnnotationMirror LOCKHELD =
            AnnotationBuilder.fromClass(elements, LockHeld.class);
    /** The @{@link LockPossiblyHeld} annotation. */
    protected final AnnotationMirror LOCKPOSSIBLYHELD =
            AnnotationBuilder.fromClass(elements, LockPossiblyHeld.class);
    /** The @{@link SideEffectFree} annotation. */
    protected final AnnotationMirror SIDEEFFECTFREE =
            AnnotationBuilder.fromClass(elements, SideEffectFree.class);
    /** The @{@link GuardedByUnknown} annotation. */
    protected final AnnotationMirror GUARDEDBYUNKNOWN =
            AnnotationBuilder.fromClass(elements, GuardedByUnknown.class);
    /** The @{@link GuardedByBottom} annotation. */
    protected final AnnotationMirror GUARDEDBY =
            createGuardedByAnnotationMirror(new ArrayList<String>());
    /** The @{@link GuardedByBottom} annotation. */
    protected final AnnotationMirror GUARDEDBYBOTTOM =
            AnnotationBuilder.fromClass(elements, GuardedByBottom.class);
    /** The @{@link GuardSatisfied} annotation. */
    protected final AnnotationMirror GUARDSATISFIED =
            AnnotationBuilder.fromClass(elements, GuardSatisfied.class);

    /** The value() element/field of a @GuardedBy annotation. */
    protected final ExecutableElement guardedByValueElement =
            TreeUtils.getMethod(GuardedBy.class, "value", 0, processingEnv);
    /** The value() element/field of a @GuardSatisfied annotation. */
    protected final ExecutableElement guardSatisfiedValueElement =
            TreeUtils.getMethod(GuardSatisfied.class, "value", 0, processingEnv);
    /** The EnsuresLockHeld.value element/field. */
    protected final ExecutableElement ensuresLockHeldValueElement =
            TreeUtils.getMethod(EnsuresLockHeld.class, "value", 0, processingEnv);
    /** The EnsuresLockHeldIf.expression element/field. */
    protected final ExecutableElement ensuresLockHeldIfExpressionElement =
            TreeUtils.getMethod(EnsuresLockHeldIf.class, "expression", 0, processingEnv);

    /** The net.jcip.annotations.GuardedBy annotation, or null if not on the classpath. */
    protected final Class<? extends Annotation> jcipGuardedBy;

    /** The javax.annotation.concurrent.GuardedBy annotation, or null if not on the classpath. */
    protected final Class<? extends Annotation> javaxGuardedBy;

    /** Create a new LockAnnotatedTypeFactory. */
    public LockAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        // This alias is only true for the Lock Checker. All other checkers must
        // ignore the @LockingFree annotation.
        addAliasedDeclAnnotation(LockingFree.class, SideEffectFree.class, SIDEEFFECTFREE);

        // This alias is only true for the Lock Checker. All other checkers must
        // ignore the @ReleasesNoLocks annotation.  Note that ReleasesNoLocks is
        // not truly side-effect-free even as far as the Lock Checker is concerned,
        // so there is additional handling of this annotation in the Lock Checker.
        addAliasedDeclAnnotation(ReleasesNoLocks.class, SideEffectFree.class, SIDEEFFECTFREE);

        jcipGuardedBy = classForNameOrNull("net.jcip.annotations.GuardedBy");

        javaxGuardedBy = classForNameOrNull("javax.annotation.concurrent.GuardedBy");

        postInit();
    }

    /**
     * Returns the value of Class.forName, or null if Class.forName would throw an exception.
     *
     * @param annotationClassName an annotation's name, in ClassGetName format
     * @return an annotation class or null
     */
    @SuppressWarnings("unchecked") // cast to generic type
    private Class<? extends Annotation> classForNameOrNull(
            @ClassGetName String annotationClassName) {
        try {
            return (Class<? extends Annotation>) Class.forName(annotationClassName);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new DependentTypesHelper(this) {
            @Override
            protected void reportErrors(Tree errorTree, List<DependentTypesError> errors) {
                // If the error message is NOT_EFFECTIVELY_FINAL, then report
                // lock.expression.not.final instead of expression.unparsable.type.invalid .
                List<DependentTypesError> superErrors = new ArrayList<>(errors.size());
                for (DependentTypesError error : errors) {
                    if (error.error.equals(NOT_EFFECTIVELY_FINAL)) {
                        checker.reportError(
                                errorTree, "lock.expression.not.final", error.expression);
                    } else {
                        superErrors.add(error);
                    }
                }
                super.reportErrors(errorTree, superErrors);
            }

            @Override
            protected boolean shouldPassThroughExpression(String expression) {
                // There is no expression to use to replace <self> here, so just pass the expression
                // along.
                return super.shouldPassThroughExpression(expression)
                        || LockVisitor.SELF_RECEIVER_PATTERN.matcher(expression).matches();
            }

            @Override
            protected @Nullable JavaExpression transform(JavaExpression javaExpr) {
                if (javaExpr instanceof Unknown || isExpressionEffectivelyFinal(javaExpr)) {
                    return javaExpr;
                }

                // If the expression isn't effectively final, then return the NOT_EFFECTIVELY_FINAL
                // error string.
                return createError(javaExpr.toString(), NOT_EFFECTIVELY_FINAL);
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
    boolean isExpressionEffectivelyFinal(JavaExpression expr) {
        if (expr instanceof FieldAccess) {
            FieldAccess fieldAccess = (FieldAccess) expr;
            JavaExpression receiver = fieldAccess.getReceiver();
            // Don't call fieldAccess
            return fieldAccess.isFinal() && isExpressionEffectivelyFinal(receiver);
        } else if (expr instanceof LocalVariable) {
            return ElementUtils.isEffectivelyFinal(((LocalVariable) expr).getElement());
        } else if (expr instanceof MethodCall) {
            MethodCall methodCall = (MethodCall) expr;
            for (JavaExpression arg : methodCall.getArguments()) {
                if (!isExpressionEffectivelyFinal(arg)) {
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
        return new LinkedHashSet<>(
                Arrays.asList(
                        LockHeld.class,
                        LockPossiblyHeld.class,
                        GuardedBy.class,
                        GuardedByUnknown.class,
                        GuardSatisfied.class,
                        GuardedByBottom.class));
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new LockQualifierHierarchy(getSupportedTypeQualifiers(), elements);
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

    /** LockQualifierHierarchy. */
    class LockQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

        /** Qualifier kind for the @{@link GuardedBy} annotation. */
        private final QualifierKind GUARDEDBY_KIND;
        /** Qualifier kind for the @{@link GuardSatisfied} annotation. */
        private final QualifierKind GUARDSATISFIED_KIND;
        /** Qualifier kind for the @{@link GuardedByBottom} annotation. */
        private final QualifierKind GUARDEDBYBOTTOM_KIND;
        /** Qualifier kind for the @{@link GuardedByUnknown} annotation. */
        private final QualifierKind GUARDEDBYUNKNOWN_KIND;

        /**
         * Creates a LockQualifierHierarchy.
         *
         * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
         * @param elements element utils
         */
        public LockQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            GUARDEDBY_KIND = getQualifierKind(GUARDEDBY);
            GUARDSATISFIED_KIND = getQualifierKind(GUARDSATISFIED);
            GUARDEDBYBOTTOM_KIND = getQualifierKind(GUARDEDBYBOTTOM);
            GUARDEDBYUNKNOWN_KIND = getQualifierKind(GUARDEDBYUNKNOWN);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (subKind == GUARDEDBY_KIND && superKind == GUARDEDBY_KIND) {
                List<String> subLocks =
                        AnnotationUtils.getElementValueArray(
                                superAnno,
                                guardedByValueElement,
                                String.class,
                                Collections.emptyList());
                List<String> superLocks =
                        AnnotationUtils.getElementValueArray(
                                subAnno,
                                guardedByValueElement,
                                String.class,
                                Collections.emptyList());
                return subLocks.containsAll(superLocks) && superLocks.containsAll(subLocks);
            } else if (subKind == GUARDSATISFIED_KIND && superKind == GUARDSATISFIED_KIND) {
                return AnnotationUtils.areSame(superAnno, subAnno);
            }
            throw new RuntimeException("Unexpected");
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1 == GUARDEDBY_KIND && qualifierKind2 == GUARDEDBY_KIND) {
                List<String> locks1 =
                        AnnotationUtils.getElementValueArray(
                                a1, guardedByValueElement, String.class, Collections.emptyList());
                List<String> locks2 =
                        AnnotationUtils.getElementValueArray(
                                a2, guardedByValueElement, String.class, Collections.emptyList());
                if (locks1.containsAll(locks2) && locks2.containsAll(locks1)) {
                    return a1;
                } else {
                    return GUARDEDBYUNKNOWN;
                }
            } else if (qualifierKind1 == GUARDSATISFIED_KIND
                    && qualifierKind2 == GUARDSATISFIED_KIND) {
                if (AnnotationUtils.areSame(a1, a2)) {
                    return a1;
                } else {
                    return GUARDEDBYUNKNOWN;
                }
            } else if (qualifierKind1 == GUARDEDBYBOTTOM_KIND) {
                return a2;
            } else if (qualifierKind2 == GUARDEDBYBOTTOM_KIND) {
                return a1;
            }
            throw new RuntimeException("Unexpected");
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1 == GUARDEDBY_KIND && qualifierKind2 == GUARDEDBY_KIND) {
                List<String> locks1 =
                        AnnotationUtils.getElementValueArray(
                                a1, guardedByValueElement, String.class, Collections.emptyList());
                List<String> locks2 =
                        AnnotationUtils.getElementValueArray(
                                a2, guardedByValueElement, String.class, Collections.emptyList());
                if (locks1.containsAll(locks2) && locks2.containsAll(locks1)) {
                    return a1;
                } else {
                    return GUARDEDBYBOTTOM;
                }
            } else if (qualifierKind1 == GUARDSATISFIED_KIND
                    && qualifierKind2 == GUARDSATISFIED_KIND) {
                if (AnnotationUtils.areSame(a1, a2)) {
                    return a1;
                } else {
                    return GUARDEDBYBOTTOM;
                }
            } else if (qualifierKind1 == GUARDEDBYUNKNOWN_KIND) {
                return a2;
            } else if (qualifierKind2 == GUARDEDBYUNKNOWN_KIND) {
                return a1;
            }
            throw new RuntimeException("Unexpected");
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
     * conservative default.
     *
     * @param element the method element
     * @param issueErrorIfMoreThanOnePresent whether to issue an error if more than one side effect
     *     annotation is present on the method
     */
    // package-private
    SideEffectAnnotation methodSideEffectAnnotation(
            Element element, boolean issueErrorIfMoreThanOnePresent) {
        if (element != null) {
            Set<SideEffectAnnotation> sideEffectAnnotationPresent =
                    EnumSet.noneOf(SideEffectAnnotation.class);
            for (SideEffectAnnotation sea : SideEffectAnnotation.values()) {
                if (getDeclAnnotationNoAliases(element, sea.getAnnotationClass()) != null) {
                    sideEffectAnnotationPresent.add(sea);
                }
            }

            int count = sideEffectAnnotationPresent.size();

            if (count == 0) {
                return defaults.applyConservativeDefaults(element)
                        ? SideEffectAnnotation.MAYRELEASELOCKS
                        : SideEffectAnnotation.RELEASESNOLOCKS;
            }

            if (count > 1 && issueErrorIfMoreThanOnePresent) {
                // TODO: Turn on after figuring out how this interacts with inherited annotations.
                // checker.reportError(element, "multiple.sideeffect.annotations");
            }

            SideEffectAnnotation weakest = null;
            // At least one side effect annotation was found. Return the weakest.
            for (SideEffectAnnotation sea : sideEffectAnnotationPresent) {
                if (weakest == null || sea.isWeakerThan(weakest)) {
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
     * Returns the index (that is, the {@code value} element) on the {@code @GuardSatisfied}
     * annotation in the given AnnotatedTypeMirror. Assumes atm is non-null and contains a
     * {@code @GuardSatisfied} annotation.
     *
     * @param atm an AnnotatedTypeMirror containing a GuardSatisfied annotation
     * @return the index on the GuardSatisfied annotation
     */
    // package-private
    int getGuardSatisfiedIndex(AnnotatedTypeMirror atm) {
        return getGuardSatisfiedIndex(atm.getAnnotation(GuardSatisfied.class));
    }

    /**
     * Returns the index (that is, the {@code value} element) on the given {@code @GuardSatisfied}
     * annotation. Assumes am is non-null and is a GuardSatisfied annotation.
     *
     * @param am an AnnotationMirror for a GuardSatisfied annotation
     * @return the index on the GuardSatisfied annotation
     */
    // package-private
    int getGuardSatisfiedIndex(AnnotationMirror am) {
        return AnnotationUtils.getElementValueInt(am, guardSatisfiedValueElement, -1);
    }

    @Override
    public ParameterizedExecutableType methodFromUse(
            ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {
        ParameterizedExecutableType mType = super.methodFromUse(tree, methodElt, receiverType);

        if (tree.getKind() != Kind.METHOD_INVOCATION) {
            return mType;
        }

        // If a method's formal return type is annotated with @GuardSatisfied(index), look for the
        // first instance of @GuardSatisfied(index) in the method definition's receiver type or
        // formal parameters, retrieve the corresponding type of the actual parameter / receiver at
        // the call site (e.g. @GuardedBy("someLock") and replace the return type at the call site
        // with this type.

        AnnotatedExecutableType invokedMethod = mType.executableType;

        if (invokedMethod.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            return mType;
        }

        AnnotatedTypeMirror methodDefinitionReturn = invokedMethod.getReturnType();

        if (methodDefinitionReturn == null
                || !methodDefinitionReturn.hasAnnotation(GuardSatisfied.class)) {
            return mType;
        }

        int returnGuardSatisfiedIndex = getGuardSatisfiedIndex(methodDefinitionReturn);

        // @GuardSatisfied with no index defaults to index -1. Ignore instances of @GuardSatisfied
        // with no index.  If a method is defined with a return type of @GuardSatisfied with no
        // index, an error is reported by LockVisitor.visitMethod.

        if (returnGuardSatisfiedIndex == -1) {
            return mType;
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
            return mType;
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
                return mType;
            }
        }

        return mType;
    }

    /**
     * If {@code atm} is not null and contains a {@code @GuardSatisfied} annotation, and if the
     * index of this {@code @GuardSatisfied} annotation matches {@code matchingGuardSatisfiedIndex},
     * then {@code methodReturnAtm} will have its annotation in the {@code @GuardedBy} hierarchy
     * replaced with that in {@code annotationInGuardedByHierarchy}.
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
            translateJcipAndJavaxAnnotations(TreeUtils.elementFromTree((VariableTree) tree), type);
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

        AnnotationMirror anno = null;

        if (jcipGuardedBy != null) {
            anno = getDeclAnnotation(element, jcipGuardedBy);
        }

        if (anno == null && javaxGuardedBy != null) {
            anno = getDeclAnnotation(element, javaxGuardedBy);
        }

        if (anno == null) {
            return;
        }

        // The version of javax.annotation.concurrent.GuardedBy included with the Checker Framework
        // declares the type of value as an array of Strings, whereas the one defined in JCIP and
        // included with FindBugs declares it as a String. So, the code below figures out which type
        // should be used.
        Map<? extends ExecutableElement, ? extends AnnotationValue> valmap =
                anno.getElementValues();
        Object value = null;
        for (ExecutableElement elem : valmap.keySet()) {
            if (elem.getSimpleName().contentEquals("value")) {
                value = valmap.get(elem).getValue();
                break;
            }
        }
        List<String> lockExpressions;
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<AnnotationValue> la = (List<AnnotationValue>) value;
            lockExpressions =
                    CollectionsPlume.mapList((AnnotationValue a) -> (String) a.getValue(), la);
        } else if (value instanceof String) {
            lockExpressions = Collections.singletonList((String) value);
        } else {
            return;
        }

        if (lockExpressions.isEmpty()) {
            atm.addAnnotation(GUARDEDBY);
        } else {
            atm.addAnnotation(createGuardedByAnnotationMirror(lockExpressions));
        }
    }

    /**
     * Returns an AnnotationMirror corresponding to @GuardedBy(values).
     *
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
