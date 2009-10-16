package daikon.simplify;

/**
 * An Assume command pushes some proposition onto the assumption stack
 * of the session.  The proposition is assumed to be true, and is not
 * proved.  This command will not block.
 **/
public class CmdAssume
  implements Cmd
{
  public final String proposition;

  public CmdAssume(String proposition) {
    this.proposition = proposition.trim();
    SimpUtil.assert_well_formed(proposition);
  }

  /** For documentation, read the class overview. */
  public void apply(Session s) {

    synchronized(s) {
      // send out the (BG_PUSH proposition)
      s.sendLine("(BG_PUSH " + proposition + ")");
      if (Session.dkconfig_verbose_progress > 1) {
        System.out.print("<");
        System.out.flush();
      }

      // there is no output from Simplify
    }

  }

  public String toString() {
    return "CmdAssume: " + proposition;
  }

}
