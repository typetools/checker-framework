package daikon.chicory;

import checkers.quals.Interned;

import utilMDE.*;

import java.util.*;
import java.io.*;

/**
 * Reads dtrace files and provides methods to access the information
 * within them.  A dtrace file contains both declarations and data.
 */
public class DTraceReader extends DeclReader {

  public void read (File pathname) {

    try {

      MultiReader dtrace_file = new MultiReader (pathname, "^(//|#).*", null);

      for (String line = dtrace_file.readLine(); line != null;
           line = dtrace_file.readLine()) {

        // System.out.printf ("Reading line %s%n", line);

        // Skip comments
        if (line.trim().startsWith ("//") || line.trim().startsWith("#"))
          continue;

        // Skip blank lines
        if (line.trim().length() == 0)
          continue;

        // Read declarations
        if (line.equals ("DECLARE")) {
          read_decl (dtrace_file);
          continue;
        }

        // Read data entries
        read_data (line, dtrace_file);
      }
    } catch (Exception e) {
      throw new Error ("Error reading dtrace file " + pathname, e);
    }
  }

  /**
   * Reads data for one ppt from the trace file.  Adds the data to
   * the list of data for the ppt.
   */
  protected void read_data (String ppt_name, MultiReader dtrace_file)
    throws IOException {

    if (!ppt_name.contains (":::"))
      throw new Error ("unexpected program point name " + ppt_name + " at "
           + dtrace_file.get_filename() + ":" + dtrace_file.get_line_number());
    DeclPpt ppt = ppts.get (ppt_name);
    if (ppt == null)
      throw new Error ("ppt " + ppt_name + " not declared");

    List<DeclVarInfo> vars = ppt.get_all_vars();

    List</*@Interned*/ Object> var_data_list = new ArrayList</*@Interned*/ Object>();
    for (DeclVarInfo vi : vars) {
      /*@Interned*/ Object obj = vi.read_data (dtrace_file);
      var_data_list.add (obj);
    }

    ppt.add_var_data (var_data_list); // an interned warning here looks like an interned checker bug
  }

  /**
   * Dumps out each record of data for each ppt and variable
   */
  public void dump_data() {

    for (String ppt_name : ppts.keySet()) {
      System.out.printf ("Ppt: %s%n", ppt_name);
      DeclPpt ppt = ppts.get (ppt_name);
      List<DeclVarInfo> vis = ppt.get_all_vars();
      for (List</*@Interned*/ Object> var_data_list : ppt.get_var_data()) {
        System.out.println ("----------------------\n");
        for (int ii = 0; ii < vis.size(); ii++) {
          System.out.printf ("  %-20s: %s%n", vis.get(ii).name,
                             var_data_list.get(ii));
        }
      }
    }
  }

}
