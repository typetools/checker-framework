package org.checkerframework.qualframework.base;

import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;

/**
 * Adapt substitution to the Qual Framework
 */
public class TypeVariableSubstitutorAdapter<Q> extends org.checkerframework.framework.type.TypeVariableSubstitutor {

    private final TypeVariableSubstitutor<Q> underlying;
    private final TypeMirrorConverter<Q> converter;

    public TypeVariableSubstitutorAdapter(TypeVariableSubstitutor<Q> underlying, TypeMirrorConverter<Q> converter) {
        this.underlying = underlying;
        this.converter = converter;
    }

    protected AnnotatedTypeMirror substituteTypeVariable(final AnnotatedTypeMirror argument,
            final AnnotatedTypeVariable use) {

        QualifiedTypeMirror<Q> qArgument = converter.getQualifiedType(argument);
        QualifiedTypeVariable<Q> qUse = (QualifiedTypeVariable<Q>) converter.getQualifiedType(use.asUse());
        return converter.getAnnotatedType(underlying.substituteTypeVariable(qArgument, qUse));
    }

    protected QualifiedTypeMirror<Q> superSubstituteTypeVariable(final QualifiedTypeMirror<Q> argument,
            final QualifiedTypeVariable<Q> use) {

        return converter.getQualifiedType(super.substituteTypeVariable(converter.getAnnotatedType(argument),
                (AnnotatedTypeVariable)converter.getAnnotatedType(use)));
    }
}
