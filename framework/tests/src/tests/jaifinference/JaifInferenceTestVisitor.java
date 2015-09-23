package tests.jaifinference;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
/**
 * Visitor for a simple type system to test a whole-program type inference
 * using .jaif files.
 *
 * @author pbsf
 */
public class JaifInferenceTestVisitor extends BaseTypeVisitor<JaifInferenceTestAnnotatedTypeFactory> {

    public JaifInferenceTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected JaifInferenceTestAnnotatedTypeFactory createTypeFactory() {
        return new JaifInferenceTestAnnotatedTypeFactory(checker);
    }
}
