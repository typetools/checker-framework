package checkers.nonnull;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeVisitor;
import checkers.commitment.CommitmentChecker;
import checkers.commitment.quals.Committed;
import checkers.commitment.quals.FBCBottom;
import checkers.commitment.quals.Free;
import checkers.commitment.quals.Unclassified;
import checkers.nonnull.quals.MonoNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.MultiGraphQualifierHierarchy;

import com.sun.source.tree.CompilationUnitTree;

// TODO: Suppress "fields.uninitialized" warning if the cause for a field not being
//		 initialized is a type error (the uninitialized error shows up before the
//		 error that actually caused the problem, which is confusing)
// TODO/later: Add "CommittedOnly" and adapt logic to support either as default, and only one annotation can be present, only present on fields

// DONE: Stefan: don't allow casts between initialization types.

@TypeQualifiers({ Nullable.class, MonoNonNull.class, NonNull.class, Free.class, Committed.class,
        Unclassified.class, FBCBottom.class })
@SupportedLintOptions({"strictmonoinit"})
public class NonNullChecker extends CommitmentChecker {

    /** Annotation constants */
    public AnnotationMirror NONNULL, NULLABLE, MONONONNULL;

    public static final boolean LINT_DEFAULT_STRICTMONOINIT = false;

    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        super.initChecker(processingEnv);
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

        NONNULL = annoFactory.fromClass(NonNull.class);
        NULLABLE = annoFactory.fromClass(Nullable.class);
        MONONONNULL = annoFactory.fromClass(MonoNonNull.class);
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor(CompilationUnitTree root) {
        return new NonNullVisitor(this, root);
    }

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new NonNullAnnotatedTypeFactory(this, root);
    }

    /**
     * @return The list of annotations of the non-null type system.
     */
    public Set<AnnotationMirror> getNonNullAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(NONNULL);
        result.add(NULLABLE);
        return result;
    }

    @Override
    public Set<AnnotationMirror> getInvalidConstructorReturnTypeAnnotations() {
        Set<AnnotationMirror> l = new HashSet<>(
                super.getInvalidConstructorReturnTypeAnnotations());
        l.addAll(getNonNullAnnotations());
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return NONNULL;
    }
}
