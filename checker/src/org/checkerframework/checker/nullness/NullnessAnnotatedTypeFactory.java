package org.checkerframework.checker.nullness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonRaw;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.nullness.qual.Raw;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GeneralAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.DependentTypes;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the nullness type-system. */
public class NullnessAnnotatedTypeFactory
        extends InitializationAnnotatedTypeFactory<
                NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis> {

    /** Annotation constants */
    protected final AnnotationMirror NONNULL, NULLABLE, POLYNULL, MONOTONIC_NONNULL;

    /** Dependent types instance. */
    protected final DependentTypes dependentTypes;

    protected final SystemGetPropertyHandler systemGetPropertyHandler;
    protected final CollectionToArrayHeuristics collectionToArrayHeuristics;

    /** Factory for arbitrary qualifiers, used for declarations and "unused" qualifier. */
    protected final GeneralAnnotatedTypeFactory generalFactory;

    /** Cache for the nullness annotations */
    protected final Set<Class<? extends Annotation>> nullnessAnnos;

    @SuppressWarnings("deprecation") // aliasing to deprecated annotation
    public NullnessAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFbc) {
        super(checker, useFbc);

        NONNULL = AnnotationUtils.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(elements, Nullable.class);
        POLYNULL = AnnotationUtils.fromClass(elements, PolyNull.class);
        MONOTONIC_NONNULL = AnnotationUtils.fromClass(elements, MonotonicNonNull.class);

        Set<Class<? extends Annotation>> tempNullnessAnnos = new LinkedHashSet<>();
        tempNullnessAnnos.add(NonNull.class);
        tempNullnessAnnos.add(MonotonicNonNull.class);
        tempNullnessAnnos.add(Nullable.class);
        tempNullnessAnnos.add(PolyNull.class);
        tempNullnessAnnos.add(PolyAll.class);
        nullnessAnnos = Collections.unmodifiableSet(tempNullnessAnnos);

        addAliasedAnnotation(
                org.checkerframework.checker.nullness.qual.LazyNonNull.class, MONOTONIC_NONNULL);

        // If you update the following, also update ../../../../../docs/manual/nullness-checker.tex .
        // Aliases for @Nonnull:
        addAliasedAnnotation(com.sun.istack.internal.NotNull.class, NONNULL);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class, NONNULL);
        addAliasedAnnotation(javax.annotation.Nonnull.class, NONNULL);
        addAliasedAnnotation(javax.validation.constraints.NotNull.class, NONNULL);
        addAliasedAnnotation(lombok.NonNull.class, NONNULL);
        addAliasedAnnotation(org.eclipse.jdt.annotation.NonNull.class, NONNULL);
        addAliasedAnnotation(org.eclipse.jgit.annotations.NonNull.class, NONNULL);
        addAliasedAnnotation(org.jetbrains.annotations.NotNull.class, NONNULL);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NonNull.class, NONNULL);
        addAliasedAnnotation(org.jmlspecs.annotation.NonNull.class, NONNULL);
        addAliasedAnnotation(android.annotation.NonNull.class, NONNULL);
        addAliasedAnnotation(android.support.annotation.NonNull.class, NONNULL);
        // Aliases for @Nullable:
        addAliasedAnnotation(com.sun.istack.internal.Nullable.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.UnknownNullness.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.eclipse.jdt.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.eclipse.jgit.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.jetbrains.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NullAllowed.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NullUnknown.class, NULLABLE);
        addAliasedAnnotation(org.jmlspecs.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(android.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(android.support.annotation.Nullable.class, NULLABLE);

        // Add compatibility annotations:
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.NullableDecl.class, NULLABLE);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.PolyNullDecl.class, POLYNULL);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.NonNullDecl.class, NONNULL);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl.class,
                MONOTONIC_NONNULL);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.NullableType.class, NULLABLE);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.PolyNullType.class, POLYNULL);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.NonNullType.class, NONNULL);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.MonotonicNonNullType.class,
                MONOTONIC_NONNULL);

        // TODO: These heuristics are just here temporarily. They all either
        // need to be replaced, or carefully checked for correctness.
        generalFactory = new GeneralAnnotatedTypeFactory(checker);
        // Alias the same generalFactory below and ensure that setRoot updates it.
        dependentTypes = new DependentTypes(checker, generalFactory);

        systemGetPropertyHandler = new SystemGetPropertyHandler(processingEnv, this);

        postInit();

        // do this last, as it might use the factory again.
        this.collectionToArrayHeuristics = new CollectionToArrayHeuristics(processingEnv, this);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // NullnessATF is used by both NullnessChecker and NullnessRawnessChecker, load the correct set of qualifiers here
        AbstractNullnessChecker ckr = (AbstractNullnessChecker) checker;
        // if useFbc is true, then it is the NullnessChecker
        if (ckr.useFbc) {
            return new LinkedHashSet<Class<? extends Annotation>>(
                    Arrays.asList(
                            Nullable.class,
                            MonotonicNonNull.class,
                            NonNull.class,
                            UnderInitialization.class,
                            Initialized.class,
                            UnknownInitialization.class,
                            FBCBottom.class,
                            PolyNull.class,
                            PolyAll.class));
        }
        // otherwise, it is the NullnessRawnessChecker
        else {
            return new LinkedHashSet<Class<? extends Annotation>>(
                    Arrays.asList(
                            Nullable.class,
                            MonotonicNonNull.class,
                            NonNull.class,
                            NonRaw.class,
                            Raw.class,
                            // PolyRaw.class, //TODO: support PolyRaw in the future
                            PolyNull.class,
                            PolyAll.class));
        }
    }

    @Override
    public void setRoot(CompilationUnitTree root) {
        generalFactory.setRoot(root);
        super.setRoot(root);
    }

    // handle dependent types
    @Override
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
        super.addComputedTypeAnnotations(tree, type, useFlow);
        dependentTypes.handle(tree, type);
    }

    /**
     * For types of left-hand side of an assignment, this method replaces {@link PolyNull} or {@link
     * PolyAll} with {@link Nullable} if the org.checkerframework.dataflow analysis has determined
     * that this is allowed soundly. For example:
     *
     * <pre> @PolyNull String foo(@PolyNull String param) {
     *    if (param == null) {
     *        //  @PolyNull is really @Nullable, so change
     *        // the type of param to @Nullable.
     *        param = null;
     *    }
     *    return param;
     * }
     * </pre>
     *
     * @param lhsType type to replace whose polymorphic qualifier will be replaced
     * @param context tree used to get dataflow value
     */
    protected void replacePolyQualifier(AnnotatedTypeMirror lhsType, Tree context) {
        if (lhsType.hasAnnotation(PolyNull.class) || lhsType.hasAnnotation(PolyAll.class)) {
            NullnessValue inferred = getInferredValueFor(context);
            if (inferred != null && inferred.isPolyNullNull) {
                lhsType.replaceAnnotation(NULLABLE);
            }
        }
    }

    // handle dependent types
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(
            NewClassTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> fromUse =
                super.constructorFromUse(tree);
        AnnotatedExecutableType constructor = fromUse.first;
        dependentTypes.handleConstructor(tree, generalFactory.getAnnotatedType(tree), constructor);
        return fromUse;
    }

    @Override
    public List<VariableTree> getUninitializedInvariantFields(
            NullnessStore store,
            TreePath path,
            boolean isStatic,
            List<? extends AnnotationMirror> receiverAnnotations) {
        List<VariableTree> candidates =
                super.getUninitializedInvariantFields(store, path, isStatic, receiverAnnotations);
        List<VariableTree> result = new ArrayList<>();
        for (VariableTree c : candidates) {
            AnnotatedTypeMirror type = getAnnotatedType(c);
            boolean isPrimitive = TypesUtils.isPrimitive(type.getUnderlyingType());
            if (!isPrimitive) {
                // primitives do not need to be initialized
                result.add(c);
            }
        }
        return result;
    }

    @Override
    protected NullnessAnalysis createFlowAnalysis(
            List<Pair<VariableElement, NullnessValue>> fieldValues) {
        return new NullnessAnalysis(checker, this, fieldValues);
    }

    @Override
    public NullnessTransfer createFlowTransferFunction(
            CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer> analysis) {
        return new NullnessTransfer((NullnessAnalysis) analysis);
    }

    /** @return an AnnotatedTypeFormatter that does not print the qualifiers on null literals */
    @Override
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
        return new NullnessAnnotatedTypeFormatter(
                checker.hasOption("printVerboseGenerics"), checker.hasOption("printAllQualifiers"));
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =
                super.methodFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;

        systemGetPropertyHandler.handle(tree, method);
        collectionToArrayHeuristics.handle(tree, method);
        return mfuPair;
    }

    @Override
    public void adaptGetClassReturnTypeToReceiver(
            final AnnotatedExecutableType getClassType, final AnnotatedTypeMirror receiverType) {

        super.adaptGetClassReturnTypeToReceiver(getClassType, receiverType);

        // Make the wildcard always @NonNull, regardless of the declared type.

        final AnnotatedDeclaredType returnAdt =
                (AnnotatedDeclaredType) getClassType.getReturnType();
        final List<AnnotatedTypeMirror> typeArgs = returnAdt.getTypeArguments();
        final AnnotatedWildcardType classWildcardArg = (AnnotatedWildcardType) typeArgs.get(0);
        classWildcardArg.getExtendsBoundField().replaceAnnotation(NONNULL);
    }

    @Override
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
        AnnotatedTypeMirror result = super.getMethodReturnType(m, r);
        replacePolyQualifier(result, r);
        return result;
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        ImplicitsTypeAnnotator implicitsTypeAnnotator = new ImplicitsTypeAnnotator(this);
        implicitsTypeAnnotator.addTypeClass(AnnotatedTypeMirror.AnnotatedNoType.class, NONNULL);
        implicitsTypeAnnotator.addTypeClass(
                AnnotatedTypeMirror.AnnotatedPrimitiveType.class, NONNULL);
        return new ListTypeAnnotator(
                new PropagationTypeAnnotator(this),
                implicitsTypeAnnotator,
                new NullnessTypeAnnotator(this),
                new CommitmentTypeAnnotator(this));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        // Don't call super.createTreeAnnotator because the default tree annotators are incorrect
        // for the Nullness Checker.
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(this);
        implicitsTreeAnnotator.addTreeKind(Tree.Kind.NEW_CLASS, NONNULL);
        implicitsTreeAnnotator.addTreeKind(Tree.Kind.NEW_ARRAY, NONNULL);

        return new ListTreeAnnotator( // DebugListTreeAnnotator(new Tree.Kind[] {Tree.Kind.CONDITIONAL_EXPRESSION},
                new NullnessPropagationAnnotator(this),
                implicitsTreeAnnotator,
                new NullnessTreeAnnotator(this),
                new CommitmentTreeAnnotator(this));
    }

    /**
     * If the element is {@link NonNull} when used in a static member access, modifies the element's
     * type (by adding {@link NonNull}).
     *
     * @param elt the element being accessed
     * @param type the type of the element {@code elt}
     */
    private void annotateIfStatic(Element elt, AnnotatedTypeMirror type) {
        if (elt == null) {
            return;
        }

        if (elt.getKind().isClass()
                || elt.getKind().isInterface()
                // Workaround for System.{out,in,err} issue: assume all static
                // fields in java.lang.System are nonnull.
                || isSystemField(elt)) {
            type.replaceAnnotation(NONNULL);
        }
    }

    private static boolean isSystemField(Element elt) {
        if (!elt.getKind().isField()) {
            return false;
        }

        if (!ElementUtils.isStatic(elt) || !ElementUtils.isFinal(elt)) {
            return false;
        }

        VariableElement var = (VariableElement) elt;

        // Heuristic: if we have a static final field in a system package,
        // treat it as NonNull (many like Boolean.TYPE and System.out
        // have constant value null but are set by the VM).
        boolean inJavaPackage =
                ElementUtils.getQualifiedClassName(var).toString().startsWith("java.");

        return (var.getConstantValue() != null
                || var.getSimpleName().contentEquals("class")
                || inJavaPackage);
    }

    /**
     * Nullness doesn't call propagation on binary and unary because the result is
     * always @Initialized (the default qualifier).
     *
     * <p>Would this be valid to move into CommitmentTreeAnnotator.
     */
    protected static class NullnessPropagationAnnotator extends PropagationTreeAnnotator {

        public NullnessPropagationAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }

        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            return null;
        }
    }

    protected class NullnessTreeAnnotator extends TreeAnnotator
    /*extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTreeAnnotator*/ {

        public NullnessTreeAnnotator(NullnessAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;
            // case 8: class in static member access
            annotateIfStatic(elt, type);
            return null;
        }

        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror type) {
            Element elt = InternalUtils.symbol(node);
            if (elt.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                if (!type.isAnnotatedInHierarchy(NONNULL)) {
                    // case 9. exception parameter
                    type.addAnnotation(NONNULL);
                }
            }
            return null;
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;

            // case 8. static method access
            annotateIfStatic(elt, type);

            if (elt.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                // TODO: It's surprising that we have to do this in
                // both visitVariable and visitIdentifier. This should
                // already be handled by applying the defaults anyway.
                // case 9. exception parameter
                type.replaceAnnotation(NONNULL);
            }

            return null;
        }

        // The result of a binary operation is always non-null.
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }

        // The result of a compound operation is always non-null.
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            // Commitment will run after for initialization defaults
            return null;
        }

        // The result of a unary operation is always non-null.
        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }

        // The result of newly allocated structures is always non-null.
        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }
    }

    protected class NullnessTypeAnnotator
            extends InitializationAnnotatedTypeFactory<
                            NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>
                    .CommitmentTypeAnnotator {

        public NullnessTypeAnnotator(InitializationAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }
    }

    /** @return the list of annotations of the non-null type system */
    public Set<Class<? extends Annotation>> getNullnessAnnotations() {
        return nullnessAnnos;
    }

    @Override
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        Set<Class<? extends Annotation>> l =
                new HashSet<>(super.getInvalidConstructorReturnTypeAnnotations());
        l.addAll(getNullnessAnnotations());
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return NONNULL;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In other words, is the lower bound @NonNull?
     *
     * @param type of field that might have invariant annotation
     * @return whether or not type has the invariant annotation
     */
    @Override
    protected boolean hasFieldInvariantAnnotation(AnnotatedTypeMirror type) {
        AnnotationMirror invariant = getFieldInvariantAnnotation();
        Set<AnnotationMirror> lowerBounds =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, type);
        return AnnotationUtils.containsSame(lowerBounds, invariant);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new NullnessQualifierHierarchy(factory, (Object[]) null);
    }

    protected class NullnessQualifierHierarchy extends InitializationQualifierHierarchy {

        public NullnessQualifierHierarchy(MultiGraphFactory f, Object[] arg) {
            super(f, arg);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (isInitializationAnnotation(subAnno) || isInitializationAnnotation(superAnno)) {
                return this.isSubtypeInitialization(subAnno, superAnno);
            }
            return super.isSubtype(subAnno, superAnno);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isInitializationAnnotation(a1) || isInitializationAnnotation(a2)) {
                return this.leastUpperBoundInitialization(a1, a2);
            }
            return super.leastUpperBound(a1, a2);
        }
    }
}
