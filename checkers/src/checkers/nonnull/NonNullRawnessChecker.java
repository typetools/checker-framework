package checkers.nonnull;

import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.NonRaw;
import checkers.initialization.quals.Raw;
import checkers.nonnull.quals.MonoNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@TypeQualifiers({ Nullable.class, MonoNonNull.class, NonNull.class,
    NonRaw.class, Raw.class, FBCBottom.class })
@SupportedLintOptions({ "strictmonoinit" })
public class NonNullRawnessChecker extends AbstractNonNullChecker {

    public NonNullRawnessChecker() {
        super(false);
    }

}
