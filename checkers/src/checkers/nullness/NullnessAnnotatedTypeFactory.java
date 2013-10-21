package checkers.nullness;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.CFAbstractAnalysis;
import checkers.initialization.InitializationAnnotatedTypeFactory;
import checkers.nullness.quals.MonotonicNonNull;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.PolyAll;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.GeneralAnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;
import checkers.util.DependentTypes;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import javacutils.AnnotationUtils;
import javacutils.ElementUtils;
import javacutils.InternalUtils;
import javacutils.Pair;
import javacutils.TreeUtils;
import javacutils.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
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

/**
 * The annotated type factory for the nullness type-system.
 */
public class NullnessAnnotatedTypeFactory
    extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis> {

    /** Annotation constants */
    protected final AnnotationMirror NONNULL, NULLABLE, POLYNULL, MONOTONIC_NONNULL;

    /** Dependent types instance. */
    protected final DependentTypes dependentTypes;

    protected final MapGetHeuristics mapGetHeuristics;
    protected final SystemGetPropertyHandler systemGetPropertyHandler;
    protected final CollectionToArrayHeuristics collectionToArrayHeuristics;

    /**
     * Factory for arbitrary qualifiers, used for declarations and "unused"
     * qualifier.
     */
    protected final GeneralAnnotatedTypeFactory generalFactory;

    // Cache for the nullness annotations
    protected final Set<Class<? extends Annotation>> nullnessAnnos;


    @SuppressWarnings("deprecation") // aliasing to deprecated annotation
    public NullnessAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFbc) {
        super(checker, useFbc);

        NONNULL = AnnotationUtils.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(elements, Nullable.class);
        POLYNULL = AnnotationUtils.fromClass(elements, PolyNull.class);
        MONOTONIC_NONNULL = AnnotationUtils.fromClass(elements, MonotonicNonNull.class);

        Set<Class<? extends Annotation>> tempNullnessAnnos = new HashSet<>();
        tempNullnessAnnos.add(NonNull.class);
        tempNullnessAnnos.add(MonotonicNonNull.class);
        tempNullnessAnnos.add(Nullable.class);
        tempNullnessAnnos.add(PolyNull.class);
        tempNullnessAnnos.add(PolyAll.class);
        nullnessAnnos = Collections.unmodifiableSet(tempNullnessAnnos);

        addAliasedAnnotation(checkers.nullness.quals.LazyNonNull.class, MONOTONIC_NONNULL);

        // If you update the following, also update ../../../manual/nullness-checker.tex .
        // Aliases for @Nonnull:
        addAliasedAnnotation(com.sun.istack.NotNull.class, NONNULL);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class, NONNULL);
        addAliasedAnnotation(javax.annotation.Nonnull.class, NONNULL);
        addAliasedAnnotation(javax.validation.constraints.NotNull.class, NONNULL);
        addAliasedAnnotation(org.eclipse.jdt.annotation.NonNull.class, NONNULL);
        addAliasedAnnotation(org.jetbrains.annotations.NotNull.class, NONNULL);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NonNull.class, NONNULL);
        addAliasedAnnotation(org.jmlspecs.annotation.NonNull.class, NONNULL);

        // Aliases for @Nullable:
        addAliasedAnnotation(com.sun.istack.Nullable.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.UnknownNullness.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.eclipse.jdt.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.jetbrains.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NullAllowed.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NullUnknown.class, NULLABLE);
        addAliasedAnnotation(org.jmlspecs.annotation.Nullable.class, NULLABLE);

        // TODO: These heuristics are just here temporarily. They all either
        // need to be replaced, or carefully checked for correctness.
        generalFactory = new GeneralAnnotatedTypeFactory(checker);
        // Alias the same generalFactory below and ensure that setRoot updates it.
        mapGetHeuristics = new MapGetHeuristics(checker, this, generalFactory);
        dependentTypes = new DependentTypes(checker, generalFactory);

        systemGetPropertyHandler = new SystemGetPropertyHandler(processingEnv, this);

        postInit();

        // do this last, as it might use the factory again.
        this.collectionToArrayHeuristics = new CollectionToArrayHeuristics(
                processingEnv, this);
    }

    @Override
    public void setRoot(CompilationUnitTree root) {
        generalFactory.setRoot(root);
        super.setRoot(root);
    }

    // handle dependent types
    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
        super.annotateImplicit(tree, type, useFlow);
        dependentTypes.handle(tree, type);
    }


    @Override
    public AnnotatedTypeMirror getDefaultedAnnotatedType(Tree varTree,
            ExpressionTree valueTree) {
        AnnotatedTypeMirror result = super.getDefaultedAnnotatedType(varTree, valueTree);
        return handlePolyNull(result, valueTree);
    }

    /**
     * Replaces {@link PolyNull} with {@link Nullable} to be more permissive
     * (because {@code type} is usually a left-hand side) if the dataflow
     * analysis has determined that this is allowed soundly.
     */
    protected AnnotatedTypeMirror handlePolyNull(AnnotatedTypeMirror type,
            Tree context) {
        if (type.hasAnnotation(PolyNull.class)
                || type.hasAnnotation(PolyAll.class)) {
            NullnessValue inferred = getInferredValueFor(context);
            if (inferred != null && inferred.isPolyNullNull) {
                type.replaceAnnotation(NULLABLE);
            }
        }
        return type;
    }

    // handle dependent types
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(
            NewClassTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> fromUse = super.constructorFromUse(tree);
        AnnotatedExecutableType constructor = fromUse.first;
        dependentTypes.handleConstructor(tree,
                generalFactory.getAnnotatedType(tree), constructor);
        return fromUse;
    }

    @Override
    public List<VariableTree> getUninitializedInvariantFields(
            NullnessStore store, TreePath path, boolean isStatic,
            List<? extends AnnotationMirror> receiverAnnotations) {
        List<VariableTree> candidates = super.getUninitializedInvariantFields(
                store, path, isStatic, receiverAnnotations);
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
    protected NullnessAnalysis createFlowAnalysis(List<Pair<VariableElement, NullnessValue>> fieldValues) {
        return new NullnessAnalysis(checker, this, fieldValues);
    }

    @Override
    public NullnessTransfer createFlowTransferFunction(CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer> analysis) {
        return new NullnessTransfer((NullnessAnalysis) analysis);
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super
                .methodFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;

        TreePath path = this.getPath(tree);
        if (path != null) {
            /*
             * The above check for null ensures that Issue 109 does not arise.
             * TODO: I'm a bit concerned about one aspect: it looks like the
             * field initializer is used to determine the type of a read field.
             * Why is this not just using the declared type of the field? Could
             * this lead to confusion for programmers? I think skipping the
             * mapGetHeuristics is always a safe option.
             */
            mapGetHeuristics.handle(path, method);
        }
        systemGetPropertyHandler.handle(tree, method);
        collectionToArrayHeuristics.handle(tree, method);
        return mfuPair;
    }

    @Override
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
        return handlePolyNull(super.getMethodReturnType(m, r), r);
    }

    protected AnnotatedTypeMirror getDeclaredAndDefaultedAnnotatedType(Tree tree) {
        HACK_DONT_CALL_POST_AS_MEMBER = true;
        shouldCache = false;

        AnnotatedTypeMirror type = getAnnotatedType(tree);

        shouldCache = true;
        HACK_DONT_CALL_POST_AS_MEMBER = false;

        return type;
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new NullnessTypeAnnotator(this);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new NullnessTreeAnnotator(this);
    }

    /**
     * If the element is {@link NonNull} when used in a static member access,
     * modifies the element's type (by adding {@link NonNull}).
     *
     * @param elt
     *            the element being accessed
     * @param type
     *            the type of the element {@code elt}
     */
    private void annotateIfStatic(Element elt, AnnotatedTypeMirror type) {
        if (elt == null)
            return;

        if (elt.getKind().isClass() || elt.getKind().isInterface()
        // Workaround for System.{out,in,err} issue: assume all static
        // fields in java.lang.System are nonnull.
                || isSystemField(elt)) {
            type.replaceAnnotation(NONNULL);
        }
    }

    private static boolean isSystemField(Element elt) {
        if (!elt.getKind().isField())
            return false;

        if (!ElementUtils.isStatic(elt) || !ElementUtils.isFinal(elt))
            return false;

        VariableElement var = (VariableElement) elt;

        // Heuristic: if we have a static final field in a system package,
        // treat it as NonNull (many like Boolean.TYPE and System.out
        // have constant value null but are set by the VM).
        boolean inJavaPackage = ElementUtils.getQualifiedClassName(var)
                .toString().startsWith("java.");

        return (var.getConstantValue() != null
                || var.getSimpleName().contentEquals("class") || inJavaPackage);
    }

    protected class NullnessTreeAnnotator
        extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTreeAnnotator {

        public NullnessTreeAnnotator(NullnessAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node,
                AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;
            // case 8: class in static member access
            annotateIfStatic(elt, type);
            return super.visitMemberSelect(node, type);
        }

        @Override
        public Void visitVariable(VariableTree node,
                AnnotatedTypeMirror type) {
            Element elt = InternalUtils.symbol(node);
            if (elt.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                if (!type.isAnnotatedInHierarchy(NONNULL)) {
                    // case 9. exception parameter
                    type.addAnnotation(NONNULL);
                }
            }
            return super.visitVariable(node, type);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node,
                AnnotatedTypeMirror type) {

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

            return super.visitIdentifier(node, type);
        }

        // The result of a binary operation is always non-null.
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null; // super.visitBinary(node, type);
        }

        // The result of a compound operation is always non-null.
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node,
                AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            // call super for initialization defaults
            return super.visitCompoundAssignment(node, type);
        }

        // The result of a unary operation is always non-null.
        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null; // super.visitUnary(node, type);
        }

        // The result of newly allocated structures is always non-null.
        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return super.visitNewClass(node, type);
        }
    }

    protected class NullnessTypeAnnotator
        extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTypeAnnotator {

        public NullnessTypeAnnotator(InitializationAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }
    }


    /**
     * @return The list of annotations of the non-null type system.
     */
    public Set<Class<? extends Annotation>> getNullnessAnnotations() {
        return nullnessAnnos;
    }

    @Override
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        Set<Class<? extends Annotation>> l = new HashSet<>(
                super.getInvalidConstructorReturnTypeAnnotations());
        l.addAll(getNullnessAnnotations());
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        Elements elements = processingEnv.getElementUtils();
        return AnnotationUtils.fromClass(elements, NonNull.class);
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
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (isInitializationAnnotation(rhs) ||
                    isInitializationAnnotation(lhs)) {
                return this.isSubtypeInitialization(rhs, lhs);
            }
            return super.isSubtype(rhs, lhs);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isInitializationAnnotation(a1) ||
                    isInitializationAnnotation(a2)) {
                return this.leastUpperBoundInitialization(a1, a2);
            }
            return super.leastUpperBound(a1, a2);
        }
    }

}
