package checkers.nullness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

/**
 * A typechecker plug-in for the Nullness type system qualifier that finds (and
 * verifies the absence of) null-pointer errors.
 *
 * @see NonNull
 * @see Nullable
 * @see Raw
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifiers({ Raw.class, NonRaw.class, PolyRaw.class })
public class RawnessSubchecker extends BaseTypeChecker {
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        return true;
    }

    @Override
    public Collection<String> getSuppressWarningsKey() {
        Collection<String> lst = new ArrayList<String>();
        lst.addAll(super.getSuppressWarningsKey());
        lst.add("nullness");
        return lst;
    }
}
