package tests;

import org.junit.Test;

public class RegexTest extends CheckerTest {

    public RegexTest() {
        super("checkers.regex.RegexChecker", "regex", "-Anomsgtext");
    }

    @Test
    public void testSimple()                 { test(); }
}
