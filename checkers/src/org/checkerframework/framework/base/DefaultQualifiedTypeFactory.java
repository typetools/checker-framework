package org.checkerframework.framework.base;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

import javacutils.Pair;

import org.checkerframework.framework.base.QualifiedTypeMirror;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedTypeVariable;

public abstract class DefaultQualifiedTypeFactory<Q> implements QualifiedTypeFactory<Q> {

    private QualifierHierarchy<Q> qualifierHierarchy;
    private TypeHierarchy<Q> typeHierarchy;
    private AnnotationConverter<Q> annotationConverter;

    private TypeAnnotator<Q> typeAnnotator;

    private QualifiedTypeFactoryAdapter<Q> adapter;

    void setAdapter(QualifiedTypeFactoryAdapter<Q> adapter) {
        this.adapter = adapter;
    }


    @Override
    public final QualifiedTypeMirror<Q> getQualifiedType(Element element) {
        return adapter.superGetAnnotatedType(element);
    }

    @Override
    public final QualifiedTypeMirror<Q> getQualifiedType(Tree tree) {
        return adapter.superGetAnnotatedType(tree);
    }

    @Override
    public final QualifiedTypeMirror<Q> getQualifiedTypeFromTypeTree(Tree typeTree) {
        return adapter.superGetAnnotatedTypeFromTypeTree(typeTree);
    }


    /*
    protected TreeAnnotator<Q> createTreeAnnotator() {
        return new TreeAnnotator<Q>();
    }
    */

    TypeAnnotator<Q> getTypeAnnotator() {
        if (this.typeAnnotator == null) {
            this.typeAnnotator = createTypeAnnotator();
        }
        return this.typeAnnotator;
    }

    protected TypeAnnotator<Q> createTypeAnnotator() {
        return new TypeAnnotator<Q>(getAnnotationConverter(),
                getQualifierHierarchy().getTop(), getQualifierHierarchy().getBottom());
    }


    @Override
    public final QualifierHierarchy<Q> getQualifierHierarchy() {
        if (this.qualifierHierarchy == null) {
            this.qualifierHierarchy = createQualifierHierarchy();
        }
        return this.qualifierHierarchy;
    }

    protected abstract QualifierHierarchy<Q> createQualifierHierarchy();


    @Override
    public final TypeHierarchy<Q> getTypeHierarchy() {
        if (this.typeHierarchy == null) {
            this.typeHierarchy = createTypeHierarchy(getQualifierHierarchy());
        }
        return this.typeHierarchy;
    }

    protected TypeHierarchy<Q> createTypeHierarchy(QualifierHierarchy<Q> qualifierHierarchy) {
        return new DefaultTypeHierarchy<Q>(qualifierHierarchy);
    }


    public final AnnotationConverter<Q> getAnnotationConverter() {
        if (this.annotationConverter == null) {
            this.annotationConverter = createAnnotationConverter();
        }
        return this.annotationConverter;
    }

    protected abstract AnnotationConverter<Q> createAnnotationConverter();


    @Override
    public List<QualifiedTypeMirror<Q>> postDirectSuperTypes(QualifiedTypeMirror<Q> subtype, List<? extends QualifiedTypeMirror<Q>> supertypes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QualifiedTypeMirror<Q> postAsMemberOf(QualifiedTypeMirror<Q> memberType, QualifiedTypeMirror<Q> receiverType, Element memberElement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<QualifiedTypeVariable<Q>> typeVariablesFromUse(QualifiedDeclaredType<Q> type, TypeElement element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> methodFromUse(MethodInvocationTree tree) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> constructorFromUse(NewClassTree tree) {
        throw new UnsupportedOperationException();
    }
}
