package daikon.simplify;

import java.io.IOException;

import utilMDE.Assert;

import java.util.logging.Logger;

/**
 * A Check command takes a given proposition and asks the Session to
 * prove it.  The apply method returns when a result is available; the
 * valid field contains the result.
 **/
public class CmdCheck
  implements Cmd
{
  public static final Logger debug = Logger.getLogger("daikon.simplify.CmdCheck");

  private static final String lineSep = System.getProperty("line.separator");

  public final String proposition;
  public boolean valid = false;
  public boolean unknown = false;
  public String counterexample = "";

  public CmdCheck(String proposition) {
    this.proposition = proposition.trim();
    SimpUtil.assert_well_formed(proposition);
  }

  /** For documentation, read the class overview. */
  public void apply(Session s) {
    try {

      String result;
      synchronized(s) {
        // send out the proposition
        s.sendLine(proposition);
        if (Session.dkconfig_verbose_progress > 0) {
          System.out.print("-");
          System.out.flush();
        }

        // read the answer
        // first, the real result
        result = s.readLine();
        if (result == null) {
          throw new SimplifyError("Probable core dump");
        }
        // The "Bad input:"  message generally comes from a syntax error in
        // a previous formula given to Simplify; see the debugging code in
        // simplify.LemmaStack.pushLemmas().
        if (result.startsWith("Bad input:") ||
            result.startsWith("Sx.ReadError in file.")) {
          if (proposition.equals("(OR)")
              && !LemmaStack.dkconfig_synchronous_errors)
            System.err.println("For improved error reporting, try using" +
                               " --config_option " +
                               "daikon.simplify.LemmaStack." +
                               "synchronous_errors=true");

          Assert.assertTrue(false, "Simplify error: " + result + " on "
                            + proposition);
        }
        if (result.equals("Abort (core dumped)")) {
          throw new SimplifyError(result);
        }
        if (result.equals("Counterexample:")) {
          // Suck in the counterexample, if given
          do {
            counterexample += result + lineSep;
            result = s.readLine();
            if (result == null) {
              throw new SimplifyError("Probable core dump");
            }
          } while (result.startsWith(" ") || result.startsWith("\t")
                   || result.equals(""));
        }
        // then, a blank line
        String blank = s.readLine();
        Assert.assertTrue("".equals(blank), "Not a blank line '" + blank +
                          "' after output '" + result + "'");
      }

      // expect "##: [Inv|V]alid."
      int colon = result.indexOf(": ");
      Assert.assertTrue(colon != -1);
      try {
        Integer.parseInt(result.substring(0, colon));
      } catch (NumberFormatException e) {
        Assert.assertTrue(false, "Expected number to prefix result '"
                          + result + "' while checking: " + proposition);
      }
      result = result.substring(colon + 2);
      if ("Valid.".equals(result)) {
        valid = true;
        if (Session.dkconfig_verbose_progress > 0) {
          System.out.print("\bT");
          System.out.flush();
        }
      } else if (result.equals("Unknown.")) {
        valid = false;
        unknown = true;
        if (Session.dkconfig_verbose_progress > 0) {
          System.out.print("\b?");
          System.out.flush();
        }
      } else {
        Assert.assertTrue("Invalid.".equals(result),
                          "unexpected reply " + result);
        if (Session.dkconfig_verbose_progress > 0) {
          System.out.print("\bF");
          System.out.flush();
        }
        valid = false;
      }

      SessionManager.debugln("Result: " + valid);
    } catch (IOException e) {
      throw new SimplifyError(e.toString());
    }
  }

  public String toString() {
    return "CmdCheck: " + proposition;
  }

}
