package testaccumulation;

import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import testaccumulation.qual.TestAccumulation;
import testaccumulation.qual.TestAccumulationBottom;
import testaccumulation.qual.TestAccumulationTop;

/** The annotated type factory for a test accumulation checker. */
public class TestAccumulationAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {
    /**
     * Create a new accumulation checker's annotated type factory.
     *
     * @param checker the checker
     */
    public TestAccumulationAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(
                checker,
                TestAccumulationTop.class,
                TestAccumulation.class,
                TestAccumulationBottom.class);
        this.postInit();
    }
}
