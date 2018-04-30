package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class ProperType extends AbstractType {
    private final AnnotatedTypeMirror type;
    private final TypeMirror properType;

    public ProperType(
            AnnotatedTypeMirror type, TypeMirror properType, Java8InferenceContext context) {
        super(context);
        assert properType != null && properType.getKind() != TypeKind.VOID && type != null;
        if (TypesUtils.isCaptured(properType) && type.getKind() == TypeKind.WILDCARD) {
            type = ((AnnotatedWildcardType) type).capture((TypeVariable) properType);
        }

        assert properType.getKind() == type.getKind();

        this.properType = properType;
        this.type = type;
    }

    public ProperType(ExpressionTree tree, Java8InferenceContext context) {
        this(context.typeFactory.getAnnotatedType(tree), TreeUtils.typeOf(tree), context);
    }

    public ProperType(VariableTree varTree, Java8InferenceContext context) {
        this(context.typeFactory.getAnnotatedType(varTree), TreeUtils.typeOf(varTree), context);
    }

    @Override
    public Kind getKind() {
        return Kind.PROPER;
    }

    @Override
    public AbstractType create(AnnotatedTypeMirror atm, TypeMirror type) {
        return new ProperType(atm, type, context);
    }

    public ProperType boxType() {
        if (properType.getKind().isPrimitive()) {
            return new ProperType(
                    typeFactory.getBoxedType((AnnotatedPrimitiveType) getAnnotatedType()),
                    context.types.boxedClass((Type) properType).asType(),
                    context);
        }
        return this;
    }

    public boolean isSubType(ProperType superType) {
        TypeMirror subType = getJavaType();
        TypeMirror superJavaType = superType.getJavaType();

        if (context.types.isSubtype((Type) subType, (Type) superJavaType)) {
            AnnotatedTypeMirror superATM = superType.getAnnotatedType();
            AnnotatedTypeMirror subATM = this.getAnnotatedType();
            return typeFactory.getTypeHierarchy().isSubtype(subATM, superATM);
        } else {
            return false;
        }
    }

    public boolean isSubTypeUnchecked(ProperType superType) {
        TypeMirror subType = getJavaType();
        TypeMirror superJavaType = superType.getJavaType();

        if (context.types.isSubtypeUnchecked((Type) subType, (Type) superJavaType)) {
            AnnotatedTypeMirror superATM = superType.getAnnotatedType();
            AnnotatedTypeMirror subATM = this.getAnnotatedType();
            return typeFactory.getTypeHierarchy().isSubtype(subATM, superATM);
        } else {
            return false;
        }
    }

    public boolean isAssignable(ProperType superType) {
        TypeMirror subType = getJavaType();
        TypeMirror superJavaType = superType.getJavaType();

        if (context.types.isAssignable((Type) subType, (Type) superJavaType)) {
            AnnotatedTypeMirror superATM = superType.getAnnotatedType();
            AnnotatedTypeMirror subATM = this.getAnnotatedType();
            return typeFactory.getTypeHierarchy().isSubtype(subATM, superATM);
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProperType otherProperType = (ProperType) o;

        if (!type.equals(otherProperType.type)) {
            return false;
        }

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
    public AnnotatedTypeMirror getAnnotatedType() {
        return type;
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
    public AbstractType applyInstantiations(List<Variable> instantiations) {
        return this;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
