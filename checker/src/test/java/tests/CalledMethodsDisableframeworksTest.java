package tests;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class CalledMethodsDisableframeworksTest extends CheckerFrameworkPerDirectoryTest {

    public CalledMethodsDisableframeworksTest(List<File> testFiles) {
        super(
                testFiles,
                CalledMethodsChecker.class,
                "calledmethods-disableframeworks",
                "-Anomsgtext",
                "-AdisableBuilderFrameworkSupports=autovalue,lombok",
                // The next option is so that we can run the usevaluechecker tests under this
                // configuration.
                "-ACalledMethodsChecker_useValueChecker",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"calledmethods-disableframeworks", "calledmethods-usevaluechecker"};
    }

    /**
     * copy-pasted code from {@link CheckerFrameworkPerDirectoryTest#run()}, except that we change
     * the annotation processors to {@link #ANNOTATION_PROCS}
     */
    @Override
    public Collection<String> checkersToRun() {
        return Arrays.asList(
                "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
                "com.google.auto.value.processor.AutoAnnotationProcessor",
                "com.google.auto.value.processor.AutoOneOfProcessor",
                "com.google.auto.value.processor.AutoValueBuilderProcessor",
                "com.google.auto.value.processor.AutoValueProcessor",
                CalledMethodsChecker.class.getName());
    }
}
