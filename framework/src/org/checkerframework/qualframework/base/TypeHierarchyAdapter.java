package org.checkerframework.qualframework.base;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;

import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;

/** Adapter class for {@link TypeHierarchy}, extending
 * {@link org.checkerframework.framework.type.TypeHierarchy org.checkerframework.framework.type.TypeHierarchy}.
 */
class TypeHierarchyAdapter<Q> extends org.checkerframework.framework.type.TypeHierarchy {
    private TypeHierarchy<Q> underlying;
    /** A copy of {@link underlying} with a more precise type, or null if
     * {@link underlying} is not a {@link DefaultTypeHierarchy} instance.
     */
    private DefaultTypeHierarchy<Q> defaultUnderlying;
    private TypeMirrorConverter<Q> converter;

    public TypeHierarchyAdapter(TypeHierarchy<Q> underlying,
            TypeMirrorConverter<Q> converter,
            CheckerAdapter<Q> checker,
            QualifierHierarchyAdapter<Q>.Implementation qualifierHierarchy) {
        super(checker, qualifierHierarchy);
        this.underlying = underlying;

        if (underlying instanceof DefaultTypeHierarchy) {
            DefaultTypeHierarchy<Q> defaultUnderlying =
                (DefaultTypeHierarchy<Q>)underlying;
            this.defaultUnderlying = defaultUnderlying;
        } else {
            // It's fine to leave 'defaultUnderlying' null here, because only
            // 'DefaultTypeHierarchy' is able to invoke the annotation-based
            // code that uses 'defaultUnderlying'.  (Note that 'isSubtype', the
            // only public entry point, uses 'underlying', not
            // 'defaultUnderlying'.)
            this.defaultUnderlying = null;
        }

        this.converter = converter;
    }


    @Override
    public boolean isSubtype(AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
        return underlying.isSubtype(
                converter.getQualifiedType(a),
                converter.getQualifiedType(b));
    }

    boolean superIsSubtype(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtype(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    @Override
    protected boolean isSubtypeAsArrayComponent(AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
        return defaultUnderlying.isSubtypeAsArrayComponent(
                converter.getQualifiedType(a),
                converter.getQualifiedType(b));
    }

    boolean superIsSubtypeAsArrayComponent(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtypeAsArrayComponent(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    @Override
    protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
        return defaultUnderlying.isSubtypeAsTypeArgument(
                converter.getQualifiedType(a),
                converter.getQualifiedType(b));
    }

    boolean superIsSubtypeAsTypeArgument(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtypeAsTypeArgument(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    // No 'isSubtypeImpl', because that method is final.

    boolean superIsSubtypeImpl(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtypeImpl(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    @Override
    protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType a, AnnotatedDeclaredType b) {
        return defaultUnderlying.isSubtypeTypeArguments(
                (QualifiedDeclaredType<Q>)converter.getQualifiedType(a),
                (QualifiedDeclaredType<Q>)converter.getQualifiedType(b));
    }

    boolean superIsSubtypeTypeArguments(QualifiedDeclaredType<Q> a, QualifiedDeclaredType<Q> b) {
        return super.isSubtypeTypeArguments(
                (AnnotatedDeclaredType)converter.getAnnotatedType(a),
                (AnnotatedDeclaredType)converter.getAnnotatedType(b));
    }
}
