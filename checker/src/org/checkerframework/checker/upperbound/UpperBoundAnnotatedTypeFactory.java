package org.checkerframework.checker.upperbound;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import org.checkerframework.checker.upperbound.qual.*;

import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

public class UpperBoundAnnotatedTypeFactory extends
    GenericAnnotatedTypeFactory<CFValue, CFStore, UpperBoundTransfer, UpperBoundAnalysis> {

    public static AnnotationMirror LTL, LTEL, EL, UNKNOWN;
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    protected static ProcessingEnvironment env;
    
    public UpperBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        LTL = AnnotationUtils.fromClass(elements, LessThanLength.class);
        LTEL = AnnotationUtils.fromClass(elements, LessThanOrEqualToLength.class);
        EL = AnnotationUtils.fromClass(elements, EqualToLength.class);
        UNKNOWN = AnnotationUtils.fromClass(elements, UpperBoundUnknown.class);
        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        env = checker.getProcessingEnvironment();
        this.postInit();
    }

    @Override
    protected UpperBoundAnalysis createFlowAnalysis(
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new UpperBoundAnalysis(checker, this, fieldValues);
    }

}
