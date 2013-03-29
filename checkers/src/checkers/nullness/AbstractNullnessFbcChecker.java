package checkers.nullness;

import java.util.Collection;
import java.util.HashSet;

import checkers.initialization.quals.Committed;
import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.Free;
import checkers.initialization.quals.Unclassified;
import checkers.nullness.quals.MonotonicNonNull;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        Free.class, Committed.class, Unclassified.class, FBCBottom.class,
        PolyNull.class, PolyAll.class })
@SupportedLintOptions({ AbstractNullnessChecker.LINT_STRICTMONOTONICNONNULLINIT,
        AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
        // Temporary option to forbid non-null array component types,
        // which is allowed by default.
        // Forbidding is sound and will eventually be the only possibility.
        // Allowing is unsound but permitted until flow-sensitivity changes are
        // made.
        "arrays:forbidnonnullcomponents" })
public class AbstractNullnessFbcChecker extends AbstractNullnessChecker {

    public AbstractNullnessFbcChecker() {
        super(true);
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>();
        result.addAll(super.getSuppressWarningsKeys());
        result.add("fbc");
        return result;
    }

}
