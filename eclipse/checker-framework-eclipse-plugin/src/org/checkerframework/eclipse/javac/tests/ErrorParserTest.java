package org.checkerframework.eclipse.javac.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.checkerframework.eclipse.javac.JavacError;
import org.checkerframework.eclipse.util.Util;
import org.junit.Test;

public class ErrorParserTest {
    private static final String SIMPLE_TEST_INPUT =
            new StringBuilder()
                    .append(
                            "/home/asumu/gsoc-workspace-4/checker testing/src/GetStarted.java:8: warning: incompatible types.")
                    .append(Util.NL)
                    .append("       @NonNull Integer bar = null;")
                    .append(Util.NL)
                    .append("                              ^")
                    .append(Util.NL)
                    .append("  found   : null")
                    .append(Util.NL)
                    .append("  required: @NonNull Integer")
                    .append(Util.NL)
                    .append(
                            "/home/asumu/gsoc-workspace-4/checker testing/src/GetStarted.java:16: warning: attempting to use a non-@Interned comparison operand")
                    .append(Util.NL)
                    .append("       else if (s1 == obj)")
                    .append(Util.NL)
                    .append("                      ^")
                    .append(Util.NL)
                    .append("  found: Object")
                    .append(Util.NL)
                    .append("2 warnings")
                    .toString();
    private static final String SIMPLE_ERROR_1 =
            new StringBuilder()
                    .append("incompatible types.")
                    .append(Util.NL)
                    .append("       @NonNull Integer bar = null;")
                    .append(Util.NL)
                    .append("  found   : null")
                    .append(Util.NL)
                    .append("  required: @NonNull Integer")
                    .toString();
    private static final String SIMPLE_ERROR_2 =
            new StringBuilder()
                    .append("attempting to use a non-@Interned comparison operand")
                    .append(Util.NL)
                    .append("       else if (s1 == obj)")
                    .append(Util.NL)
                    .append("  found: Object")
                    .toString();
    private static final String OTHER_TEST_INPUT =
            new StringBuilder()
                    .append(
                            "/homes/gws/wmdietl/research/eclipse-workspaces/2010-08-icse/SwingEval/AwtSwing/java/awt/Window.java:58: warning: Disposer is internal proprietary API and may be removed in a future release")
                    .append(Util.NL)
                    .append("import sun.java2d.Disposer;")
                    .append(Util.NL)
                    .append("                 ^")
                    .append(Util.NL)
                    .append(
                            "/homes/gws/wmdietl/research/eclipse-workspaces/2010-08-icse/SwingEval/AwtSwing/java/awt/Window.java:59: warning: Region is internal proprietary API and may be removed in a future release")
                    .append(Util.NL)
                    .append("import sun.java2d.pipe.Region;")
                    .append(Util.NL)
                    .append("                      ^")
                    .append(Util.NL)
                    .append("2 warnings")
                    .toString();
    private static final String OTHER_ERROR_1 =
            new StringBuilder()
                    .append(
                            "Disposer is internal proprietary API and may be removed in a future release")
                    .append(Util.NL)
                    .append("import sun.java2d.Disposer;")
                    .toString();
    private static final String OTHER_ERROR_2 =
            new StringBuilder()
                    .append(
                            "Region is internal proprietary API and may be removed in a future release")
                    .append(Util.NL)
                    .append("import sun.java2d.pipe.Region;")
                    .toString();
    private static final String W_TEST_INPUT =
            new StringBuilder()
                    .append(
                            "/foo/bar/Baz.java:35: warning: AttributesValues is internal proprietary API and may be removed in a future release")
                    .append(Util.NL)
                    .append("  private static void applyStyle(int style, AttributeValues values) {")
                    .append(Util.NL)
                    .append("/foo/bar/Baz.java:38: package sun.java2d.cmm does not exist")
                    .append(Util.NL)
                    .append("import sun.java2d.cmm.ColorTransform")
                    .append(Util.NL)
                    .append("/foo/bar/Baz.java:39: package sun.java2d.cmm does not exist")
                    .append(Util.NL)
                    .append("import sun.java2d.cmm.CMSManager")
                    .append(Util.NL)
                    .append("3 warnings")
                    .toString();
    private static final String W_ERROR_2 =
            new StringBuilder()
                    .append("package sun.java2d.cmm does not exist")
                    .append(Util.NL)
                    .append("import sun.java2d.cmm.ColorTransform")
                    .toString();
    private static final String WINDOWS_INPUT =
            new StringBuilder()
                    .append(
                            "C:/workspace/checker testing/src/GetStarted.java:8: warning: incompatible types.")
                    .append(Util.NL)
                    .append("       @NonNull Integer bar = null;")
                    .append(Util.NL)
                    .append("                              ^")
                    .append(Util.NL)
                    .append("  found   : null")
                    .append(Util.NL)
                    .append("  required: @NonNull Integer")
                    .append(Util.NL)
                    .append(
                            "C:/workspace/checker testing/src/GetStarted.java:16: warning: attempting to use a non-@Interned comparison operand")
                    .append(Util.NL)
                    .append("       else if (s1 == obj)")
                    .append(Util.NL)
                    .append("                      ^")
                    .append(Util.NL)
                    .append("  found: Object")
                    .append(Util.NL)
                    .append("2 warnings")
                    .toString();
    private static final String WINDOWS_ERROR_MSG_1 = SIMPLE_ERROR_1;
    private static final String WINDOWS_ERROR_MSG_2 = SIMPLE_ERROR_2;
    private static final String WARNING_TEST_INPUT =
            new StringBuilder()
                    .append(
                            "/home/asumu/gsoc-workspace-4/checker testing/src/GetStarted.java:8: warning: incompatible types.")
                    .append(Util.NL)
                    .append("       @NonNull Integer bar = null;")
                    .append(Util.NL)
                    .append("warning: foo bar (non-checker error)")
                    .toString();
    private static final String WARNING_TEST_MSG_1 =
            new StringBuilder()
                    .append("incompatible types.")
                    .append(Util.NL)
                    .append("       @NonNull Integer bar = null;")
                    .toString();
    private static final String NOTE_TEST_INPUT =
            new StringBuilder()
                    .append(
                            "/home/asumu/gsoc-workspace-4/checker testing/src/GetStarted.java:8: warning: incompatible types.")
                    .append(Util.NL)
                    .append("       @NonNull Integer bar = null;")
                    .append(Util.NL)
                    .append("Note: foo bar (non-checker error)")
                    .toString();
    private static final String NOTE_TEST_MSG_1 =
            new StringBuilder()
                    .append("incompatible types.")
                    .append(Util.NL)
                    .append("       @NonNull Integer bar = null;")
                    .toString();

    @Test
    public void emptyTest() {
        List<JavacError> errors = JavacError.parse("");

        assert (errors.isEmpty());
    }

    @Test
    public void simpleParseTest() {
        List<JavacError> errors = JavacError.parse(SIMPLE_TEST_INPUT);

        assertEquals(2, errors.size());
        assertEquals(8, errors.get(0).lineNumber);
        assertEquals(SIMPLE_ERROR_1, errors.get(0).message);
        assertEquals(16, errors.get(1).lineNumber);
        assertEquals(SIMPLE_ERROR_2, errors.get(1).message);
    }

    @Test
    public void otherParseTest() {
        List<JavacError> errors = JavacError.parse(OTHER_TEST_INPUT);

        assertEquals(2, errors.size());
        assertEquals(OTHER_ERROR_1, errors.get(0).message);
        assertEquals(58, errors.get(0).lineNumber);
        assertEquals(OTHER_ERROR_2, errors.get(1).message);
        assertEquals(59, errors.get(1).lineNumber);
    }

    @Test
    public void wParseTest() {
        List<JavacError> errors = JavacError.parse(W_TEST_INPUT);

        assertEquals(3, errors.size());
        assertEquals(W_ERROR_2, errors.get(1).message);
    }

    @Test
    public void windowsParseTest() {
        List<JavacError> errors = JavacError.parse(WINDOWS_INPUT);

        assertEquals(2, errors.size());
        assertEquals(8, errors.get(0).lineNumber);
        assertEquals(WINDOWS_ERROR_MSG_1, errors.get(0).message);
        assertEquals(16, errors.get(1).lineNumber);
        assertEquals(WINDOWS_ERROR_MSG_2, errors.get(1).message);
    }

    @Test
    public void warningParseTest() {
        List<JavacError> errors = JavacError.parse(WARNING_TEST_INPUT);

        assertEquals(1, errors.size());
        assertEquals(WARNING_TEST_MSG_1, errors.get(0).message);
    }

    @Test
    public void noteParseTest() {
        List<JavacError> errors = JavacError.parse(NOTE_TEST_INPUT);

        assertEquals(1, errors.size());
        assertEquals(NOTE_TEST_MSG_1, errors.get(0).message);
    }
}
