package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CalledMethodsDisableframeworksTest extends CheckerFrameworkPerDirectoryTest {

    public CalledMethodsDisableframeworksTest(List<File> testFiles) {
        super(
                testFiles,
                Arrays.asList(
                        "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
                        "com.google.auto.value.processor.AutoAnnotationProcessor",
                        "com.google.auto.value.processor.AutoOneOfProcessor",
                        "com.google.auto.value.processor.AutoValueBuilderProcessor",
                        "com.google.auto.value.processor.AutoValueProcessor",
                        CalledMethodsChecker.class.getName()),
                "calledmethods-disableframeworks",
                Collections.emptyList(),
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
}
