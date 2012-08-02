package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull checker.
 */
public class NonNullTest extends ParameterizedCheckerTest {

	public NonNullTest(File testFile) {
		super(testFile, checkers.nonnull.NonNullFbcChecker.class.getName(),
				"nonnull", "-Anomsgtext" /* , "-AprintErrorStack", "-Ashowchecks" */);
	}

	@Parameters
	public static Collection<Object[]> data() {
		return testFiles("nonnull");
	}

}
