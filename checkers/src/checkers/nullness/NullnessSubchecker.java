package checkers.nullness;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.source.*;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

/**
 * A typechecker plug-in for the Nullness type system qualifier that finds (and
 * verifies the absence of) null-pointer errors.
 *
 * @see NonNull
 * @see Nullable
 * @see Raw
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifiers({ Nullable.class, LazyNonNull.class, NonNull.class, PolyNull.class, Primitive.class })
@SupportedLintOptions({"nulltest", "uninitialized", "advancedchecks"})
public class NullnessSubchecker extends BaseTypeChecker {

    // warn about uninitialized primitive and nullable fields in the constructor
    public static final boolean UNINIT_DEFAULT = false;
    // warn about a null check performed against a value that is guaranteed
    // to be non-null, as in:  "m" == null.
    public static final boolean NULLTEST_DEFAULT = false;

    // TODO: This lint option should only be temporary, until all checks are implemented correctly.
    public static final boolean ADVANCEDCHECKS_DEFAULT = false;

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // At most a single qualifier on a type
        if (useType.getAnnotations().size() > 1) {
            return false;
        }
        return super.isValidUse(declarationType, useType);
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        // No explicit qualifiers on primitive types
        if (type.getAnnotations().size()>1 ||
             (type.getAnnotation(Primitive.class)==null &&
             // Flow inference might implicitly add a NonNull
             !type.getElement().getAnnotationMirrors().isEmpty())) {
            return false;
        }
        return super.isValidUse(type);
    }

    @Override
    public boolean isSubtype(AnnotatedTypeMirror sub, AnnotatedTypeMirror sup) {
        // System.out.println("sub: " + sub + " sup: " + sup + " result: " + super.isSubtype(sub,sup));
        // @Primitive and @NonNull are interchangeable
        if (sub.getAnnotation(Primitive.class)!=null &&
                sup.getAnnotation(NonNull.class)!=null) {
            return true;
        }
        return super.isSubtype(sub, sup);
    }
}
