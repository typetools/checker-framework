package checkers.nonnull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.CommitmentAnnotatedTypeFactory;
import checkers.flow.analysis.checkers.CFValue;
import checkers.nonnull.quals.MonoNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.nullness.quals.Primitive;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;
import checkers.util.ElementUtils;
import checkers.util.Pair;
import checkers.util.TreeUtils;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

public class NonNullAnnotatedTypeFactory
        extends
        CommitmentAnnotatedTypeFactory<NonNullChecker, NonNullTransfer, NonNullAnalysis> {

    /** Annotation constants */
    protected final AnnotationMirror NONNULL, NULLABLE, PRIMITIVE;

    public NonNullAnnotatedTypeFactory(NonNullChecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        NONNULL = this.annotations.fromClass(NonNull.class);
        NULLABLE = this.annotations.fromClass(Nullable.class);
        PRIMITIVE = this.annotations.fromClass(Primitive.class);

        // aliases with checkers.nullness.quals qualifiers
        addAliasedAnnotation(checkers.nullness.quals.NonNull.class, NONNULL);
        addAliasedAnnotation(checkers.nullness.quals.Nullable.class, NULLABLE);

        addAliasedAnnotation(checkers.nullness.quals.LazyNonNull.class,
                annotations.fromClass(MonoNonNull.class));

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

        Set<AnnotationMirror> localdef = new HashSet<AnnotationMirror>();
        localdef.add(NULLABLE);
        localdef.add(checker.createUnclassifiedAnnotation(Object.class));
        defaults.setLocalVariableDefault(localdef);

        postInit();
    }

    @Override
    protected NonNullAnalysis createFlowAnalysis(NonNullChecker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new NonNullAnalysis(this, env, checker, fieldValues);
    }

    protected AnnotatedTypeMirror getDeclaredAndDefaultedAnnotatedType(Tree tree) {
        HACK_DONT_CALL_POST_AS_MEMBER = true;
        SHOULD_CACHE = false;

        AnnotatedTypeMirror type = getAnnotatedType(tree);

        SHOULD_CACHE = true;
        HACK_DONT_CALL_POST_AS_MEMBER = false;

        return type;
    }

    @Override
    protected TypeAnnotator createTypeAnnotator(NonNullChecker checker) {
        return new NonNullTypeAnnotator(checker);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator(NonNullChecker checker) {
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
            CommitmentAnnotatedTypeFactory<NonNullChecker, NonNullTransfer, NonNullAnalysis>.CommitmentTreeAnnotator {
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
                type.addAnnotation(NONNULL);

            return super.visitIdentifier(node, type);
        }

    }

    protected class NonNullTypeAnnotator
            extends
            CommitmentAnnotatedTypeFactory<NonNullChecker, NonNullTransfer, NonNullAnalysis>.CommitmentTypeAnnotator {
        public NonNullTypeAnnotator(BaseTypeChecker checker) {
            super(checker);
        }
    }
}
