package tests.signatureinference;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
/**
 * Checker for a simple type system to test signature inference
 * using .jaif files.
 *
 * @author pbsf
 */
public class SignatureInferenceTestChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new SignatureInferenceTestVisitor(this);
    }
}
