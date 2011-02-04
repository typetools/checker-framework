package checkers.nullness;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.source.*;

/**
 * A typechecker plug-in for the Nullness type system qualifier that finds (and
 * verifies the absence of) null-pointer errors.
 *
 * @see NonNull
 * @see Nullable
 * @see Raw
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifiers({ Nullable.class, LazyNonNull.class, NonNull.class, PolyNull.class })
@SupportedLintOptions({"nulltest", "uninitialized", "inferFromAsserts", "advancedchecks"})
public class NullnessSubchecker extends BaseTypeChecker {

    public static final boolean UNINIT_DEFAULT = false;
    public static final boolean NULLTEST_DEFAULT = false;

    // TODO: This lint option should only be temporary, until all checks are implemented correctly.
    public static final boolean ADVANCEDCHECKS_DEFAULT = false;

}
