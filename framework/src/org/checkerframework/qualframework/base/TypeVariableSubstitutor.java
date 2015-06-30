package org.checkerframework.qualframework.base;

import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;

import java.util.Map;

import javax.lang.model.type.TypeVariable;

/**
 * TypeVariableSubstitutor replaces type variables from a declaration with arguments to its use.
 */
public class TypeVariableSubstitutor<Q> {

    private TypeVariableSubstitutorAdapter<Q> adapter;

    /**
     * Currently substitution is performed by AnnotatedTypes class. There are no hooks in the qual system
     * to make that functionality flow through this method. When that is plumbed, this method should be implemented.
     */
    public QualifiedTypeMirror<Q> substitute(final Map<TypeVariable, QualifiedTypeMirror<Q>> typeParamToArg,
            final QualifiedTypeMirror<Q> typeMirror) {

        throw new UnsupportedOperationException("Calling this method directly is not " +
                "yet supported by the qualifier parameter framework.");
    }

    // @see doesn't work because the method has protected visibility
    /**
     * see org.checkerframework.framework.type.TypeVariableSubstitutor#substituteTypeVariable(AnnotatedTypeMirror, AnnotatedTypeVariable)
     */
    protected QualifiedTypeMirror<Q> substituteTypeVariable(final QualifiedTypeMirror<Q> argument,
            final QualifiedTypeVariable<Q> use) {

        return adapter.superSubstituteTypeVariable(argument, use);
    }

    public void setAdapter(TypeVariableSubstitutorAdapter<Q> adapter) {
        this.adapter = adapter;
    }
}
