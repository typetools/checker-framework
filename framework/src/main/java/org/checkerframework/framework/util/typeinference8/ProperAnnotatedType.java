package org.checkerframework.framework.util.typeinference8;

import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.typeinference8.types.AbstractType;
import org.checkerframework.javacutil.typeinference8.types.ProperType;
import org.checkerframework.javacutil.typeinference8.types.Variable;

public class ProperAnnotatedType extends AbstractAnnotatedType implements ProperType {
    private final AnnotatedTypeMirror type;

    ProperAnnotatedType(AnnotatedTypeMirror type, CFInferenceContext context) {
        super(context);
        this.type = type;
    }

    @Override
    public ProperType create(AnnotatedTypeMirror type) {
        return new ProperAnnotatedType(type, context);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType() {
        return type;
    }

    @Override
    public ProperType boxType() {
        if (this.getTypeKind().isPrimitive()) {
            return create(typeFactory.getBoxedType((AnnotatedPrimitiveType) getAnnotatedType()));
        } else {
            return this;
        }
    }

    @Override
    public boolean isSubType(ProperType superType) {
        TypeMirror subType = getJavaType();
        TypeMirror superJavaType = superType.getJavaType();

        if (context.types.isSubtypeUnchecked((Type) subType, (Type) superJavaType)) {
            AnnotatedTypeMirror superATM = ((ProperAnnotatedType) superType).getAnnotatedType();
            AnnotatedTypeMirror subATM = this.getAnnotatedType();
            return typeFactory.getTypeHierarchy().isSubtype(subATM, superATM);
        } else {
            return false;
        }
    }

    @Override
    public TypeMirror getJavaType() {
        return type.getUnderlyingType();
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
    public boolean isObject() {
        return TypesUtils.isObject(getJavaType());
    }
}
