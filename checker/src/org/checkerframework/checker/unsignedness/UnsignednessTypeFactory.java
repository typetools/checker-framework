package org.checkerframework.checker.unsignedness;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.javacutil.Pair;

public class UnsignednessTypeFactory extends 
                GenericAnnotatedTypeFactory<CFValue, CFStore, 
                    UnsignednessTransfer, UnsignednessAnalysis> {

    public UnsignednessTypeFactory( BaseTypeChecker checker ) {
        super( checker );
    }

    public UnsignednessTypeFactory(BaseTypeChecker checker, 
        boolean useFlow) {

        super(checker, useFlow);
    }

    @Override
    protected UnsignednessAnalysis createFlowAnalysis(
                List<Pair<VariableElement, CFValue>> fieldValues ) {
        
        return new UnsignednessAnalysis( checker, this, fieldValues );
    }
}