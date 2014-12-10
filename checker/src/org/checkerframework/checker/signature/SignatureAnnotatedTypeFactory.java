package org.checkerframework.checker.signature;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;


// TODO: Does not yet handle method signature annotations, such as
// @MethodDescriptor.


/**
 * This class is currently not needed.
 * It is retained here to make future extension easier.
 */
public class SignatureAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public SignatureAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    /*
    @Override
    public TreeAnnotator createTreeAnnotator(SignatureChecker checker) {
        return new SignatureTreeAnnotator(checker);
    }

    private class SignatureTreeAnnotator extends TreeAnnotator {

        public SignatureTreeAnnotator(BaseTypeChecker checker) {
            super(checker, SignatureAnnotatedTypeFactory.this);
        }

    }
    */
}
