package org.checkerframework.framework.test.junit;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.*;
import org.junit.runners.Parameterized.Parameters;

/** tests the returns receiver checker's AutoValue integration. */
public class ReturnsReceiverAutoValueTest extends CheckerFrameworkPerDirectoryTest {

    public ReturnsReceiverAutoValueTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker.class,
                "basic",
                "-Anomsgtext",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"returnsreceiverautovalue"};
    }

    @Override
    Collection<String> checkersToRun() {
        ImmutableList.of(
                "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
                "com.google.auto.value.processor.AutoAnnotationProcessor",
                "com.google.auto.value.processor.AutoOneOfProcessor",
                "com.google.auto.value.processor.AutoValueBuilderProcessor",
                "com.google.auto.value.processor.AutoValueProcessor",
                org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker.class.getName());
    }
}
