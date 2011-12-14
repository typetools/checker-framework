package daikon.tools.gretel;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;

// see /g4/projects/invariants/tools/Gretel/current/lib/residue

/**
 * Given a .gretel file and a list of source file names, produce a
 * report of the line coverage on those files.
 **/
public class CoverageStats
{

  public static void main(String[] args)
    throws Exception
  {

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
  public static void mainHelper(final String[] args)
    throws Exception
  {
    String gretelFile = args[0];
    List<String> relevant = Arrays.asList(args).subList(1, args.length);

    // We use reflection for Gretel calls so that people don't have to
    // download Gretel to build Daikon.
    Class<?> clazzListHits = Class.forName("residue.ListHits");

    // setConfigFile(gretelFile);
    {
      Method m = clazzListHits.getMethod("setConfigFile", new Class[] { String.class });
      m.invoke(null, new Object[] { gretelFile });
    }

    Map<String,Set<Integer>> hits, misses;
    // hits = getHits(); misses = getMisses();
    {
      Method m;
      m = clazzListHits.getMethod("getHits", (Class[])null);
      hits = (Map<String,Set<Integer>>) m.invoke(null, (Object[])null); // unchecked cast
      m = clazzListHits.getMethod("getMisses", (Class[])null);
      misses = (Map<String,Set<Integer>>) m.invoke(null, (Object[])null); // unchecked cast
    }

    // Compute coverage
    Map<String,HashSet<Integer>> all = new HashMap<String,HashSet<Integer>>();
    Map<String,HashSet<Integer>> covered = new HashMap<String,HashSet<Integer>>();
    for (Map.Entry<String,Set<Integer>> entry : hits.entrySet()) {
      String file = entry.getKey();
      Set<Integer> lines = entry.getValue();
      if (! relevant.contains(file)) continue;
      if (! all.containsKey(file)) all.put(file, new HashSet<Integer>());
      all.get(file).addAll(lines);
      if (! covered.containsKey(file)) covered.put(file, new HashSet<Integer>());
      covered.get(file).addAll(lines);
    }
    for (Map.Entry<String,Set<Integer>> entry : misses.entrySet()) {
      String file = entry.getKey();
      Set<Integer> lines = entry.getValue();
      if (! relevant.contains(file)) continue;
      if (! all.containsKey(file)) all.put(file, new HashSet<Integer>());
      all.get(file).addAll(lines);
      // We choose to count partially-covered lines as fully covered,
      // instead of uncovered.  Uncomment below code to not count them.
      // if (! covered.containsKey(file)) covered.put(file, new HashSet<Integer>(0));
      // covered.get(file).removeAll(lines);
    }

    // Display results
    int nall_sum = 0;
    int ncov_sum = 0;
    for (String file : relevant) {
      if (all.get(file) == null) {
	System.out.println(file + " not instrumented (?)");
	continue;
      }
      int nall = all.get(file).size();
      int ncov = (covered.get(file) == null) ? 0 : covered.get(file).size();
      System.out.println(file + " covered on " + ncov + " of " + nall + " lines");
      nall_sum += nall;
      ncov_sum += ncov;
    }
    double pct = ((double) ncov_sum) / nall_sum;
    System.out.println("Total coverage "
		       + ncov_sum + " of " + nall_sum + " lines ==> "
		       + (new DecimalFormat("0.00").format(pct)) + " %");
  }

}
