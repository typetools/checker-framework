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
 * initialized before being used. Implements the freedom-before-commitment scheme for
 * initialization, augmented by type frames.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
public abstract class InitializationChecker extends BaseTypeChecker {

    /** Create a new InitializationChecker. */
    public InitializationChecker() {}

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        // This key suppresses *all* warnings, not just those related to initialization.
        result.add("initialization");
        result.add("fbc");
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
