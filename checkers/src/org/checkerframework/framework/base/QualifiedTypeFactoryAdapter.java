package org.checkerframework.framework.base;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.util.MultiGraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

class QualifiedTypeFactoryAdapter<Q> extends BaseAnnotatedTypeFactory {
    private QualifiedTypeFactory<Q> underlying;
    private TypeMirrorConverter<Q> converter;

    public QualifiedTypeFactoryAdapter(QualifiedTypeFactory<Q> underlying,
            TypeMirrorConverter<Q> converter,
            CheckerAdapter<Q> checker) {
        super(checker, true);
        this.underlying = underlying;
        this.converter = converter;

        this.postInit();
    }

    QualifiedTypeFactory<Q> getUnderlying() {
        return underlying;
    }

    @SuppressWarnings("unchecked")
    private CheckerAdapter<Q> getCheckerAdapter() {
        return (CheckerAdapter<Q>)checker;
    }

    @SuppressWarnings("unchecked")
    public QualifierHierarchyAdapter<Q>.Implementation getQualifierHierarchyAdapter() {
        return (QualifierHierarchyAdapter<Q>.Implementation)getQualifierHierarchy();
    }

    @SuppressWarnings("unchecked")
    public TypeHierarchyAdapter<Q> getTypeHierarchyAdapter() {
        return (TypeHierarchyAdapter<Q>)getTypeHierarchy();
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public MultiGraphQualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        QualifierHierarchy<Q> underlyingHierarchy = underlying.getQualifierHierarchy();

        QualifierHierarchyAdapter<Q>.Implementation adapter =
            new QualifierHierarchyAdapter<Q>(
                underlyingHierarchy,
                getCheckerAdapter().getTypeMirrorConverter())
            .createImplementation(factory);
        return adapter;
    }

    @Override
    protected checkers.types.TypeHierarchy createTypeHierarchy() {
        TypeHierarchy<Q> underlyingHierarchy = underlying.getTypeHierarchy();
        TypeHierarchyAdapter<Q> adapter = new TypeHierarchyAdapter<Q>(
                underlyingHierarchy,
                getCheckerAdapter().getTypeMirrorConverter(),
                getCheckerAdapter(),
                getQualifierHierarchyAdapter());

        if (underlyingHierarchy instanceof DefaultTypeHierarchy) {
            @SuppressWarnings("unchecked")
            DefaultTypeHierarchy<Q> defaultHierarchy =
                (DefaultTypeHierarchy<Q>)underlyingHierarchy;
            defaultHierarchy.setAdapter(adapter);
        }

        return adapter;
    }

    @Override
    protected checkers.types.TreeAnnotator createTreeAnnotator() {
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        DefaultQualifiedTypeFactory<Q> defaultUnderlying =
            (DefaultQualifiedTypeFactory<Q>)underlying;
        TreeAnnotator<Q> underlyingAnnotator = defaultUnderlying.getTreeAnnotator();
        TreeAnnotatorAdapter<Q> adapter = new TreeAnnotatorAdapter<Q>(
                underlyingAnnotator,
                getCheckerAdapter().getTypeMirrorConverter(),
                this);

        return adapter;
    }

    @Override
    protected checkers.types.TypeAnnotator createTypeAnnotator() {
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        DefaultQualifiedTypeFactory<Q> defaultUnderlying =
            (DefaultQualifiedTypeFactory<Q>)underlying;
        TypeAnnotator<Q> underlyingAnnotator = defaultUnderlying.getTypeAnnotator();
        TypeAnnotatorAdapter<Q> adapter = new TypeAnnotatorAdapter<Q>(
                underlyingAnnotator,
                getCheckerAdapter().getTypeMirrorConverter(),
                this);

        underlyingAnnotator.setAdapter(adapter);

        return adapter;
    }

    @Override
    public boolean isSupportedQualifier(AnnotationMirror anno) {
        return true;
    }


    @Override
    public AnnotatedTypeMirror getAnnotatedType(Element elt) {
        return converter.getAnnotatedType(
                underlying.getQualifiedType(elt));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedType(Element elt) {
        return converter.getQualifiedType(
                super.getAnnotatedType(elt));
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        AnnotatedTypeMirror result = converter.getAnnotatedType(
                underlying.getQualifiedType(tree));
        return result;
    }

    QualifiedTypeMirror<Q> superGetAnnotatedType(Tree tree) {
        return converter.getQualifiedType(
                super.getAnnotatedType(tree));
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        return converter.getAnnotatedType(
                underlying.getQualifiedTypeFromTypeTree(tree));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedTypeFromTypeTree(Tree tree) {
        return converter.getQualifiedType(
                super.getAnnotatedTypeFromTypeTree(tree));
    }


    @Override
    public AnnotatedWildcardType getWildcardBoundedBy(AnnotatedTypeMirror upper) {
        AnnotatedWildcardType result = super.getWildcardBoundedBy(upper);
        typeAnnotator.scanAndReduce(result, null, null);
        return result;
    }

    @Override
    public AnnotatedWildcardType getUninferredWildcardType(AnnotatedTypeVariable var) {
        AnnotatedWildcardType result = super.getUninferredWildcardType(var);
        typeAnnotator.scanAndReduce(result, null, null);
        return result;
    }
}
