package org.checkerframework.qualframework.base;

import java.util.*;

import com.sun.source.tree.ExpressionTree;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;

import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;

/** {@link QualifiedTypes} implementation that wraps {@link AnnotatedTypes}.
 */
class AdapterQualifiedTypes<Q> implements QualifiedTypes<Q> {
    private QualifiedTypeFactoryAdapter<Q> typeFactory;
    private TypeMirrorConverter<Q> converter;

    public AdapterQualifiedTypes(QualifiedTypeFactoryAdapter<Q> typeFactory) {
        this.typeFactory = typeFactory;
        this.converter = typeFactory.getCheckerAdapter().getTypeMirrorConverter();
    }

    @Override
    public List<QualifiedTypeMirror<Q>> expandVarArgs(
            QualifiedExecutableType<Q> method,
            List<? extends ExpressionTree> args) {
        AnnotatedExecutableType annoMethod = (AnnotatedExecutableType)converter.getAnnotatedType(method);
        List<AnnotatedTypeMirror> annoResult = AnnotatedTypes.expandVarArgs(
                typeFactory, annoMethod, args);
        return converter.getQualifiedTypeList(annoResult);
    }
}
