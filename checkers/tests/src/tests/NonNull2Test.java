package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests from the Nullness Checker for the NonNull checker. Some tests have been adapted.
 */
public class NonNull2Test extends ParameterizedCheckerTest {

	public NonNull2Test(File testFile) {
		super(testFile, checkers.nonnull.NonNullChecker.class.getName(),
				"nonnull", "-AassumeAssertionsAreEnabled", "-Anomsgtext");
	}

	@Parameters
	public static Collection<Object[]> data() {
		return testFiles("nonnull2");
	}

}
