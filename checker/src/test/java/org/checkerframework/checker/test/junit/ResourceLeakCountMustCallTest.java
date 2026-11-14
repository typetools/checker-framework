package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CompilationResult;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.junit.Assert;
import org.junit.Test;

/** Tests the statistics that the Resource Leak Checker prints under {@code -AcountMustCall}. */
public class ResourceLeakCountMustCallTest {

  /** A leaked JDK resource is counted as found but not as verified. */
  @Test
  public void leakedJdkResourceIsNotVerified() {
    String testDir = "tests/resourceleak-countmustcall";
    TestConfiguration config =
        TestConfigurationBuilder.buildDefaultConfiguration(
            testDir,
            new File(testDir, "CountMustCall.java"),
            ResourceLeakChecker.class,
            List.of("-AcountMustCall"),
            TestUtilities.getShouldEmitDebugInfo());
    CompilationResult result = new TypecheckExecutor().compile(config);
    List<String> messages =
        result.getDiagnostics().stream().map(d -> d.getMessage(Locale.ROOT)).toList();
    int found = countFromMessage(messages, "Found (\\d+) must call obligation");
    int verified = countFromMessage(messages, "Successfully verified (\\d+) must call obligation");
    Assert.assertEquals("Messages: " + messages, found - 1, verified);
  }

  /** A resource whose type is a type variable is counted by the erasure of the type variable. */
  @Test
  public void typeVariableIsCounted() {
    String testDir = "tests/resourceleak-countmustcall";
    TestConfiguration config =
        TestConfigurationBuilder.buildDefaultConfiguration(
            testDir,
            new File(testDir, "TypeVariable.java"),
            ResourceLeakChecker.class,
            List.of("-AcountMustCall"),
            TestUtilities.getShouldEmitDebugInfo());
    CompilationResult result = new TypecheckExecutor().compile(config);
    List<String> messages =
        result.getDiagnostics().stream().map(d -> d.getMessage(Locale.ROOT)).toList();
    Assert.assertTrue("Messages: " + messages, result.compiledWithoutError());
    int found = countFromMessage(messages, "Found (\\d+) must call obligation");
    int verified = countFromMessage(messages, "Successfully verified (\\d+) must call obligation");
    Assert.assertTrue("Messages: " + messages, found > 0);
    Assert.assertEquals("Messages: " + messages, found, verified);
  }

  /**
   * Returns the number in the first message that the given regular expression matches.
   *
   * @param messages diagnostic messages
   * @param regex a regular expression whose first group matches a number
   * @return the number that the first group of {@code regex} matches in the first message that
   *     {@code regex} matches
   */
  private static int countFromMessage(List<String> messages, String regex) {
    Pattern pattern = Pattern.compile(regex);
    for (String message : messages) {
      Matcher m = pattern.matcher(message);
      if (m.find()) {
        return Integer.parseInt(m.group(1));
      }
    }
    throw new AssertionError("No message matches " + regex + "; messages: " + messages);
  }
}
