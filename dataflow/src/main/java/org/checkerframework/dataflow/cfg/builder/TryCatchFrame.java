package org.checkerframework.dataflow.cfg.builder;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.util.Types;
import org.checkerframework.javacutil.Pair;

/**
 * A TryCatchFrame contains an ordered list of catch labels that apply to exceptions with specific
 * types.
 */
class TryCatchFrame implements TryFrame {
    /** The Types utilities. */
    protected final Types types;

    /** An ordered list of pairs because catch blocks are ordered. */
    protected final List<Pair<TypeMirror, Label>> catchLabels;

    /**
     * Construct a TryCatchFrame.
     *
     * @param types the Types utilities
     * @param catchLabels the catch labels
     */
    public TryCatchFrame(Types types, List<Pair<TypeMirror, Label>> catchLabels) {
        this.types = types;
        this.catchLabels = catchLabels;
    }

    @Override
    public String toString() {
        if (this.catchLabels.isEmpty()) {
            return "TryCatchFrame: no catch labels.";
        } else {
            StringJoiner sb = new StringJoiner(System.lineSeparator(), "TryCatchFrame: ", "");
            for (Pair<TypeMirror, Label> ptml : this.catchLabels) {
                sb.add(ptml.first.toString() + " -> " + ptml.second.toString());
            }
            return sb.toString();
        }
    }

    /**
     * Given a type of thrown exception, add the set of possible control flow successor {@link
     * Label}s to the argument set. Return true if the exception is known to be caught by one of
     * those labels and false if it may propagate still further.
     */
    @Override
    public boolean possibleLabels(TypeMirror thrown, Set<Label> labels) {
        // A conservative approach would be to say that every catch block
        // might execute for any thrown exception, but we try to do better.
        //
        // We rely on several assumptions that seem to hold as of Java 7.
        // 1) An exception parameter in a catch block must be either
        //    a declared type or a union composed of declared types,
        //    all of which are subtypes of Throwable.
        // 2) A thrown type must either be a declared type or a variable
        //    that extends a declared type, which is a subtype of Throwable.
        //
        // Under those assumptions, if the thrown type (or its bound) is
        // a subtype of the caught type (or one of its alternatives), then
        // the catch block must apply and none of the later ones can apply.
        // Otherwise, if the thrown type (or its bound) is a supertype
        // of the caught type (or one of its alternatives), then the catch
        // block may apply, but so may later ones.
        // Otherwise, the thrown type and the caught type are unrelated
        // declared types, so they do not overlap on any non-null value.

        while (!(thrown instanceof DeclaredType)) {
            assert thrown instanceof TypeVariable
                    : "thrown type must be a variable or a declared type";
            thrown = ((TypeVariable) thrown).getUpperBound();
        }
        DeclaredType declaredThrown = (DeclaredType) thrown;
        assert thrown != null : "thrown type must be bounded by a declared type";

        for (Pair<TypeMirror, Label> pair : catchLabels) {
            TypeMirror caught = pair.first;
            boolean canApply = false;

            if (caught instanceof DeclaredType) {
                DeclaredType declaredCaught = (DeclaredType) caught;
                if (types.isSubtype(declaredThrown, declaredCaught)) {
                    // No later catch blocks can apply.
                    labels.add(pair.second);
                    return true;
                } else if (types.isSubtype(declaredCaught, declaredThrown)) {
                    canApply = true;
                }
            } else {
                assert caught instanceof UnionType
                        : "caught type must be a union or a declared type";
                UnionType caughtUnion = (UnionType) caught;
                for (TypeMirror alternative : caughtUnion.getAlternatives()) {
                    assert alternative instanceof DeclaredType
                            : "alternatives of an caught union type must be declared types";
                    DeclaredType declaredAlt = (DeclaredType) alternative;
                    if (types.isSubtype(declaredThrown, declaredAlt)) {
                        // No later catch blocks can apply.
                        labels.add(pair.second);
                        return true;
                    } else if (types.isSubtype(declaredAlt, declaredThrown)) {
                        canApply = true;
                    }
                }
            }

            if (canApply) {
                labels.add(pair.second);
            }
        }

        return false;
    }
}
