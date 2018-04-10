package testlib.wholeprograminference;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** Checker for a simple type system to test whole-program inference using .jaif files. */
public class WholeProgramInferenceTestChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new WholeProgramInferenceTestVisitor(this);
    }
}
