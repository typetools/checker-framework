package checkers.nonnull;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import javacutils.AnnotationUtils;

import checkers.basetype.BaseTypeVisitor;
import checkers.initialization.InitializationChecker;
import checkers.nonnull.quals.MonotonicNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import com.sun.source.tree.CompilationUnitTree;

// TODO: Suppress "fields.uninitialized" warning if the cause for a field not being
//		 initialized is a type error (the uninitialized error shows up before the
//		 error that actually caused the problem, which is confusing)
// TODO/later: Add "CommittedOnly" and adapt logic to support either as default, and only one annotation can be present, only present on fields

// DONE: Stefan: don't allow casts between initialization types.

public abstract class AbstractNonNullChecker extends InitializationChecker {

    /** Annotation constants */
    public AnnotationMirror NONNULL, NULLABLE, MONOTONICNONNULL;

    public static final boolean LINT_DEFAULT_STRICTMONOTONICNONNULLINIT = false;

    public AbstractNonNullChecker(boolean useFbc) {
        super(useFbc);
    }

    @Override
    public void initChecker() {
        Elements elements = processingEnv.getElementUtils();
        NONNULL = AnnotationUtils.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(elements, Nullable.class);
        MONOTONICNONNULL = AnnotationUtils.fromClass(elements, MonotonicNonNull.class);
        super.initChecker();
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
        result.add(MONOTONICNONNULL);
        result.add(NULLABLE);
        return result;
    }

    @Override
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        Set<Class<? extends Annotation>> l = new HashSet<>(
                super.getInvalidConstructorReturnTypeAnnotations());
        l.add(NonNull.class);
        l.add(Nullable.class);
        l.add(MonotonicNonNull.class);
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return NONNULL;
    }

    @Override
    protected QualifierHierarchy getChildQualifierHierarchy() {
        MultiGraphFactory factory = new MultiGraphFactory(this);
        Set<Class<? extends Annotation>> supportedTypeQualifiers = new HashSet<>();
        supportedTypeQualifiers.add(NonNull.class);
        supportedTypeQualifiers.add(Nullable.class);
        supportedTypeQualifiers.add(MonotonicNonNull.class);
        return createQualifierHierarchy(processingEnv.getElementUtils(), supportedTypeQualifiers, factory);
    }
}
