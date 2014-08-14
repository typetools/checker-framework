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
    public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
        return underlying.isSubtype(
                converter.getQualifiedType(subtype),
                converter.getQualifiedType(supertype));
    }

    boolean superIsSubtype(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return super.isSubtype(
                converter.getAnnotatedType(subtype),
                converter.getAnnotatedType(supertype));
    }

    @Override
    protected boolean isSubtypeAsArrayComponent(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
        return defaultUnderlying.isSubtypeAsArrayComponent(
                converter.getQualifiedType(subtype),
                converter.getQualifiedType(supertype));
    }

    boolean superIsSubtypeAsArrayComponent(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return super.isSubtypeAsArrayComponent(
                converter.getAnnotatedType(subtype),
                converter.getAnnotatedType(supertype));
    }

    @Override
    protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
        return defaultUnderlying.isSubtypeAsTypeArgument(
                converter.getQualifiedType(subtype),
                converter.getQualifiedType(supertype));
    }

    boolean superIsSubtypeAsTypeArgument(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return super.isSubtypeAsTypeArgument(
                converter.getAnnotatedType(subtype),
                converter.getAnnotatedType(supertype));
    }

    // No 'isSubtypeImpl', because that method is final.

    boolean superIsSubtypeImpl(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return super.isSubtypeImpl(
                converter.getAnnotatedType(subtype),
                converter.getAnnotatedType(supertype));
    }

    @Override
    protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype) {
        return defaultUnderlying.isSubtypeTypeArguments(
                converter.getQualifiedType(subtype),
                converter.getQualifiedType(supertype));
    }

    boolean superIsSubtypeTypeArguments(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return super.isSubtypeTypeArguments(
                (AnnotatedDeclaredType)converter.getAnnotatedType(subtype),
                (AnnotatedDeclaredType)converter.getAnnotatedType(supertype));
    }
}
