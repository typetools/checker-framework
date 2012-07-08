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
@SuiteClasses({ Flow2Test.class, RegexTest.class, I18nTest.class,
        PuritySuggestionsTest.class })
public class AllFlowTests {
}
