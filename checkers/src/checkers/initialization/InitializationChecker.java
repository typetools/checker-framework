package checkers.initialization;

/*>>>
import checkers.interning.quals.*;
*/

import checkers.basetype.BaseTypeChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * The checker for the freedom-before-commitment type-system. Also supports
 * rawness as a type-system for tracking initialization, though FBC is
 * preferred.
 *
 * @author Stefan Heule
 */
public abstract class InitializationChecker extends BaseTypeChecker {

    /**
     * Should the initialization type system be FBC? If not, the rawness type
     * system is used for initialization.
     */
    public final boolean useFbc;

    public InitializationChecker(boolean useFbc) {
        this.useFbc = useFbc;
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        if (useFbc) {
            result.add("initialization");
            result.add("fbc");
            // Temporary, to make transition easier.
            result.add("rawness");
        } else {
            result.add("rawness");
        }
        return result;
    }

    /**
     * Returns a list of all fields of the given class
     */
    public static List<VariableTree> getAllFields(ClassTree clazz) {
        List<VariableTree> fields = new ArrayList<>();
        for (Tree t : clazz.getMembers()) {
            if (t.getKind().equals(Tree.Kind.VARIABLE)) {
                VariableTree vt = (VariableTree) t;
                fields.add(vt);
            }
        }
        return fields;
    }
}
