package org.checkerframework.checker.test.junit;

import java.util.Properties;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that the checkers of the Resource Leak Checker, which issue the diagnostics, have the
 * message text for their message keys. Other tests run with {@code -Anomsgtext}, so they do not
 * notice missing text.
 */
public class ResourceLeakMessagesTest {

  /** Message keys issued by the Resource Leak Checker (the MustCallConsistencyAnalyzer). */
  private static final String[] RLC_KEYS = {"required.method.not.called", "reset.not.owning"};

  /** Message keys issued by the RLCCalledMethodsChecker (the RLCCalledMethodsVisitor). */
  private static final String[] RLCCM_KEYS = {"required.method.not.called", "owning.override.param"};

  @Test
  public void resourceLeakCheckerHasMessageText() {
    assertHasMessages(new ResourceLeakChecker().getMessagesProperties(), RLC_KEYS);
  }

  @Test
  public void rlcCalledMethodsCheckerHasMessageText() {
    assertHasMessages(new RLCCalledMethodsChecker().getMessagesProperties(), RLCCM_KEYS);
  }

  private static void assertHasMessages(Properties messages, String[] keys) {
    for (String key : keys) {
      Assert.assertNotNull("No message text for " + key, messages.getProperty(key));
    }
  }
}
