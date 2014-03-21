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

import org.checkerframework.framework.util.WrappedAnnotatedTypeMirror;

/**
 * Adapter class for {@link QualifiedTypeFactory}, extending
 * {@link BaseAnnotatedTypeFactory BaseAnnotatedTypeFactory}.
 */
class QualifiedTypeFactoryAdapter<Q> extends BaseAnnotatedTypeFactory {
    /** The underlying {@link QualifiedTypeFactory}. */
    private QualifiedTypeFactory<Q> underlying;

    public QualifiedTypeFactoryAdapter(QualifiedTypeFactory<Q> underlying,
            CheckerAdapter<Q> checker) {
        super(checker, true);
        this.underlying = underlying;

        this.postInit();
    }

    /** Returns the underlying {@link QualifiedTypeFactory}. */
    public QualifiedTypeFactory<Q> getUnderlying() {
        return underlying;
    }

    /** Returns {@link checker}, downcast to a more precise type. */
    @SuppressWarnings("unchecked")
    private CheckerAdapter<Q> getCheckerAdapter() {
        return (CheckerAdapter<Q>)checker;
    }

    /** Returns the same result as {@link getQualifierHierarchy}, but downcast
     * to a more precise type. */
    @SuppressWarnings("unchecked")
    private QualifierHierarchyAdapter<Q>.Implementation getQualifierHierarchyAdapter() {
        return (QualifierHierarchyAdapter<Q>.Implementation)getQualifierHierarchy();
    }

    /** Returns the same result as {@link getTypeHierarchy}, but downcast to a
     * more precise type. */
    @SuppressWarnings("unchecked")
    private TypeHierarchyAdapter<Q> getTypeHierarchyAdapter() {
        return (TypeHierarchyAdapter<Q>)getTypeHierarchy();
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public MultiGraphQualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        QualifierHierarchy<Q> underlyingHierarchy = underlying.getQualifierHierarchy();

        // See QualifierHierarchyAdapter for an explanation of why we need this
        // strange pattern instead of just making a single call to the
        // QualifierHierarchyAdapter constructor.
        QualifierHierarchyAdapter<Q>.Implementation adapter =
            new QualifierHierarchyAdapter<Q>(
                underlyingHierarchy,
                getCheckerAdapter().getTypeMirrorConverter())
            .createImplementation(factory);
        return adapter;
    }

    /* Constructs a TypeHierarchyAdapter for the underlying factory's
     * TypeHierarchy.
     */
    @Override
    protected checkers.types.TypeHierarchy createTypeHierarchy() {
        TypeHierarchy<Q> underlyingHierarchy = underlying.getTypeHierarchy();
        TypeHierarchyAdapter<Q> adapter = new TypeHierarchyAdapter<Q>(
                underlyingHierarchy,
                getCheckerAdapter().getTypeMirrorConverter(),
                getCheckerAdapter(),
                getQualifierHierarchyAdapter());

        // TODO: Move this check (and others like it) into the adapter
        // constructor.
        if (underlyingHierarchy instanceof DefaultTypeHierarchy) {
            DefaultTypeHierarchy<Q> defaultHierarchy =
                (DefaultTypeHierarchy<Q>)underlyingHierarchy;
            defaultHierarchy.setAdapter(adapter);
        }

        return adapter;
    }

    /* Constructs a TreeAnnotatorAdapter for the underlying factory's
     * TreeAnnotator.
     */
    @Override
    protected checkers.types.TreeAnnotator createTreeAnnotator() {
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            // In theory, the result of this branch should never be used.  Only
            // DefaultQTFs have a way to access the annotation-based logic
            // which requires the TreeAnnotator produced by this method.
            return null;
        }

        DefaultQualifiedTypeFactory<Q> defaultUnderlying =
            (DefaultQualifiedTypeFactory<Q>)underlying;
        TreeAnnotator<Q> underlyingAnnotator = defaultUnderlying.getTreeAnnotator();
        TreeAnnotatorAdapter<Q> adapter = new TreeAnnotatorAdapter<Q>(
                underlyingAnnotator,
                getCheckerAdapter().getTypeMirrorConverter(),
                this);

        return adapter;
    }

    /* Constructs a TypeAnnotatorAdapter for the underlying factory's
     * TypeAnnotator.
     */
    @Override
    protected checkers.types.TypeAnnotator createTypeAnnotator() {
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            // In theory, the result of this branch should never be used.  Only
            // DefaultQTFs have a way to access the annotation-based logic
            // which requires the TypeAnnotator produced by this method.
            return null;
        }

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
        if (anno == null) {
            return false;
        }

        // If 'underlying' is not a DefaultQTF, there is no AnnotationConverter
        // for us to use for this check.
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            return true;
        }

        DefaultQualifiedTypeFactory<Q> defaultUnderlying = (DefaultQualifiedTypeFactory<Q>)underlying;
        AnnotationConverter<Q> annoConverter = defaultUnderlying.getAnnotationConverter();

        return annoConverter.isAnnotationSupported(anno)
            || getCheckerAdapter().getTypeMirrorConverter().isKey(anno);
    }


    @Override
    public AnnotatedTypeMirror getAnnotatedType(Element elt) {
        return getCheckerAdapter().getTypeMirrorConverter().getAnnotatedType(
                underlying.getQualifiedType(elt));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedType(Element elt) {
        AnnotatedTypeMirror atm = super.getAnnotatedType(elt);
        typeAnnotator.visit(atm, null);
        return getCheckerAdapter().getTypeMirrorConverter().getQualifiedType(atm);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        return getCheckerAdapter().getTypeMirrorConverter().getAnnotatedType(
                underlying.getQualifiedType(tree));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedType(Tree tree) {
        AnnotatedTypeMirror atm = super.getAnnotatedType(tree);
        typeAnnotator.visit(atm, null);
        return getCheckerAdapter().getTypeMirrorConverter().getQualifiedType(atm);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        return getCheckerAdapter().getTypeMirrorConverter().getAnnotatedType(
                underlying.getQualifiedTypeFromTypeTree(tree));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedTypeFromTypeTree(Tree tree) {
        AnnotatedTypeMirror atm = super.getAnnotatedTypeFromTypeTree(tree);
        typeAnnotator.visit(atm, null);
        return getCheckerAdapter().getTypeMirrorConverter().getQualifiedType(atm);
    }


    @Override
    public AnnotatedWildcardType getWildcardBoundedBy(AnnotatedTypeMirror upper) {
        // The superclass implementation of this method doesn't run the
        // TypeAnnotator, which means annotations won't get converted to
        // qualifier @Keys.  This causes problems later on, so we run the
        // TypeAnnotator manually here.
        AnnotatedWildcardType result = super.getWildcardBoundedBy(upper);
        typeAnnotator.scanAndReduce(result, null, null);
        return result;
    }

    @Override
    public AnnotatedWildcardType getUninferredWildcardType(AnnotatedTypeVariable var) {
        // Same logic as getWildcardBoundedBy.
        AnnotatedWildcardType result = super.getUninferredWildcardType(var);
        typeAnnotator.scanAndReduce(result, null, null);
        return result;
    }
}
