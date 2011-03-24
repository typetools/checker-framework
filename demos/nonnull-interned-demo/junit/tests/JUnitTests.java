import org.junit.*;

public class JUnitTests {

  /**
   * Tests that assertEquals handles null reference properly.
   */
	@Test(expected = AssertionError.class)
	public void testEquality() {
		Assert.assertEquals(null, "null");
	}

  /**
   * Tests that assertEquals handles arrays with null elements properly
   */
	@Test(expected = AssertionError.class)
	public void testArrays() {
		Assert.assertEquals(new Object[] { null }, new Object[] { "s" });
	}

}
