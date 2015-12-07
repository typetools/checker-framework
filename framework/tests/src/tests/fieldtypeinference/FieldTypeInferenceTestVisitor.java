package tests.fieldtypeinference;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
/**
 * Visitor for a simple type system to test private field type inference.
 *
 * @author pbsf
 */
public class FieldTypeInferenceTestVisitor extends BaseTypeVisitor<FieldTypeInferenceTestAnnotatedTypeFactory> {

    public FieldTypeInferenceTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected FieldTypeInferenceTestAnnotatedTypeFactory createTypeFactory() {
        return new FieldTypeInferenceTestAnnotatedTypeFactory(checker);
    }
}
