// DtracePartitioner.java
package daikon.tools;
import java.util.*;
import java.io.*;
import utilMDE.*;

/** This class partitions Daikon trace files so that invocations of
 *  the same program point are grouped together for use with random
 *  selection.
 *
 */

public class DtracePartitioner
  implements Partitioner<String,String>, Iterator<String>
{

  private static final String lineSep = System.getProperty("line.separator");

  // reading from the file as a lazy iterator
  private BufferedReader br;
  // the name of the Daikon trace file
  private String fileName;

  /** @param filename The Daikon trace file to be partitioned
   */
  public DtracePartitioner (String filename) {
    try {
      this.fileName = filename;
      br = UtilMDE.bufferedFileReader (fileName);

    } catch (IOException e) {e.printStackTrace(); }
  }

  public boolean hasNext() {
    try {
      return br.ready();
    } catch (IOException e) {e.printStackTrace(); return false; }
  }

  /** Not implemented, because this class does not modify the underlying
      trace file. */
  public void remove () {
    throw new UnsupportedOperationException("Can not remove");
  }



  public String next() {
    try {
      String ret = grabNextInvocation ();
      if (ret.indexOf ("EXIT") != -1) {
        if (!br.ready()) return "";
        return next();
      }
      else return ret;
    } catch (IOException e) {e.printStackTrace(); }
    throw new RuntimeException ("Should never reach this statement");

  }

  /** Grabs the next invocation in the Daikon trace file by interpreting
   * a blank line as the invocation delimter.  Note that multiple blank
   * lines between invocations might occur, so the callee is responsible
   * for checking if the returned String is a blank line */
  private String grabNextInvocation () throws IOException {
    StringBuffer sb = new StringBuffer();
    while (br.ready()) {
      String line = br.readLine();
      line = line.trim();
      if (line.equals ("")) {
        break;
      }
      sb.append(line).append (lineSep);
    }
    return sb.toString();
  }


  /** Returns the program point name given by the input invocation.
   *  Throws RuntimeException if invocation is not instanceof String.
   */
  public String assignToBucket (String invocation) {
    if (invocation.indexOf (lineSep) == -1) return null;
    return invocation.substring (0, invocation.indexOf (lineSep));
  }

  // TODO: this should be a Javadoc link
  /** Same as this.patchValues (enters, false)
   */
  public List<String> patchValues (List<String> enters) {
    return patchValues (enters, false);
  }

  /** Finds the exits that correspond to Enters.
   *  <br>Modifies: none
   *  <br>Returns: An ArrayList containing all of the elements of 'enters'
   *  <br> @param includeUnreturnedEnters
   *    ensures that any ENTER ppt invocations will definitely have
   *    a corresponding EXIT ppt invocation following them.
   *  <p> The original order is NOT guaranteed.
   */

  public List<String> patchValues (List<String> enters, boolean includeUnreturnedEnters) {
    try {
      System.out.println ("Entering patchValues");
      // Keep a list of enters that are so far unmatched
      Set<String> unreturned = new HashSet<String> (enters);

      // Build a hashmap of values to watch
      HashMap<Object/*String or Integer*/,String> nonceMap = new HashMap<Object,String>();
      for (String enterStr : enters) {
        // it could be an OBJECT or CLASS invocation ppt, ignore those
        // by putting them in the HashMap to themselves, they'll
        // be reaped up later
        if (enterStr.indexOf ("ENTER") == -1) {
          nonceMap.put (enterStr, enterStr);
          // no way for OBJECT or CLASS to be unresolved
          unreturned.remove (enterStr);
          continue;
        }

        // get the nonce of this invocation and use it
        // as the key in the nonceMap, which maps
        // nonces --> ENTER half of invocation
        int theNonce = calcNonce (enterStr);
        nonceMap.put (new Integer (theNonce), enterStr);
      }



      // look for EXIT half of invocations and augment
      // the values of nonceMap so that the map eventually
      // maps nonces --> full invocations with ENTER / EXIT
      br = UtilMDE.bufferedFileReader(fileName);
      while (br.ready()) {
        String nextInvo = grabNextInvocation();
        if (nextInvo.indexOf ("EXIT") == -1) continue;
        int invoNonce = calcNonce (nextInvo);
        Integer key = new Integer (invoNonce);
        String enterInvo = nonceMap.get (key);
        if (enterInvo != null) {
          nonceMap.put (key, enterInvo + lineSep + nextInvo);
          unreturned.remove (enterInvo);
        }
      }

      // Return a list of all the invocations where matching ENTER and
      // EXIT points were found as well as the OBJECT and CLASS
      // invocations.
      ArrayList<String> al = new ArrayList<String>();
      for (String s : nonceMap.values()) {
        al.add (s);
      }
      // add in the invocations that were never resolved because no
      // matching EXIT invocation exists.
      if (!includeUnreturnedEnters) {
        al.removeAll (unreturned);
      }
      return al;


    } catch (IOException e) {e.printStackTrace(); }
    return enters;
  }

  private int calcNonce (String invocation) {
    StringTokenizer st = new StringTokenizer (invocation, lineSep);
    while (st.hasMoreTokens()) {
      String line = st.nextToken();
      if (line.equals ("this_invocation_nonce"))
        return Integer.parseInt (st.nextToken());
    }
    throw new RuntimeException ("This invocation didn't contain a nonce: "
                                + invocation);

  }

}
