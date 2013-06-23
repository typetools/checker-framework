package checkers.nullness;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
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
import checkers.types.QualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import com.sun.source.tree.CompilationUnitTree;

/**
 * An implementation of the nullness type-system based on an initialization
 * type-system for safe initialization.
 */
public abstract class AbstractNullnessChecker extends InitializationChecker<NullnessAnnotatedTypeFactory> {

    /** Annotation constants */
    public AnnotationMirror NONNULL, NULLABLE, MONOTONICNONNULL;

    /**
     * Should we be strict about initialization of {@link MonotonicNonNull} variables.
     */
    public static final String LINT_STRICTMONOTONICNONNULLINIT = "strictMonotonicNonNullInit";

    /**
     * Default for {@link #LINT_STRICTMONOTONICNONNULLINIT}.
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
    public static final boolean LINT_DEFAULT_REDUNDANTNULLCOMPARISON = false;

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
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        result.add("nullness");
        return result;
    }

    @Override
    protected BaseTypeVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new NullnessVisitor(this, root);
    }

    @Override
    public NullnessAnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new NullnessAnnotatedTypeFactory(this, root);
    }

    // Cache for the nullness annotations
    protected Set<Class<? extends Annotation>> nullnessAnnos;

    /**
     * @return The list of annotations of the non-null type system.
     */
    public Set<Class<? extends Annotation>> getNullnessAnnotations() {
        if (nullnessAnnos == null) {
            Set<Class<? extends Annotation>> result = new HashSet<>();
            result.add(NonNull.class);
            result.add(MonotonicNonNull.class);
            result.add(Nullable.class);
            result.add(PolyNull.class);
            result.add(PolyAll.class);
            nullnessAnnos = Collections.unmodifiableSet(result);
        }
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
        return NONNULL;
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
        public boolean isSubtype(AnnotationMirror anno1, AnnotationMirror anno2) {
            if (isInitializationAnnotation(anno1) ||
                    isInitializationAnnotation(anno2)) {
                return this.isSubtypeInitialization(anno1, anno2);
            }
            return super.isSubtype(anno1, anno2);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror anno1, AnnotationMirror anno2) {
            if (isInitializationAnnotation(anno1) ||
                    isInitializationAnnotation(anno2)) {
                return this.leastUpperBoundInitialization(anno1, anno2);
            }
            return super.leastUpperBound(anno1, anno2);
        }
    }
}
