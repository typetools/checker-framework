package org.checkerframework.javacutil;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link UserError}. */
public class UserErrorTest {

  /** Creates a new UserErrorTest. */
  public UserErrorTest() {}

  /** A UserError constructed with a message and a cause retains the cause. */
  @Test
  public void testMessageAndCause() {
    IOException cause = new IOException("the cause");
    UserError e = new UserError("the message", cause);
    Assert.assertEquals("the message", e.getMessage());
    Assert.assertSame(cause, e.getCause());
  }

  /** A UserError constructed with a cause and a format string retains the cause. */
  @Test
  public void testCauseAndFormatString() {
    IOException cause = new IOException("the cause");
    UserError e = new UserError(cause, "Problem while writing %s: %s", "f.jaif", cause);
    Assert.assertEquals(
        "Problem while writing f.jaif: java.io.IOException: the cause", e.getMessage());
    Assert.assertSame(cause, e.getCause());
  }

  /**
   * A Throwable as the sole vararg selects the (String, Throwable) constructor: it is the cause,
   * not a format argument.
   */
  @Test
  public void testThrowableArgIsCause() {
    IOException cause = new IOException("the cause");
    UserError e = new UserError("Cannot read %s", cause);
    Assert.assertEquals("Cannot read %s", e.getMessage());
    Assert.assertSame(cause, e.getCause());
  }

  /**
   * A message that is not a format string is used literally, even if it contains a percent sign.
   */
  @Test
  public void testMessageIsNotFormatString() {
    IOException cause = new IOException("the cause");
    UserError e = new UserError("Cannot create /tmp/100%done", cause);
    Assert.assertEquals("Cannot create /tmp/100%done", e.getMessage());
  }

  /** A UserError constructed without a cause has none. */
  @Test
  public void testNoCause() {
    UserError e = new UserError("the %s", "message");
    Assert.assertEquals("the message", e.getMessage());
    Assert.assertNull(e.getCause());
  }
}
