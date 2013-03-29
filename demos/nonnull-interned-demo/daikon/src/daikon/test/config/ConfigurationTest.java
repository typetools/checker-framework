package daikon.test.config;

import junit.framework.*;
import daikon.config.*;

public class ConfigurationTest
  extends TestCase
{

  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(ConfigurationTest.class));
  }

  public ConfigurationTest(String name) {
    super(name);
  }

  // Mostly useful to check that our resource files are bound correctly
  public void testGetInstance() {
    // Executed for side effect.
    Configuration.getInstance();
  }

}
