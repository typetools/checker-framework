package daikon.test;

import java.io.*;
import java.util.*;
import junit.framework.*;

/**
 * This tests various aspects of VarInfoName's and transforming
 * VarInfoName's.  This calls VarInfoNameDriver after parsing all
 * input files of the name "varInfoNameTest.<foo>".  VarInfoNameDriver
 * does transform tests, and its output is compared to the
 * "varInfoNameTest.<foo>.goal" file by this.
 *
 * <br>
 *
 * To add a new test case, add a line to the <foo> file and a line to
 * the goal file with intended output.  Format of the <foo> file is
 * output method, followed by a variable name.  Output methods are
 * defined in VarInfoNameDriver.  To add a new transformation method
 * (which can then be tested in test cases) add a static Handler
 * implementation to VarInfoNameDriver modeled after one of the ones
 * already present and add a static {} line after to add the handler
 * to the list of handlers.
 **/
public class VarInfoNameTest
  extends TestCase
{

  private static final String lineSep = daikon.Global.lineSep;

  // for convenience
  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(VarInfoNameTest.class));
  }

  public VarInfoNameTest(String name) {
    super(name);
  }

  public void testParse() { run("testParse"); }
  public void testEscForall() { run("testEscForall"); }
  public void testSubscript() { run("testSubscript"); }
  public void testJML() { run("testJML"); }

  private void run(String name) {
    String file = "varInfoNameTest." + name;
    InputStream input_stream = VarInfoNameTest.class.getResourceAsStream(file);
    InputStream goal_stream = VarInfoNameTest.class.getResourceAsStream(file + ".goal");

    // run the tests
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    VarInfoNameDriver.run(input_stream, new PrintStream(out));

    // put output into actual
    List<String> _actual = new ArrayList<String>();
    StringTokenizer tok = new StringTokenizer(out.toString(), lineSep);
    while (tok.hasMoreTokens()) {
      _actual.add(tok.nextToken());
    }
    String[] actual = _actual.toArray(new String[_actual.size()]);

    // put desired into goal
    List<String> _goal = new ArrayList<String>();
    try {
      BufferedReader buf = new BufferedReader(new InputStreamReader(goal_stream));
      while (buf.ready()) {
        String line = buf.readLine();
        _goal.add(line);
      }
      buf.close();
    } catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
    String[] goal = _goal.toArray(new String[_goal.size()]);

    // diff desired and output
    diff(goal, actual);
  }

  private void diff(String[] goal, String[] actual) {
    for (int i=0; i < goal.length; i++) {
      String goal_line = goal[i];
      if (i >= actual.length) {
        fail("Diff error:" + lineSep + "Actual had too few lines, starting with goal line:" + lineSep + "\t" + goal_line);
      }
      String actual_line = actual[i];
      if (!goal_line.equals(actual_line)) {
        String goals = "";
        String actuals = "";
        int low = Math.max(0, i-3);
        int high = Math.min(Math.min(i+3, actual.length-1), goal.length-1);
        for (int j = low; j <= high; j++) {
          if (!goal[j].equals(actual[j])) {
            goals += ">";
            actuals += ">";
          }
          goals += "\t" + goal[j] + lineSep;
          actuals += "\t" + actual[j] + lineSep;
        }
        fail("Diff error:" + lineSep + "Different output encountered.  Expected:" + lineSep +
             goals + "Received:" + lineSep + actuals + " on line: " + i);
      }
    }
    if (actual.length > goal.length) {
      StringBuffer extra = new StringBuffer();
      for (int i = goal.length; i < actual.length; i++) {
        extra.append ("\t");
        extra.append (actual[i]);
        extra.append (lineSep);
      }
      fail("Diff error:" + lineSep + "Actual had extra lines:" + lineSep +
           extra.toString());

    }
  }

  // parsing
  // interning
  // *name()
  // object methods

  // Simple
  // Size
  // Function
  // TypeOf
  // Prestate
  // Poststate
  // Add
  // Elements
  // Subscript
  // Slice

  // ElementsFinder
  // Replacer
  // InorderFlattener
  // QuantifierVisitor
  // QuantHelper.format_esc
}
