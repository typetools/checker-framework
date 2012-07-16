package checkers.nullness;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.quals.PolyAll;
import checkers.source.*;
import checkers.types.*;
import checkers.util.AnnotationUtils;

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
@SupportedLintOptions({"nulltest", "uninitialized", "advancedchecks"})
public class NullnessSubchecker extends BaseTypeChecker {

    // warn about uninitialized primitive and nullable fields in the constructor
    public static final boolean UNINIT_DEFAULT = false;
    // warn about a null check performed against a value that is guaranteed
    // to be non-null, as in:  "m" == null.
    public static final boolean NULLTEST_DEFAULT = false;

    // TODO: This lint option should only be temporary, until all checks are implemented correctly.
    public static final boolean ADVANCEDCHECKS_DEFAULT = false;

    protected AnnotationMirror NONNULL, NULLABLE, PRIMITIVE;

    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        super.initChecker(processingEnv);
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        NONNULL = annoFactory.fromClass(NonNull.class);
        NULLABLE = annoFactory.fromClass(Nullable.class);
        PRIMITIVE = annoFactory.fromClass(Primitive.class);
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

    /*
     * TODO: it's ugly that this method cannot be in the TypeHierarchy, as these methods
     * are final there. Try to refactor this.
     */
    @Override
    public boolean isSubtype(AnnotatedTypeMirror sub, AnnotatedTypeMirror sup) {
        // @Primitive and @NonNull are interchangeable
        if (sub.getEffectiveAnnotations().contains(PRIMITIVE) &&
                sup.getEffectiveAnnotations().contains(NONNULL)) {
            return true;
        }
        return super.isSubtype(sub, sup);
    }

    /*
     * TODO: actually use the MultiGraphQH and incorporate rawness.
    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new NullnessQualifierHierarchy((MultiGraphQualifierHierarchy)super.createQualifierHierarchy());
    }

    private final class NullnessQualifierHierarchy extends MultiGraphQualifierHierarchy {
        public NullnessQualifierHierarchy(MultiGraphQualifierHierarchy hierarchy) {
            super(hierarchy);
        }
    }
    */
}
