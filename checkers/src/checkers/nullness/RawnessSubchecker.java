package checkers.nullness;

import java.util.ArrayList;
import java.util.Collection;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.TypeQualifiers;

/**
 * A typechecker that is part of the Nullness type system.
 * The Rawness Subchecker checks whether an object has been fully
 * initialized by the constructor.  If not, then some fields (that are
 * annotated as @NonNull may still be null.
 *
 * @see NonNull
 * @see Nullable
 * @see Raw
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifiers({ Raw.class, NonRaw.class, PolyRaw.class })
public class RawnessSubchecker extends BaseTypeChecker {
    @Override
    public Collection<String> getSuppressWarningsKey() {
        Collection<String> lst = new ArrayList<String>();
        lst.addAll(super.getSuppressWarningsKey());
        lst.add("nullness");
        return lst;
    }
}
