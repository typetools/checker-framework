package checkers.nonnull;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeVisitor;
import checkers.initialization.InitializationChecker;
import checkers.nonnull.quals.MonoNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import com.sun.source.tree.CompilationUnitTree;

// TODO: Suppress "fields.uninitialized" warning if the cause for a field not being
//		 initialized is a type error (the uninitialized error shows up before the
//		 error that actually caused the problem, which is confusing)
// TODO/later: Add "CommittedOnly" and adapt logic to support either as default, and only one annotation can be present, only present on fields

// DONE: Stefan: don't allow casts between initialization types.

public abstract class AbstractNonNullChecker extends InitializationChecker {

    /** Annotation constants */
    public AnnotationMirror NONNULL, NULLABLE, MONONONNULL;

    public static final boolean LINT_DEFAULT_STRICTMONOTONICNONNULLINIT = false;

    public AbstractNonNullChecker(boolean useFbc) {
        super(useFbc);
    }

    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        AnnotationUtils annoFactory = AnnotationUtils
                .getInstance(processingEnv);
        NONNULL = annoFactory.fromClass(NonNull.class);
        NULLABLE = annoFactory.fromClass(Nullable.class);
        MONONONNULL = annoFactory.fromClass(MonoNonNull.class);

        super.initChecker(processingEnv);
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
        result.add(MONONONNULL);
        result.add(NULLABLE);
        return result;
    }

    @Override
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        Set<Class<? extends Annotation>> l = new HashSet<>(
                super.getInvalidConstructorReturnTypeAnnotations());
        l.add(NonNull.class);
        l.add(Nullable.class);
        l.add(MonoNonNull.class);
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return NONNULL;
    }

    @Override
    protected QualifierHierarchy getChildQualifierHierarchy() {
        MultiGraphFactory factory = new GraphQualifierHierarchy.GraphFactory(
                this);
        Set<Class<? extends Annotation>> supportedTypeQualifiers = new HashSet<>();
        supportedTypeQualifiers.add(NonNull.class);
        supportedTypeQualifiers.add(Nullable.class);
        supportedTypeQualifiers.add(MonoNonNull.class);
        return createQualifierHierarchy(supportedTypeQualifiers,
                AnnotationUtils.getInstance(env), factory);
    }
}
