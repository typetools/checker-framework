package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Tracks whether a value is initialized (all its fields are set), and checks that values are
 * initialized before being used. Supports two different type systems for initialization:
 * freedom-before-commitment (which is generally preferred) and rawness.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
public abstract class InitializationChecker extends BaseTypeChecker {

    /**
     * Should the initialization type system be FBC? If not, the rawness type system is used for
     * initialization.
     */
    public final boolean useFbc;

    public InitializationChecker(boolean useFbc) {
        this.useFbc = useFbc;
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        if (useFbc) {
            // This key suppresses *all* warnings, not just those related to initialization.
            result.add("initialization");
            result.add("fbc");
        } else {
            result.add("rawness");
        }
        return result;
    }

    /** Returns a list of all fields of the given class. */
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
