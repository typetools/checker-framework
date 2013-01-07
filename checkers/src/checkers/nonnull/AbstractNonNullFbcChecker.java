package checkers.nonnull;

import checkers.initialization.quals.Committed;
import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.Free;
import checkers.initialization.quals.Unclassified;
import checkers.nonnull.quals.MonotonicNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.nonnull.quals.PolyNull;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        Free.class, Committed.class, Unclassified.class, FBCBottom.class,
        PolyNull.class })
@SupportedLintOptions({ "strictMonotonicNonNullInit",
// Temporary option to forbid non-null array component types,
// which is allowed by default.
// Forbidding is sound and will eventually be the only possibility.
// Allowing is unsound but permitted until flow-sensitivity changes are made.
        "arrays:forbidnonnullcomponents" })
public class AbstractNonNullFbcChecker extends AbstractNonNullChecker {

    public AbstractNonNullFbcChecker() {
        super(true);
    }

}
