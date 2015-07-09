package tests.fieldtypeinference;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
/**
 * Checker for a simple type system to test private field type inference.
 *
 * @author pbsf
 */
public class FieldTypeInferenceTestChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new FieldTypeInferenceTestVisitor(this);
    }

}
