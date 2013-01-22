package daikon.simplify;

/**
 * A Raw command provides no additional structure, allowing arbitrary
 * commands (as long as they have no ouput) to be sent to the
 * prover. It will not block.
 **/
public class CmdRaw
  implements Cmd
{
  public final String cmd;

  public CmdRaw(String cmd) {
    this.cmd = cmd.trim();
    SimpUtil.assert_well_formed(this.cmd);
  }

  /** For documentation, read the class overview. */
  public void apply(Session s) {

    synchronized(s) {
      // send out the command
      s.sendLine(cmd);
      // there is no output from Simplify
    }

  }

  public String toString() {
    return "CmdRaw: " + cmd;
  }

}
