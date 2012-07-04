package tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({Flow2Test.class, RegexTest.class, I18nTest.class, PuritySuggestionsTest.class})
public class AllFlowTests {
}