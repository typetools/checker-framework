package checkers.nonnull;

import checkers.initialization.quals.NonRaw;
import checkers.initialization.quals.Raw;
import checkers.nonnull.quals.MonotonicNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.nonnull.quals.PolyNull;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        NonRaw.class, Raw.class, PolyNull.class, PolyAll.class })
@SupportedLintOptions({ AbstractNonNullChecker.LINT_STRICTMONOTONICNONNULLINIT,
        AbstractNonNullChecker.LINT_REDUNDANTNULLCOMPARISON,
        // Temporary option to forbid non-null array component types,
        // which is allowed by default.
        // Forbidding is sound and will eventually be the only possibility.
        // Allowing is unsound but permitted until flow-sensitivity changes are
        // made.
        "arrays:forbidnonnullcomponents" })
public class AbstractNonNullRawnessChecker extends AbstractNonNullChecker {

    public AbstractNonNullRawnessChecker() {
        super(false);
    }

}
