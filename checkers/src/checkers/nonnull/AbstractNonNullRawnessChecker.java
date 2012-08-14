package checkers.nonnull;

import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.NonRaw;
import checkers.initialization.quals.Raw;
import checkers.nonnull.quals.MonotonicNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
    NonRaw.class, Raw.class, FBCBottom.class })
@SupportedLintOptions({ "strictMonotonicNonNullInit" })
public class AbstractNonNullRawnessChecker extends AbstractNonNullChecker {

    public AbstractNonNullRawnessChecker() {
        super(false);
    }

}
