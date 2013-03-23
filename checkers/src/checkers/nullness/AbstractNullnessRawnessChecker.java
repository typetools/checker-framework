package checkers.nullness;

import checkers.initialization.quals.NonRaw;
import checkers.initialization.quals.Raw;
import checkers.nullness.quals.MonotonicNonNull;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        NonRaw.class, Raw.class, PolyNull.class, PolyAll.class })
@SupportedLintOptions({ AbstractNullnessChecker.LINT_STRICTMONOTONICNONNULLINIT,
        AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
        // Temporary option to forbid non-null array component types,
        // which is allowed by default.
        // Forbidding is sound and will eventually be the only possibility.
        // Allowing is unsound but permitted until flow-sensitivity changes are
        // made.
        "arrays:forbidnonnullcomponents" })
public class AbstractNullnessRawnessChecker extends AbstractNullnessChecker {

    public AbstractNullnessRawnessChecker() {
        super(false);
    }

}
