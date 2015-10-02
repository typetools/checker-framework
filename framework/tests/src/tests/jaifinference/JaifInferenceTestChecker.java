package tests.jaifinference;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
/**
 * Checker for a simple type system to test a whole-program type inference
 * using .jaif files.
 *
 * @author pbsf
 */
public class JaifInferenceTestChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new JaifInferenceTestVisitor(this);
    }
}
