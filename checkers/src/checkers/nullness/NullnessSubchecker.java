package checkers.nullness;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.quals.PolyAll;
import checkers.source.*;
import checkers.types.*;
import checkers.util.AnnotationUtils;
import checkers.util.MultiGraphQualifierHierarchy;

/**
 * A typechecker plug-in for the Nullness type system qualifier that finds (and
 * verifies the absence of) null-pointer errors.
 *
 * @see NonNull
 * @see Nullable
 * @see Raw
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifiers({ Nullable.class, LazyNonNull.class, NonNull.class, Primitive.class,
    PolyNull.class, PolyAll.class})
@SupportedLintOptions({"nulltest", "uninitialized", "advancedchecks",
    // Temporary option to forbid non-null array component types,
    // which is allowed by default.
    // Forbidding is sound and will eventually be the only possibility.
    // Allowing is unsound but permitted until flow-sensitivity changes are made.
    "arrays:forbidnonnullcomponents"})
public class NullnessSubchecker extends BaseTypeChecker {

    // warn about uninitialized primitive and nullable fields in the constructor
    public static final boolean UNINIT_DEFAULT = false;
    // warn about a null check performed against a value that is guaranteed
    // to be non-null, as in:  "m" == null.
    public static final boolean NULLTEST_DEFAULT = false;

    // TODO: This lint option should only be temporary, until all checks are implemented correctly.
    public static final boolean ADVANCEDCHECKS_DEFAULT = false;

    protected AnnotationMirror NONNULL, NULLABLE, LAZYNONNULL, PRIMITIVE, POLYNULL;

    // The associated Rawness Checker.
    protected RawnessSubchecker rawnesschecker;

    @Override
    public void initChecker() {
        super.initChecker();

        Elements elements = processingEnv.getElementUtils();
        NONNULL = AnnotationUtils.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(elements, Nullable.class);
        LAZYNONNULL = AnnotationUtils.fromClass(elements, LazyNonNull.class);
        PRIMITIVE = AnnotationUtils.fromClass(elements, Primitive.class);
        POLYNULL = AnnotationUtils.fromClass(elements, PolyNull.class);

        rawnesschecker = new RawnessSubchecker();
        rawnesschecker.initChecker(this);
    }

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        // typeProcess is never called on the rawnesschecker.
        // We need to at least set the path.
        rawnesschecker.currentPath = this.currentPath;
        return new NullnessAnnotatedTypeFactory(this, rawnesschecker, root);
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new NullnessTypeHierarchy(this, getQualifierHierarchy());
    }

    class NullnessTypeHierarchy extends TypeHierarchy {

        public NullnessTypeHierarchy(BaseTypeChecker checker,
                QualifierHierarchy qualifierHierarchy) {
            super(checker, qualifierHierarchy);
        }

        @Override
        protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror sub, AnnotatedTypeMirror sup) {
            // @Primitive and @NonNull are interchangeable
            if (sub.getEffectiveAnnotations().contains(PRIMITIVE) &&
                    sup.getEffectiveAnnotations().contains(NONNULL)) {
                return true;
            }
            return super.isSubtypeAsTypeArgument(sub, sup);
        }

    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new NullnessQualifierHierarchy(factory);
    }

    private final class NullnessQualifierHierarchy extends MultiGraphQualifierHierarchy {
        public NullnessQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public boolean isSubtype(AnnotationMirror sub, AnnotationMirror sup) {
            // @Primitive and @NonNull are interchangeable, mostly.
            if (AnnotationUtils.areSame(sub, PRIMITIVE) &&
                    AnnotationUtils.areSame(sup, PRIMITIVE)) {
                return true;
            }
            if (AnnotationUtils.areSame(sub, PRIMITIVE)) {
                return this.isSubtype(NONNULL, sup);
            }
            if (AnnotationUtils.areSame(sup, PRIMITIVE)) {
                return this.isSubtype(sub, NONNULL);
            }
            return super.isSubtype(sub, sup);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, PRIMITIVE) &&
                    AnnotationUtils.areSame(a2, PRIMITIVE)) {
                return PRIMITIVE;
            }
            if (AnnotationUtils.areSame(a1, PRIMITIVE)) {
                return this.leastUpperBound(NONNULL, a2);
            }
            if (AnnotationUtils.areSame(a2, PRIMITIVE)) {
                return this.leastUpperBound(a1, NONNULL);
            }
            return super.leastUpperBound(a1, a2);
        }
    }
}
