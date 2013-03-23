package checkers.nullness;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javacutils.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.basetype.BaseTypeVisitor;
import checkers.initialization.InitializationChecker;
import checkers.nullness.quals.MonotonicNonNull;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.PolyAll;
import checkers.source.SuppressWarningsKeys;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import com.sun.source.tree.CompilationUnitTree;

@SuppressWarningsKeys("nullness")
public abstract class AbstractNullnessChecker extends InitializationChecker {

    /** Annotation constants */
    public AnnotationMirror NONNULL, NULLABLE, MONOTONICNONNULL;

    /**
     * Default for {@link #LINT_REDUNDANTNULLCOMPARISON}.
     */
    public static final String LINT_STRICTMONOTONICNONNULLINIT = "strictMonotonicNonNullInit";

    /**
     * Should we be strict about initialization of {@link MonotonicNonNull} variables.
     */
    public static final boolean LINT_DEFAULT_STRICTMONOTONICNONNULLINIT = false;

    /**
     * Warn about redundant comparisons of expressions with {@code null}, if the
     * expressions is known to be non-null.
     */
    public static final String LINT_REDUNDANTNULLCOMPARISON = "redundantNullComparison";

    /**
     * Default for {@link #LINT_REDUNDANTNULLCOMPARISON}.
     */
    public static final boolean LINT_DEFAULT_STRICTNULLCOMPARISON = false;

    public AbstractNullnessChecker(boolean useFbc) {
        super(useFbc);
    }

    @Override
    public void initChecker() {
        Elements elements = processingEnv.getElementUtils();
        NONNULL = AnnotationUtils.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(elements, Nullable.class);
        MONOTONICNONNULL = AnnotationUtils.fromClass(elements,
                MonotonicNonNull.class);
        super.initChecker();
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>();
        result.addAll(super.getSuppressWarningsKeys());
        result.add("nonnull");
        return result;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor(CompilationUnitTree root) {
        return new NullnessVisitor(this, root);
    }

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new NullnessAnnotatedTypeFactory(this, root);
    }

    /**
     * @return The list of annotations of the non-null type system.
     */
    public Set<AnnotationMirror> getNonNullAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(NONNULL);
        result.add(MONOTONICNONNULL);
        result.add(NULLABLE);
        Elements elements = processingEnv.getElementUtils();
        result.add(AnnotationUtils.fromClass(elements, PolyNull.class));
        result.add(AnnotationUtils.fromClass(elements, PolyAll.class));
        return result;
    }

    @Override
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        Set<Class<? extends Annotation>> l = new HashSet<>(
                super.getInvalidConstructorReturnTypeAnnotations());
        l.add(NonNull.class);
        l.add(Nullable.class);
        l.add(MonotonicNonNull.class);
        l.add(PolyNull.class);
        l.add(PolyAll.class);
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
        supportedTypeQualifiers.add(PolyNull.class);
        supportedTypeQualifiers.add(PolyAll.class);
        return createQualifierHierarchy(processingEnv.getElementUtils(),
                supportedTypeQualifiers, factory);
    }
}
