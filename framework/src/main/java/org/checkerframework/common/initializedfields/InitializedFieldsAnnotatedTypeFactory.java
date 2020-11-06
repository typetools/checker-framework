package org.checkerframework.common.initializedfields;

import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.initializedfields.qual.InitializedFields;
import org.checkerframework.common.initializedfields.qual.InitializedFieldsBottom;
import org.checkerframework.common.initializedfields.qual.InitializedFieldsPredicate;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

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
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(), new InitializedFieldsTreeAnnotator(this));
    }

    /**
     * Necessary for the type rule for called methods described below. A new accumulation analysis
     * might have other type rules here, or none at all.
     */
    private class InitializedFieldsTreeAnnotator extends AccumulationTreeAnnotator {
        /**
         * Creates an instance of this tree annotator for the given type factory.
         *
         * @param factory the type factory
         */
        public InitializedFieldsTreeAnnotator(AccumulationAnnotatedTypeFactory factory) {
            super(factory);
        }
    }
}
