package org.checkerframework.qualframework.base;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.checkerframework.qualframework.util.QualifierContext;

/** Main entry point for a pluggable type system.  Each type system must
 * provide an implementation of this abstract class that produces an
 * appropriate {@link QualifiedTypeFactory} for the type system.
 */
public abstract class Checker<Q> implements QualifierContext<Q> {
    private QualifiedTypeFactory<Q> typeFactory;
    private CheckerAdapter<Q> adapter;

    void setAdapter(CheckerAdapter<Q> adapter) {
        this.adapter = adapter;
    }

    public QualifierContext<Q> getContext() {
        return this;
    }

    @Override
    public ProcessingEnvironment getProcessingEnvironment() {
        return getCheckerAdapter().getContext().getProcessingEnvironment();
    }

    @Override
    public Elements getElementUtils() {
        return getCheckerAdapter().getContext().getElementUtils();
    }

    @Override
    public Types getTypeUtils() {
        return getCheckerAdapter().getContext().getTypeUtils();
    }

    @Override
    public Checker<Q> getChecker() {
        return this;
    }

    @Override
    public CheckerAdapter<Q> getCheckerAdapter() {
        return adapter;
    }

    @Override
    public QualifiedTypes<Q> getQualifiedTypeUtils() {
        return getTypeFactory().getQualifiedTypes();
    }

    /**
     * Constructs the {@link QualifiedTypeFactory} for use by this {@link
     * Checker}. 
     */
    protected abstract QualifiedTypeFactory<Q> createTypeFactory();

    /**
     * Returns the {@link QualifiedTypeFactory} used by this {@link Checker}.
     */
    @Override
    public QualifiedTypeFactory<Q> getTypeFactory() {
        if (this.typeFactory == null) {
            this.typeFactory = createTypeFactory();
        }
        return this.typeFactory;
    }

    // TODO: support for checker-defined visitor
}
