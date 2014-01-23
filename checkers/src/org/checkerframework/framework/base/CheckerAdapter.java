package org.checkerframework.framework.base;

import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

public class CheckerAdapter<Q> extends BaseTypeChecker {
    private Checker<Q> underlying;
    private TypeMirrorConverter<Q> typeMirrorConverter;

    public CheckerAdapter(Checker<Q> underlying) {
        this.underlying = underlying;
    }

    public TypeMirrorConverter<Q> getTypeMirrorConverter() {
        if (this.typeMirrorConverter == null) {
            this.typeMirrorConverter = new TypeMirrorConverter<Q>(getProcessingEnvironment());
        }
        return this.typeMirrorConverter;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new BaseTypeVisitor<QualifiedTypeFactoryAdapter<Q>>(this) {
            @Override
            protected QualifiedTypeFactoryAdapter<Q> createTypeFactory() {
                CheckerAdapter<Q> thisChecker = CheckerAdapter.this;
                QualifiedTypeFactory<Q> underlyingFactory = thisChecker.underlying.getTypeFactory();
                QualifiedTypeFactoryAdapter<Q> factoryAdapter = new QualifiedTypeFactoryAdapter<Q>(
                        underlyingFactory,
                        thisChecker.getTypeMirrorConverter(),
                        thisChecker);

                if (underlyingFactory instanceof DefaultQualifiedTypeFactory) {
                    @SuppressWarnings("unchecked")
                    DefaultQualifiedTypeFactory<Q> defaultFactory =
                        (DefaultQualifiedTypeFactory<Q>)underlyingFactory;
                    defaultFactory.setAdapter(factoryAdapter);
                }

                return factoryAdapter;
            }

            @Override
            public boolean isValidUse(AnnotatedDeclaredType declarationType,
                    AnnotatedDeclaredType useType, Tree tree) {
                return true;
            }
        };
    }
}
