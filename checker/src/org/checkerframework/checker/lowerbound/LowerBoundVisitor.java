package org.checkerframework.checker.lowerbound;

import org.checkerframework.checker.lowerbound.qual.*;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

public class LowerBoundVisitor extends BaseTypeVisitor<LowerBoundAnnotatedTypeFactory> {

    public LowerBoundVisitor(BaseTypeChecker checker){
	super(checker);
    }

    //FIXME: kelloggm: need to actually enforce da rulez here.
    
}
