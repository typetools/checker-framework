package org.checkerframework.framework.base;

import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

public class CheckerAdapter<Q> extends BaseTypeChecker {
    private Checker<Q> underlying;
    private TypeMirrorConverter<Q> typeMirrorConverter;
    private QualifiedTypeFactoryAdapter<Q> typeFactory;

    public CheckerAdapter(Checker<Q> underlying) {
        this.underlying = underlying;
    }

    public TypeMirrorConverter<Q> getTypeMirrorConverter() {
        if (this.typeMirrorConverter == null) {
            this.typeMirrorConverter =
                new TypeMirrorConverter<Q>(getProcessingEnvironment(), this);
        }
        return this.typeMirrorConverter;
    }


    public QualifiedTypeFactoryAdapter<Q> getTypeFactory() {
        if (typeFactory == null) {
            typeFactory = createTypeFactory();
        }
        return typeFactory;
    }

    private QualifiedTypeFactoryAdapter<Q> createTypeFactory() {
        QualifiedTypeFactory<Q> underlyingFactory = underlying.getTypeFactory();
        QualifiedTypeFactoryAdapter<Q> factoryAdapter = new QualifiedTypeFactoryAdapter<Q>(
                underlyingFactory,
                getTypeMirrorConverter(),
                this);

        if (underlyingFactory instanceof DefaultQualifiedTypeFactory) {
            @SuppressWarnings("unchecked")
            DefaultQualifiedTypeFactory<Q> defaultFactory =
                (DefaultQualifiedTypeFactory<Q>)underlyingFactory;
            defaultFactory.setAdapter(factoryAdapter);
        }

        return factoryAdapter;
    }


    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new BaseTypeVisitor<QualifiedTypeFactoryAdapter<Q>>(this) {
            @Override
            protected QualifiedTypeFactoryAdapter<Q> createTypeFactory() {
                return CheckerAdapter.this.getTypeFactory();
            }
        };
    }
}
