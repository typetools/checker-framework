package org.checkerframework.framework.util.typeinference8.typemirror.type;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A type that does not contain any inference variables. */
public class ProperTypeMirror extends AbstractTypeMirror implements ProperType {
    private final TypeMirror properType;

    ProperTypeMirror(TypeMirror properType, Java8InferenceContext context) {
        super(context);
        assert properType != null && context != null && properType.getKind() != TypeKind.VOID;
        this.properType = properType;
    }

    ProperTypeMirror(ExpressionTree tree, Java8InferenceContext context) {
        this(TreeUtils.typeOf(tree), context);
    }

    ProperTypeMirror(VariableTree varTree, Java8InferenceContext context) {
        this(TreeUtils.typeOf(varTree), context);
    }

    @Override
    public ProperTypeMirror create(TypeMirror properType) {
        return new ProperTypeMirror(properType, context);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProperTypeMirror otherProperType = (ProperTypeMirror) o;

        return properType == otherProperType.properType // faster
                || context.env
                        .getTypeUtils()
                        .isSameType(properType, otherProperType.properType); // slower
    }

    @Override
    public int hashCode() {
        int result = properType.toString().hashCode();
        result = 31 * result + Kind.PROPER.hashCode();
        return result;
    }

    @Override
    public TypeMirror getJavaType() {
        return properType;
    }

    @Override
    public boolean isObject() {
        return TypesUtils.isObject(properType);
    }

    @Override
    public Collection<Variable> getInferenceVariables() {
        return Collections.emptyList();
    }

    @Override
    public AbstractTypeMirror applyInstantiations(List<Variable> instantiations) {
        return this;
    }

    @Override
    public ProperType boxType() {
        if (properType.getKind().isPrimitive()) {
            return new ProperTypeMirror(
                    context.types.boxedClass((Type) properType).asType(), context);
        }
        return this;
    }

    @Override
    public String toString() {
        return properType.toString();
    }
}
