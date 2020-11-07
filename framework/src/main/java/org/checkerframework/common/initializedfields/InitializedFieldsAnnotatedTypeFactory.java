package org.checkerframework.common.initializedfields;

import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.initializedfields.qual.InitializedFields;
import org.checkerframework.common.initializedfields.qual.InitializedFieldsBottom;
import org.checkerframework.common.initializedfields.qual.InitializedFieldsPredicate;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;

/** The annotated type factory for the Initialized Fields Checker. */
public class InitializedFieldsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {
    /**
     * Create a new accumulation checker's annotated type factory.
     *
     * @param checker the checker
     */
    public InitializedFieldsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(
                checker,
                InitializedFields.class,
                InitializedFieldsBottom.class,
                InitializedFieldsPredicate.class);
        this.postInit();
    }

    @Override
    protected void postAnalyze(ControlFlowGraph cfg) {
        System.out.printf("cfg=%s%n", cfg);
    }
}
