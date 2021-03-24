package org.checkerframework.dataflow.cfg.builder;

import com.sun.source.tree.Tree;
import com.sun.source.tree.UnionTypeTree;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A TryCatchFrame contains an ordered list of catch labels that apply to exceptions with specific
 * types.
 */
class TryCatchFrame implements TryFrame {
    /** The Types utilities. */
    protected final Types types;

    /**
     * A mapping from type tree (of a catch clause) to label. It is ordered list because catch
     * blocks are ordered.
     */
    protected final List<Pair<Tree, Label>> catchLabels;

    /**
     * Construct a TryCatchFrame.
     *
     * @param types the Types utilities
     * @param catchLabels a mapping from type tree (of a catch clause) to label
     */
    public TryCatchFrame(Types types, List<Pair<Tree, Label>> catchLabels) {
        this.types = types;
        this.catchLabels = catchLabels;
    }

    @Override
    public String toString() {
        if (this.catchLabels.isEmpty()) {
            return "TryCatchFrame: no catch labels.";
        } else {
            StringJoiner sb = new StringJoiner(System.lineSeparator(), "TryCatchFrame: ", "");
            for (Pair<Tree, Label> ptml : this.catchLabels) {
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
        // 2) A thrown type must either be a declared type or a type variable
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

        for (Pair<Tree, Label> pair : catchLabels) {
            Tree catchTypeTree = pair.first;
            Tree.Kind catchTypeKind = catchTypeTree.getKind();

            boolean canApply = false;

            if (catchTypeKind != Tree.Kind.UNION_TYPE) {
                DeclaredType caught = (DeclaredType) TreeUtils.typeOf(catchTypeTree);
                if (types.isSubtype(declaredThrown, caught)) {
                    // No later catch blocks can apply.
                    labels.add(pair.second);
                    return true;
                } else if (types.isSubtype(caught, declaredThrown)) {
                    canApply = true;
                }
            } else {
                // catchTypeKind == Tree.Kind.UNION_TYPE

                // Ideally `TreeUtils.typeOf(catchTypeTree)` would be a UnionType, but I have
                // observed it being a DeclaredType that is the LUB of the alternatives, so
                // process each alternative individually.
                UnionTypeTree unionTypeTree = (UnionTypeTree) catchTypeTree;

                for (Tree alternativeTree : unionTypeTree.getTypeAlternatives()) {
                    TypeMirror alternative = TreeUtils.typeOf(alternativeTree);
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
