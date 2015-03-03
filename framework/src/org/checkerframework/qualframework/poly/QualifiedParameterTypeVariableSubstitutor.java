package org.checkerframework.qualframework.poly;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedWildcardType;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.TypeVariableSubstitutor;

import javax.lang.model.type.TypeKind;
import java.util.HashMap;

/**
 * TypeVariableSubstitutor that handles the qualifier parameter specific logic
 * or combining TypeVariable uses with substituted types.
 */
public abstract class QualifiedParameterTypeVariableSubstitutor<Q> extends TypeVariableSubstitutor<QualParams<Q>> {

    /** Combine two wildcards into one when substituting a qualified type into
     * a qualified type variable use (for example, substituting {@code
     * [T := C《Q=TAINTED》]} into the use {@code T + 《Q=UNTAINTED》}).
     */
    protected abstract Wildcard<Q> combineForSubstitution(Wildcard<Q> a, Wildcard<Q> b);
    protected abstract PolyQual<Q> combineForSubstitution(PolyQual<Q> a, PolyQual<Q> b);

    @Override
    protected QualifiedTypeMirror<QualParams<Q>> substituteTypeVariable(
            final QualifiedTypeMirror<QualParams<Q>> argument,
            final QualifiedTypeVariable<QualParams<Q>> use) {

        if (argument.getKind() == TypeKind.WILDCARD) {
            // Ideally we would never get a wildcard type as `argument`, but
            // sometimes it happens due to checker framework misbehavior.
            // There are no top-level qualifiers on a wildcard type, so instead
            // we apply the combining to both the upper and lower bounds of the
            // wildcard.
            QualifiedWildcardType<QualParams<Q>> wild = (QualifiedWildcardType<QualParams<Q>>)argument;
            QualifiedTypeMirror<QualParams<Q>> extendsBound = wild.getExtendsBound();
            QualifiedTypeMirror<QualParams<Q>> superBound = wild.getSuperBound();

            if (extendsBound != null) {
                extendsBound = substituteTypeVariable(extendsBound, use);
            }

            if (superBound != null) {
                superBound = substituteTypeVariable(superBound, use);
            }

            return new QualifiedWildcardType<>(wild.getUnderlyingType(), extendsBound, superBound);
        }

        // If the underlying type is not primary qualified
        // then we should not use the type variables primary qualifier.
        if (!use.isPrimaryQualifierValid()) {
            return argument;
        }

        QualParams<Q> useParams = use.getQualifier();
        QualParams<Q> argumentParams = argument.getQualifier();

        HashMap<String, Wildcard<Q>> newParams = new HashMap<>(useParams);
        for (String name : argumentParams.keySet()) {
            Wildcard<Q> newValue = argumentParams.get(name);

            Wildcard<Q> oldValue = newParams.get(name);
            if (oldValue != null) {
                newValue = combineForSubstitution(oldValue, newValue);
            }

            newParams.put(name, newValue);
        }

        PolyQual<Q> primary;
        if (useParams.getPrimary() != null && argumentParams.getPrimary() != null) {
            primary = combineForSubstitution(useParams.getPrimary(), argumentParams.getPrimary());
        } else {
            throw new RuntimeException("Expected both QualParams to have a primary qualifier");
        }
        return SetQualifierVisitor.apply(argument, new QualParams<>(newParams, primary));

    }
}
