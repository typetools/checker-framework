package tests;

import org.junit.Test;

public class I18nTest extends CheckerTest {

    public I18nTest() {
        super("checkers.i18n.I18nChecker", "i18n", "-Anomsgtext");
    }

    @Test
    public void testLocalizedMessage()                 { test(); }
}
