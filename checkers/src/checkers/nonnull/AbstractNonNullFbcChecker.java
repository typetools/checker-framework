package checkers.nonnull;

import checkers.initialization.quals.Committed;
import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.Free;
import checkers.initialization.quals.Unclassified;
import checkers.nonnull.quals.MonotonicNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class, Free.class,
    Committed.class, Unclassified.class, FBCBottom.class })
@SupportedLintOptions({ "strictMonotonicNonNullInit" })
public class AbstractNonNullFbcChecker extends AbstractNonNullChecker {

    public AbstractNonNullFbcChecker() {
        super(true);
    }

}
