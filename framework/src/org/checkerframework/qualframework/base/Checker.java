package org.checkerframework.qualframework.base;

import org.checkerframework.framework.util.OptionConfiguration;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.qualframework.base.format.DefaultQualFormatter;
import org.checkerframework.qualframework.base.format.DefaultQualifiedTypeFormatter;
import org.checkerframework.qualframework.base.format.QualifiedTypeFormatter;
import org.checkerframework.qualframework.util.QualifierContext;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.sun.source.util.Trees;

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

    @Override
    public AnnotationProvider getAnnotationProvider() {
        return getCheckerAdapter().getTypeFactory();
    }

    @Override
    public Trees getTreeUtils() {
        return getCheckerAdapter().getTreeUtils();
    }

    @Override
    public OptionConfiguration getOptionConfiguration() {
        return getCheckerAdapter();
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

    /**
     * Create a QualifiedTypeFormatter to format QualifiedTypeMirrors into strings.
     *
     * @return the QualifiedTypeFormatter
     */
    public QualifiedTypeFormatter<Q> createQualifiedTypeFormatter() {
        return new DefaultQualifiedTypeFormatter<>(
                new DefaultQualFormatter<Q>(getInvisibleQualifiers()),
                getContext().getCheckerAdapter().getTypeMirrorConverter(),
                getContext().getOptionConfiguration().hasOption("printVerboseGenerics"),
                getContext().getOptionConfiguration().hasOption("printAllQualifiers")
        );
    }

    /**
     * Return a list of qualifiers that should not be printed unless printAllQualifiers is enabled.
     */
    protected Set<?> getInvisibleQualifiers() {
        return new HashSet<>();
    }


    // TODO: support for checker-defined visitor
}
