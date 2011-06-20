package daikon.split;

import java.util.*;
import daikon.*;
import utilMDE.*;
import java.util.logging.Level;

// SplitterList maps from a program point name to an array of Splitter
// objects that should be used when splitting that program point.
// Invariant:  each of those splitters should be non-instantiated (each is
// a factory, not an instantiated splitter).
// It's a shame to have to hard-code for each program point name.

public abstract class SplitterList
{
  // This causes problems right now, probably due to classloading
  // before logs are configured.
  // /**
  //  * Debug tracer
  //  **/
  // public static final Logger debug = Logger.getLogger("daikon.split.SplitterList");

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  // "@ref{}" produces a cross-reference in the printed manual.  It must
  // *not* come at the beginning of a line, or Javadoc will get confused.
  /**
   * Boolean.  Enables indiscriminate splitting
   * (see Daikon manual, @ref{Indiscriminate splitting},
   * for an explanation of this technique).
   **/
  public static boolean dkconfig_all_splitters = true;

  private static final HashMap<String,Splitter[]> ppt_splitters = new HashMap<String,Splitter[]>();

  /**
   * Associate an array of splitters with the program point pptname.
   */
  public static void put(String pptname, Splitter[] splits) {
    // for (int i=0; i<splits.length; i++) {
    //   Assert.assertTrue(splits[i].instantiated() == false);
    // }

    if ((Global.debugSplit != null) && Global.debugSplit.isLoggable(Level.FINE)) {
      String[] splits_strings = new String[splits.length];
      for (int i=0; i<splits.length; i++) {
        splits_strings[i] = splits[i].condition();
      }
      Global.debugSplit.fine ("Registering splitters for " + pptname + ":"
                              + utilMDE.ArraysMDE.toString(splits_strings));
    }

    if (ppt_splitters.containsKey(pptname)) {
      Splitter[] old = ppt_splitters.get(pptname);
      Splitter[] new_splits = new Splitter[old.length + splits.length];
      System.arraycopy(old, 0, new_splits, 0, old.length);
      System.arraycopy(splits, 0, new_splits, old.length, splits.length);
      ppt_splitters.put(pptname, new_splits);
    } else {
      Assert.assertTrue(! ppt_splitters.containsKey(pptname));
      // Assert.assertTrue(! ppt_splitters.containsKey(pptname),
      //               "SplitterList already contains " + pptname
      //               + " which maps to" + lineSep + " " + ArraysMDE.toString(get_raw(pptname))
      //               + lineSep + " which is " + formatSplitters(get_raw(pptname)));
      ppt_splitters.put(pptname, splits);
    }
  }

  // This is only used by the debugging output in SplitterList.put().
  public static String formatSplitters(Splitter[] splits) {
    if (splits == null)
      return "null";
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (int i=0; i<splits.length; i++) {
      if (i != 0)
        sb.append(", ");
      sb.append("\"");
      sb.append(splits[i].condition());
      sb.append("\"");
    }
    sb.append("]");
    return sb.toString();
  }

  public static Splitter[] get_raw(String pptname) {
    return ppt_splitters.get(pptname);
  }

  //   // This returns a list of all the splitters that are applicable to the
  //   // program point named "name".  The list is constructed by looking up
  //   // various parts of "name" in the SplitterList hashtable.
  //
  //   // This routine tries the name first, then the base of the name, then the
  //   // class, then the empty string.  For instance, if the program point name is
  //   // "Foo.bar(IZ)V:::EXIT2", then it tries, in order:
  //   //   "Foo.bar(IZ)V:::EXIT2"
  //   //   "Foo.bar(IZ)V"
  //   //   "Foo.bar"
  //   //   "Foo"
  //   //   ""
  //
  //   public static Splitter[] get(String pptName) {
  //     String pptName_ = pptName;        // debugging
  //     Splitter[] result;
  //     Vector splitterArrays = new Vector();
  //     Vector splitters = new Vector();
  //
  //     result = get_raw(pptName);
  //     if (result != null)
  //       splitterArrays.addElement(result);
  //
  //     {
  //       int tag_index = pptName.indexOf(FileIO.ppt_tag_separator);
  //       if (tag_index != -1) {
  //         pptName = pptName.substring(0, tag_index);
  //         result = get_raw(pptName);
  //         if (result != null)
  //           splitterArrays.addElement(result);
  //       }
  //     }
  //
  //     int lparen_index = pptName.indexOf('(');
  //     {
  //       if (lparen_index != -1) {
  //         pptName = pptName.substring(0, lparen_index);
  //         result = get_raw(pptName);
  //         if (result != null)
  //           splitterArrays.addElement(result);
  //       }
  //     }
  //     {
  //       // The class pptName runs up to the last dot before any open parenthesis.
  //       int dot_limit = (lparen_index == -1) ? pptName.length() : lparen_index;
  //       int dot_index = pptName.lastIndexOf('.', dot_limit - 1);
  //       if (dot_index != -1) {
  //         pptName = pptName.substring(0, dot_index);
  //         result = get_raw(pptName);
  //         if (result != null)
  //           splitterArrays.addElement(result);
  //       }
  //     }
  //
  //     // Empty string means always applicable.
  //     result = get_raw("");
  //     if (result != null)
  //       splitterArrays.addElement(result);
  //
  //     if (splitterArrays.size() == 0) {
  //       if (Global.debugPptSplit) {
  //         System.out.println("SplitterList.get found no splitters for " + pptName);
  //         return null;
  //       }
  //     } else {
  //       int counter = 0;
  //       for (int i = 0; i < splitterArrays.size(); i++) {
  //         Splitter[] tempsplitters = (Splitter[])splitterArrays.elementAt(i);
  //         for (int j = 0; j < tempsplitters.length; j++) {
  //           splitters.addElement(tempsplitters[j]);
  //           counter++;
  //         }
  //       }
  //       if (Global.debugPptSplit)
  //         System.out.println("SplitterList.get found " + counter + " splitters for " + pptName);
  //     }
  //     return (Splitter[])splitters.toArray(new Splitter[0]);
  //   }
  //////////////////////

  /**
   * Return the splitters associated with this program point name.
   * The resulting splitters are factories, not instantiated splitters.
   * @param pptName
   * @return an array of splitters
   */
  public static Splitter[] get(String pptName) {
    Vector<Splitter[]> splitterArrays = new Vector<Splitter[]>();

    for (String name : ppt_splitters.keySet()) {
      // name is a ppt name, assumed to begin with "ClassName.functionName"
      if (pptName.indexOf(name) != -1) {
        Splitter[] result = get_raw(name);
        if (result != null) {
          splitterArrays.addElement(result);
        }
        // For the OBJECT program point, we want to use all the splitters.
      } else if ((pptName.indexOf("OBJECT") != -1)
                 && (name.indexOf("OBJECT") != -1)) {
        for (Splitter[] sa : ppt_splitters.values()) {
          splitterArrays.addElement(sa);
        }
      }
    }

    if (splitterArrays.size() == 0) {
      if (Global.debugSplit.isLoggable(Level.FINE)) {
        Global.debugSplit.fine ("SplitterList.get found no splitters for " + pptName);
      }
      return null;
    } else {
      Vector<Splitter> splitters = new Vector<Splitter>();
      for (Splitter[] tempsplitters : splitterArrays) {
        for (int j = 0; j < tempsplitters.length; j++) {
          splitters.addElement(tempsplitters[j]);
        }
      }
      if (Global.debugSplit.isLoggable(Level.FINE)) {
        Global.debugSplit.fine ("SplitterList.get found " + splitters.size() + " splitters for " + pptName);
      }
      return splitters.toArray(new Splitter[0]);
    }
  }

  /**
   * Return all the splitters in this program,
   * The resulting splitters are factories, not instantiated splitters.
   * @return an array of splitters
   */
  public static Splitter[] get_all( ) {
    Vector<Splitter> splitters = new Vector<Splitter>();
    for (Splitter[] splitter_array : ppt_splitters.values()) {
      for (int i = 0; i < splitter_array.length; i++) {
        Splitter tempsplitter = splitter_array[i];
        int j = 0; boolean duplicate = false;
        // Weed out splitters with the same condition.
        if (!splitters.isEmpty()) {
          for (j = 0; j < splitters.size(); j++) {
            if ((tempsplitter.condition().trim()).equals( splitters.elementAt(j).condition().trim())) {
              // System.err.println(" duplicate " + tempsplitter.condition()); System.err.println();
              duplicate = true;
              break;
            }
          }
        }
        if (!duplicate) {
          splitters.addElement(tempsplitter);
        }
      }
    }
    return splitters.toArray(new Splitter[0]);
  }

}
