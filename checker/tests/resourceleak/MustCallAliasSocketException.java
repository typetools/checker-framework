// Based on a false positive in Zookeeper's SaslQuorumAuthLearner.

import java.io.*;
import java.net.*;

class MustCallAliasSocketException {

  public boolean quorumRequireSasl;

  // This socket isn't owning, so we shouldn't warn on it.
  public void authenticate(Socket sock, String hostName) throws IOException {
    if (!quorumRequireSasl) {
      // I kept this block in the test case because it demonstrates
      // that sock is definitely non-owning.
      System.out.println("Skipping SASL authentication as");
      return;
    }
    try {
      DataOutputStream dout = new DataOutputStream(sock.getOutputStream());
      // Before MCA was implemented, the call to getInputStream() below triggered
      // a false positive warning that dout had not been closed.
      DataInputStream din = new DataInputStream(sock.getInputStream());
      // ~30 lines omitted...
    } finally {
      // do some other things that are definitely not closing sock
    }
  }
}
