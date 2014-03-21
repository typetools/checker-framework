package org.checkerframework.framework.base;

import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

/** Adapter class for {@link Checker}, extending
 * {@link BaseTypeChecker checkers.basetype.BaseTypeChecker}.
 */
public class CheckerAdapter<Q> extends BaseTypeChecker {
    /** The underlying qualifier-based checker. */
    private Checker<Q> underlying;
    /** The {@link TypeMirrorConverter} used by this {@link CheckerAdapter} and
     * its components. */
    private TypeMirrorConverter<Q> typeMirrorConverter;
    /** The adapter for the underlying checker's {@link QualifiedTypeFactory}.
     */
    private QualifiedTypeFactoryAdapter<Q> typeFactory;

    /** Constructs a {@link CheckerAdapter} from an underlying qualifier-based
     * {@link Checker}. */
    public CheckerAdapter(Checker<Q> underlying) {
        this.underlying = underlying;
    }

    /** Gets the {@link TypeMirrorConverter} used by this {@link CheckerAdapter}
     * and its component adapters. */
    public TypeMirrorConverter<Q> getTypeMirrorConverter() {
        if (this.typeMirrorConverter == null) {
            this.typeMirrorConverter =
                new TypeMirrorConverter<Q>(getProcessingEnvironment(), this);
        }
        return this.typeMirrorConverter;
    }

    /**
     * Gets the {@link QualifiedTypeFactoryAdapter} for the underlying
     * checker's {@link QualifiedTypeFactory}.  This is used by the {@link
     * SourceVisitor} defined below to obtain the {@link
     * QualifiedTypeFactoryAdapter} using lazy initialization.
     */
    // This method has package access so it can be called from
    // TypeMirrorConverter.  It should be made private once the converter is no
    // longer needed.
    QualifiedTypeFactoryAdapter<Q> getTypeFactory() {
        // TODO: check if lazy init is actually necessary for typeFactory.
        if (typeFactory == null) {
            typeFactory = createTypeFactory();
        }
        return typeFactory;
    }

    /** Constructs a {@link QualifiedTypeFactoryAdapter} for the underlying
     * {@link QualifiedTypeFactory}. */
    private QualifiedTypeFactoryAdapter<Q> createTypeFactory() {
        QualifiedTypeFactory<Q> underlyingFactory = underlying.getTypeFactory();
        QualifiedTypeFactoryAdapter<Q> factoryAdapter = new QualifiedTypeFactoryAdapter<Q>(
                underlyingFactory,
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
