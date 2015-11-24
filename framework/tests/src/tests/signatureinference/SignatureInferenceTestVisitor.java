package tests.signatureinference;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
/**
 * Visitor for a simple type system to test signature inference
 * using .jaif files.
 *
 * @author pbsf
 */
public class SignatureInferenceTestVisitor extends BaseTypeVisitor<SignatureInferenceTestAnnotatedTypeFactory> {

    public SignatureInferenceTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected SignatureInferenceTestAnnotatedTypeFactory createTypeFactory() {
        return new SignatureInferenceTestAnnotatedTypeFactory(checker);
    }
}
