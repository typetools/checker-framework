package checkers.nullness;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import javacutils.AnnotationUtils;
import javacutils.ElementUtils;
import javacutils.InternalUtils;
import javacutils.Pair;
import javacutils.TreeUtils;
import javacutils.TypesUtils;

import checkers.basetype.BaseTypeChecker;
import checkers.initialization.InitializationAnnotatedTypeFactory;
import checkers.nullness.quals.MonotonicNonNull;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.Unused;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.GeneralAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;
import checkers.util.DependentTypes;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Attribute.TypeCompound;

public class NullnessAnnotatedTypeFactory
        extends
        InitializationAnnotatedTypeFactory<AbstractNullnessChecker, NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis> {

    /** Annotation constants */
    protected final AnnotationMirror NONNULL, NULLABLE, POLYNULL;

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

    @SuppressWarnings("deprecation") // aliasing to deprecated annotation
    public NullnessAnnotatedTypeFactory(AbstractNullnessChecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        NONNULL = AnnotationUtils.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(elements, Nullable.class);
        POLYNULL = AnnotationUtils.fromClass(elements, PolyNull.class);

        addAliasedAnnotation(checkers.nullness.quals.LazyNonNull.class,
                AnnotationUtils.fromClass(elements, MonotonicNonNull.class));

        // aliases borrowed from NullnessAnnotatedTypeFactory
        addAliasedAnnotation(com.sun.istack.NotNull.class, NONNULL);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class,
                NONNULL);
        addAliasedAnnotation(javax.annotation.Nonnull.class, NONNULL);
        addAliasedAnnotation(javax.validation.constraints.NotNull.class,
                NONNULL);
        addAliasedAnnotation(org.jetbrains.annotations.NotNull.class, NONNULL);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NonNull.class,
                NONNULL);
        addAliasedAnnotation(org.jmlspecs.annotation.NonNull.class, NONNULL);
        addAliasedAnnotation(com.sun.istack.Nullable.class, NULLABLE);
        addAliasedAnnotation(
                edu.umd.cs.findbugs.annotations.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.Nullable.class,
                NULLABLE);
        addAliasedAnnotation(
                edu.umd.cs.findbugs.annotations.UnknownNullness.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.jetbrains.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(
                org.netbeans.api.annotations.common.CheckForNull.class,
                NULLABLE);
        addAliasedAnnotation(
                org.netbeans.api.annotations.common.NullAllowed.class, NULLABLE);
        addAliasedAnnotation(
                org.netbeans.api.annotations.common.NullUnknown.class, NULLABLE);
        addAliasedAnnotation(org.jmlspecs.annotation.Nullable.class, NULLABLE);

        // TODO: These heuristics are just here temporarily. They all either
        // need to be replaced, or carefully checked for correctness.
        generalFactory = new GeneralAnnotatedTypeFactory(checker, root);
        mapGetHeuristics = new MapGetHeuristics(processingEnv, this,
                generalFactory);
        systemGetPropertyHandler = new SystemGetPropertyHandler(processingEnv,
                this);
        // do this last, as it might use the factory again.
        this.collectionToArrayHeuristics = new CollectionToArrayHeuristics(
                processingEnv, this);

        dependentTypes = new DependentTypes(checker, root);

        postInit();
    }

    // handle dependent types
    @Override
    public void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        super.annotateImplicit(tree, type);
        substituteUnused(tree, type);
        dependentTypes.handle(tree, type);
    }

    // handle dependent types
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(
            NewClassTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> fromUse = super
                .constructorFromUse(tree);
        AnnotatedExecutableType constructor = fromUse.first;
        dependentTypes.handleConstructor(tree,
                generalFactory.getAnnotatedType(tree), constructor);
        return fromUse;
    }

    @Override
    public Set<VariableTree> getUninitializedInvariantFields(
            NullnessStore store, TreePath path, boolean isStatic,
            List<? extends AnnotationMirror> receiverAnnotations) {
        Set<VariableTree> candidates = super.getUninitializedInvariantFields(
                store, path, isStatic, receiverAnnotations);
        Set<VariableTree> result = new HashSet<>();
        for (VariableTree c : candidates) {
            AnnotatedTypeMirror type = getAnnotatedType(c);
            boolean isPrimitive = TypesUtils.isPrimitive(type
                    .getUnderlyingType());
            if (!isPrimitive) {
                // primitives do not need to be initialized
                result.add(c);
            }
        }
        return result;
    }

    @Override
    protected NullnessAnalysis createFlowAnalysis(
            AbstractNullnessChecker checker,
            List<Pair<VariableElement, NullnessValue>> fieldValues) {
        return new NullnessAnalysis(this, processingEnv, checker, fieldValues);
    }

    @Override
    public NullnessTransfer createFlowTransferFunction(NullnessAnalysis analysis) {
        return new NullnessTransfer(analysis);
    };

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

    protected AnnotatedTypeMirror getDeclaredAndDefaultedAnnotatedType(Tree tree) {
        HACK_DONT_CALL_POST_AS_MEMBER = true;
        shouldCache = false;

        AnnotatedTypeMirror type = getAnnotatedType(tree);

        shouldCache = true;
        HACK_DONT_CALL_POST_AS_MEMBER = false;

        return type;
    }

    @Override
    protected TypeAnnotator createTypeAnnotator(AbstractNullnessChecker checker) {
        return new NonNullTypeAnnotator(checker);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator(AbstractNullnessChecker checker) {
        return new NonNullTreeAnnotator(checker);
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

    private static boolean isExceptionParameter(IdentifierTree node) {
        Element elt = TreeUtils.elementFromUse(node);
        assert elt != null;
        return elt.getKind() == ElementKind.EXCEPTION_PARAMETER;
    }

    protected class NonNullTreeAnnotator
            extends
            InitializationAnnotatedTypeFactory<AbstractNullnessChecker, NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTreeAnnotator {
        public NonNullTreeAnnotator(BaseTypeChecker checker) {
            super(checker);
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
        public Void visitIdentifier(IdentifierTree node,
                AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;

            // case 8. static method access
            annotateIfStatic(elt, type);

            // Workaround: exception parameters should be implicitly
            // NonNull, but due to a compiler bug they have
            // kind PARAMETER instead of EXCEPTION_PARAMETER. Until it's
            // fixed we manually inspect enclosing catch blocks.
            // case 9. exception parameter
            if (isExceptionParameter(node))
                type.replaceAnnotation(NONNULL);

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
            return null; // super.visitCompoundAssignment(node, type);
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

    protected class NonNullTypeAnnotator
            extends
            InitializationAnnotatedTypeFactory<AbstractNullnessChecker, NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTypeAnnotator {
        public NonNullTypeAnnotator(BaseTypeChecker checker) {
            super(checker);
        }
    }

    private boolean substituteUnused(Tree tree, AnnotatedTypeMirror type) {
        if (tree.getKind() != Tree.Kind.MEMBER_SELECT
            && tree.getKind() != Tree.Kind.IDENTIFIER)
            return false;

        Element field = InternalUtils.symbol(tree);
        if (field == null || field.getKind() != ElementKind.FIELD)
            return false;

        AnnotationMirror unused = getDeclAnnotation(field, Unused.class);
        if (unused == null) {
            return false;
        }

        Name whenName = AnnotationUtils.getElementValueClassName(unused, "when", false);
        MethodTree method = TreeUtils.enclosingMethod(this.getPath(tree));
        if (TreeUtils.isConstructor(method)) {
            /* TODO: this is messy and should be cleaned up.
             * The problem is that "receiver" means something different in
             * constructors and methods. Should we adapt .getReceiverType to do something
             * different in constructors vs. methods?
             * Or should we change this annotation into a declaration annotation?
             */
            com.sun.tools.javac.code.Symbol meth =
                    (com.sun.tools.javac.code.Symbol)TreeUtils.elementFromDeclaration(method);
            com.sun.tools.javac.util.List<TypeCompound> retannos = meth.getTypeAnnotationMirrors();
            if (retannos == null) {
                return false;
            }
            boolean matched = false;
            for (TypeCompound anno :  retannos) {
                if (anno.getAnnotationType().toString().equals(whenName.toString())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        } else {
            AnnotatedTypeMirror receiver = generalFactory.getReceiverType((ExpressionTree)tree);
            Elements elements = processingEnv.getElementUtils();
            if (receiver == null || !receiver.hasAnnotation(elements.getName(whenName))) {
                return false;
            }
        }
        type.replaceAnnotation(NULLABLE);
        return true;
    }
}
