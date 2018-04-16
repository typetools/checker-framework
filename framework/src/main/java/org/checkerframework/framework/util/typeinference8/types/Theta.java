package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.typemirror.VariableTypeMirror;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;

/** A mapping from type variable to inference variable. */
public class Theta extends HashMap<TypeVariable, Variable> {
    private static final long serialVersionUID = 42L;

    private final List<Entry<TypeVariable, Variable>> entryList = new ArrayList<>();

    @Override
    public Variable put(TypeVariable key, Variable value) {
        assert !this.containsKey(key) || this.get(key).equals(value);
        if (!this.containsKey(key)) {
            entryList.add(new SimpleEntry<>(key, value));
        }
        return super.put(key, value);
    }

    public List<Entry<TypeVariable, Variable>> getEntryList() {
        return entryList;
    }

    @Override
    public Set<Entry<TypeVariable, Variable>> entrySet() {
        return super.entrySet();
    }

    @Override
    public Set<TypeVariable> keySet() {
        return super.keySet();
    }

    /**
     * If a mapping for {@code invocation} doesn't exist create it by:
     *
     * <p>Creates inference variables for the type parameters to {@code methodType} for a particular
     * {@code invocation}. Initializes the bounds of the variables. Returns a mapping from type
     * variables to newly created variables.
     *
     * <p>Otherwise, returns the previously created mapping.
     *
     * @param invocation method or constructor invocation
     * @param methodType type of generic method
     * @param context Java8InferenceContext
     * @return a mapping of the type variables of {@code methodType} to inference variables
     */
    public static Theta create(
            ExpressionTree invocation, InvocationType methodType, Java8InferenceContext context) {
        if (context.maps.containsKey(invocation)) {
            return context.maps.get(invocation);
        }
        Theta map = new Theta();
        for (TypeVariable pl : methodType.getTypeVariables()) {
            Variable al = new VariableTypeMirror(pl, invocation, context);
            map.put(pl, al);
        }
        if (TreeUtils.isDiamondTree(invocation)) {
            DeclaredType classType =
                    (DeclaredType)
                            ElementUtils.enclosingClass(
                                            TreeUtils.elementFromUse((NewClassTree) invocation))
                                    .asType();
            for (TypeMirror typeMirror : classType.getTypeArguments()) {
                if (typeMirror.getKind() != TypeKind.TYPEVAR) {
                    ErrorReporter.errorAbort("Expected type variable, found: %s", typeMirror);
                    return map;
                }
                TypeVariable pl = (TypeVariable) typeMirror;
                Variable al = new VariableTypeMirror(pl, invocation, context);
                map.put(pl, al);
            }
        }

        for (Variable v : map.values()) {
            v.initialBounds(map);
        }
        context.maps.put(invocation, map);
        return map;
    }
}
