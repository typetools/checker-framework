// TraceSelect.java
package daikon.tools;

import java.util.*;
import java.io.*;
import utilMDE.*;

public class TraceSelect {

  private static final int DEFAULT_NUM = 10;


  public static boolean CLEAN = true;
  public static boolean INCLUDE_UNRETURNED = false;
  public static boolean DO_DIFFS = false;

  private static int num_reps;

  private static String filePrefix;
  private static String fileName = null;

  // Just a quick command line cache
  private static String[] argles;
  // // stores the invocations in Strings
  // private static ArrayList invokeBuffer;

  private static int numPerSample;

  private static Random randObj;


  private static int daikonArgStart = 0;

  // This allows us to simply call MultiDiff
  // with the same files we just created
  private static String[] sampleNames;

  private static final String usage =
    UtilMDE.joinLines(
    "USAGE: TraceSelect num_reps sample_size [options] [Daikon-args]...",
    "Example: java TraceSelect 20 10 -NOCLEAN -INCLUDE_UNRETURNED-SEED 1000 foo.dtrace foo2.dtrace foo.decls RatPoly.decls foo3.dtrace");

  public static void main (String[] args) {
    try {
      mainHelper(args);
    } catch (daikon.Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than daikon.Daikon.TerminationMessage gets propagated.
    // This simplifies debugging by showing the stack trace.
  }

  /**
   * This does the work of main, but it never calls System.exit, so it
   * is appropriate to be called progrmmatically.
   * Termination of the program with a message to the user is indicated by
   * throwing daikon.Daikon.TerminationMessage.
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   **/
  public static void mainHelper(final String[] args) {
    argles = args;
    if (args.length == 0) {
      throw new daikon.Daikon.TerminationMessage("No arguments found.", usage);
    }

    num_reps = Integer.parseInt (args[0]);
    numPerSample = Integer.parseInt (args[1]);

    // process optional switches
    // also deduce index of arg for Daikon
    boolean knowArgStart = false;
    for (int i = 2; i < args.length; i++) {
      // allows seed setting
      if (args[i].toUpperCase().equals ("-SEED")) {
        if (i+1 >= args.length) {
          throw new daikon.Daikon.TerminationMessage ("-SEED options requires argument");
        }
        randObj = new Random (Long.parseLong (args[++i]));
        daikonArgStart = i+1;
      }

      // NOCLEAN argument will leave the trace samples even after
      // the invariants from these samples have been generated
      else if (args[i].toUpperCase().equals ("-NOCLEAN")) {
        CLEAN = false;
        daikonArgStart = i+1;
      }

      // INCLUDE_UNRETURNED option will allow selecting method invocations
      // that entered the method successfully but did not exit normally;
      // either from a thrown Exception or abnormal termination.
      else if (args[i].toUpperCase().equals ("-INCLUDE_UNRETURNED")) {
        INCLUDE_UNRETURNED = true;
        daikonArgStart = i+1;
      }

      // DO_DIFFS will create an spinfo file for generating
      // conditional invariants and implications by running
      // daikon.diff.Diff over each of the samples and finding
      // properties that appear in some but not all of the
      // samples.
      else if (args[i].toUpperCase().equals ("-DO_DIFFS")) {
        DO_DIFFS = false;
        daikonArgStart = i+1;
      }

      // TODO: The current implementation assumes that a decls
      // or dtrace file will be the first of the Daikon arguments,
      // marking the end of the TraceSelect arguments.  That is
      // not necessarily true, especially in cases when someone
      // uses a Daikon argument such as "--noheirarchy" or "--format java"
      // and the manual examples place the arguments before any dtrace
      // or decls arguments.

      // For now, only the first dtrace file will be sampled
      else if (args[i].endsWith (".dtrace")) {
        if (fileName == null) {
          fileName = args[i];
        }
        else {
          throw new daikon.Daikon.TerminationMessage ("Only 1 dtrace file for input allowed");
        }

        if (!knowArgStart) {
          daikonArgStart = i;
          knowArgStart = true;
        }
      }

      else if (args[i].endsWith (".decls")) {
        if (!knowArgStart) {
          daikonArgStart = i;
          knowArgStart = true;
        }
      }


    }

    // if no seed provided, use default Random() constructor
    if (randObj == null) {
      randObj = new Random();
    }

    sampleNames = new String[num_reps + 1];
    sampleNames[0] = "-p";




    try {

      // invokeBuffer = new ArrayList();
      //	    fileName = args[1];

      System.out.println ("*******Processing********");

      // Have to call the DtraceNonceDoctor
      // to avoid the broken Dtrace from
      // using a command-line 'cat' that
      // results in repeat nonces
      /*
        String[] doctorArgs = new String[1];
        doctorArgs[0] = fileName;
        DtraceNonceDoctor.main (doctorArgs );
        Runtime.getRuntime().exec ("mv " + doctorArgs[0] + "_fixed " +
        doctorArgs[0]);
      */

      while (num_reps > 0) {

        DtracePartitioner dec =
          new DtracePartitioner (fileName);
        MultiRandSelector<String> mrs = new MultiRandSelector<String> (numPerSample,
                                                       dec);


        while (dec.hasNext()) {
          mrs.accept (dec.next());
        }
        List<String> al = new ArrayList<String>();

        for (Iterator<String> i = mrs.valuesIter(); i.hasNext();) {
          al.add (i.next());
        }

        al = dec.patchValues (al, INCLUDE_UNRETURNED);

        filePrefix = calcOut (fileName);

        // gotta do num_reps - 1 because of "off by one"
        // but now add a '-p' in the front so it's all good
        sampleNames[num_reps] = filePrefix + ".inv";

        PrintWriter pwOut = new PrintWriter
          (UtilMDE.bufferedFileWriter (filePrefix));

        for (String toPrint : al) {
          pwOut.println (toPrint);
        }
        pwOut.flush();
        pwOut.close();

        invokeDaikon(filePrefix);

        // cleanup the mess
        if (CLEAN) {
          Runtime.getRuntime().exec( "rm " + filePrefix);
        }

        num_reps--;
      }


      if (DO_DIFFS) {
        // histograms
        //  daikon.diff.Diff.main (sampleNames);

        // spinfo format
        daikon.diff.MultiDiff.main (sampleNames);



      }

      // cleanup the mess!
      for (int j = 0; j < sampleNames.length; j++) {
        if (CLEAN) {
          Runtime.getRuntime().exec ("rm " + sampleNames[j]);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void invokeDaikon(String dtraceName) throws IOException {

    System.out.println ("Created file: " + dtraceName);
    String[] daikonArgs = {	 dtraceName,
                                 "-o", dtraceName + ".inv"};

    // this part adds on the rest of the decls files
    ArrayList<String> al = new ArrayList<String> ();
    al.add (dtraceName);
    al.add ("-o");
    al.add (dtraceName + ".inv");


    // find all the Daikon args except for the original
    // single dtrace file.
    for (int i = daikonArgStart; i < argles.length; i++) {
      if (argles[i].endsWith (".dtrace")) {
        continue;
      }
      al.add (argles[i]);
    }

    // create an array to store the Strings in al
    daikonArgs = new String [al.size()];
    for (int i = 0; i < daikonArgs.length; i++) {
      daikonArgs[i] = al.get(i);
    }


    // initializes daikon again or else an exception is thrown
    daikon.Daikon.inv_file = null;
    daikon.Daikon.main (daikonArgs);
    Runtime.getRuntime().exec ("java daikon.PrintInvariants "
                               + dtraceName + ".inv" + " > "
                               + dtraceName + ".txt");

    return;
  }

  /** Used when I used to select by probability, not absolute number. */
  private static boolean myRand (String[] args) {
    if (args.length >= 2) try {
      double prob = Double.parseDouble (args[3]);
      return Math.random() > prob;
    }
    catch (Exception e) {

      return (Math.random() > 0.900);
    }
    // Defaults to 10% chance of keeping
    return (Math.random() > 0.900);
  }


  private static String calcOut (String strFileName) {
    StringBuffer product = new StringBuffer();
    int index = strFileName.indexOf ('.');
    if (index >= 0) {
      product.append(strFileName.substring (0, index));
      product.append(num_reps);
      if (index  != strFileName.length())
        product.append (strFileName.substring (index));
    }
    else product.append (strFileName).append ("2");
    return product.toString();
  }

}

// I don't think any of this is used anymore...
// Now all of the random selection comes from the
// classes in utilMDE.

class InvocationComparator implements Comparator<String> {
    /** Requires:  s1 and s2 are String representations of invocations
     *  from a tracefile. */
    public int compare (String s1, String s2) {
        if (s1 == s2) {
            return 0;
        }

	// sorts first by program point
	int pptCompare = s1.substring (0, s1.indexOf(":::")).compareTo
	    (s2.substring (0, s2.indexOf(":::")));
	if (pptCompare != 0) return pptCompare;

	// next sorts based on the other stuff
	int nonce1 = getNonce (s1);
	int nonce2 = getNonce (s2);
	int type1 = getType (s1);
	int type2 = getType (s2);
	// This makes sure nounce takes priority, ties are broken
	// so that ENTER comes before EXIT for the same program point
	return 3 * (nonce1 - nonce2) + (type1 - type2);
    }

    private int getNonce (String s1) {
	if (s1.indexOf ("OBJECT") != -1 ||
	    s1.indexOf ("CLASS") != -1) {
	    // it's ok, no chance of overflow wrapa round
	    return Integer.MAX_VALUE;
	}
	StringTokenizer st = new StringTokenizer(s1);
	st.nextToken();
	st.nextToken();
	return Integer.parseInt (st.nextToken());
    }

    private int getType(String s1) {
      // we want ENTER to come before EXIT
      if (s1.indexOf ("CLASS") != -1) return -1;
      if (s1.indexOf ("OBJECT") != -1) return 0;
      if (s1.indexOf ("ENTER") != -1) return 1;
      if (s1.indexOf ("EXIT") != -1) return 2;
      System.out.println ("ERROR" + s1);
      return 0;
    }
}
