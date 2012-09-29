package daikon.test;

import junit.framework.*;
import daikon.*;
import daikon.tools.DtraceDiff;
import java.lang.reflect.*;
import java.net.URL;

public class DtraceDiffTester extends TestCase {

  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(DtraceDiffTester.class));
  }

  public DtraceDiffTester(String name) {
    super(name);
  }

  private static boolean diff(String file1, String file2) {
    //System.out.println("Diff: " + file1 + " " + file2);
    return DtraceDiff.mainTester(new String[] {find(file1), find(file2)});
  }

  private static boolean diff(String option, String optval,
			      String file1, String file2) {
    //System.out.println("Diff: " + file1 + " " + file2);
    return DtraceDiff.mainTester(new String[] {option, optval, find(file1), find(file2)});
  }


  private static String find(String file) {
    String file1 = "daikon/test/dtracediff/" + file;
    URL input_file_location =
      ClassLoader.getSystemClassLoader().getSystemResource(file1);
    assertTrue(input_file_location != null);
    return input_file_location.getFile();
  }

  public void test_samples () {
    // these tests should succeed
    assertTrue(diff("AllTypes.dtrace.gz", "AllTypes.dtrace.gz"));
    assertTrue(diff("Hanoi.dtrace.gz", "Hanoi.dtrace.gz"));
    assertTrue(diff("Hanoi.dtrace.gz", "Hanoi-mungpointers.dtrace.gz"));

    // test for the diffs that this utility is supposed to find
    assertFalse(diff("Hanoi.dtrace.gz", "Hanoi-badvar.dtrace.gz"));
    assertFalse(diff("Hanoi.dtrace.gz", "Hanoi-badvalue.dtrace.gz"));
    assertFalse(diff("Hanoi.dtrace.gz", "Hanoi-truncated.dtrace.gz"));

    // test that command-line options work (to avoid comparing ppts with
    // a missing variable)
    assertTrue(diff("--ppt-omit-pattern", "six170.Hanoi.showTowers*",
		    "Hanoi.dtrace.gz", "Hanoi-badvar.dtrace.gz"));
    assertTrue(diff("--var-omit-pattern", "this.height",
		    "Hanoi.dtrace.gz", "Hanoi-badvar.dtrace.gz"));
    assertTrue(diff("--ppt-select-pattern", "six170.Hanoi.moveDisk*",
		    "Hanoi.dtrace.gz", "Hanoi-badvar.dtrace.gz"));
    // needs to test --var-select-pattern
  }

}
