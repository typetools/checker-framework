package tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * A collection of all test targets that allready work with the new dataflow
 * framework. This class will be removed once all tests pass.
 *
 * @author Stefan Heule
 */
@RunWith(Suite.class)
@SuiteClasses({ AnnotationBuilderTest.class, BasicEncryptedTest.class,
        BasicSuperSubTest.class, FenumTest.class, FenumSwingTest.class,
        FlowTest.class, Flow2Test.class, FrameworkTest.class, I18nTest.class,
        InterningTest.class, LubGlbTest.class, PolyAllTest.class,
        PuritySuggestionsTest.class, RegexTest.class,
        ReportModifiersTest.class, ReportTest.class, ReportTreeKindsTest.class,
        SignatureTest.class, TaintingTest.class, TreeParserTest.class,
        UnitsTest.class, NonNullFbcTest.class, NonNullRawnessTest.class })
public class AllFlowTests {
}
