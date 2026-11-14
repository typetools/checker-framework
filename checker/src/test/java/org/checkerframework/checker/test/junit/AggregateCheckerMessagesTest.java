package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that an aggregate checker has the message text of its subcheckers. Per-directory tests do
 * not notice missing text, because the test framework compares an expected diagnostic with an
 * issued one by its message key, not by its message text.
 */
public class AggregateCheckerMessagesTest {

  /**
   * The Resource Leak Checker reports {@code required.method.not.called}, whose text is defined by
   * its subchecker.
   */
  @Test
  public void resourceLeakCheckerHasSubcheckerMessageText() {
    String key = "required.method.not.called";
    String expected = new RLCCalledMethodsChecker().getMessagesProperties().getProperty(key);
    Assert.assertNotNull("No message text for " + key + " in RLCCalledMethodsChecker", expected);
    Assert.assertEquals(
        expected, new ResourceLeakChecker().getMessagesProperties().getProperty(key));
  }
}
