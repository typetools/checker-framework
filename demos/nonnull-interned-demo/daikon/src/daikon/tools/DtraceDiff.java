// DtraceDiff.java

package daikon.tools;
import java.io.*;
import java.util.*;
import utilMDE.*;
import daikon.*;
import daikon.config.Configuration;
import java.util.regex.*;
import gnu.getopt.*;

/** This tool is used to find the differences between two dtrace files
 *  based on analysis of the files' content, rather than a straight textual
 *  comparison.
 */


public class DtraceDiff {

  private static String usage =
    UtilMDE.joinLines(
        "Usage: DtraceDiff [OPTION]... [DECLS1]... DTRACE1 [DECLS2]... DTRACE2",
	"DTRACE1 and DTRACE2 are the data trace files to be compared.",
	"You may optionally specify corresponding DECLS files for each one.",
	"If no DECLS file is specified, it is assumed that the declarations",
	"are included in the data trace file instead.",
	"OPTIONs are:",
	"  -h, --" + Daikon.help_SWITCH,
	"      Display this usage message",
	"  --" + Daikon.ppt_regexp_SWITCH,
	"      Only include ppts matching regexp",
	"  --" + Daikon.ppt_omit_regexp_SWITCH,
	"      Omit all ppts matching regexp",
	"  --" + Daikon.var_regexp_SWITCH,
	"      Only include variables matching regexp",
	"  --" + Daikon.var_omit_regexp_SWITCH,
	"      Omit all variables matching regexp",
	"  --" + Daikon.config_SWITCH,
	"      Specify a configuration file ",
	"  --" + Daikon.config_option_SWITCH,
	"      Specify a configuration option ",
	"See the Daikon manual for more information."
        );


  public static void main (String[] args) {
    try {
      mainHelper(args);
    } catch (daikon.Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than daikon.Daikon.TerminationMessage gets
    // propagated.  This simplifies debugging by showing the stack trace.
  }

  /**
   * This entry point is useful for testing.  It returns a boolean to indicate
   * return status instead of croaking with an error.
   **/

  public static boolean mainTester (String[] args) {
    try {
      mainHelper(args);
      return true;
    } catch (daikon.Daikon.TerminationMessage e) {
      return true;
    } catch (Error e) {
      // System.out.printf ("Diff encountered error " + e.getMessage());
      // e.printStackTrace();
      return false;
    }
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
    Set<File> declsfile1 = new HashSet<File>();
    String dtracefile1 = null;
    Set<File> declsfile2 = new HashSet<File>();
    String dtracefile2 = null;

    LongOpt[] longopts = new LongOpt[] {
      // Process only part of the trace file
      new LongOpt(Daikon.ppt_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.ppt_omit_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.var_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.var_omit_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      // Configuration options
      new LongOpt(Daikon.config_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.config_option_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
    };

    Getopt g = new Getopt("daikon.tools.DtraceDiff", args, "h:", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {

	// long option
      case 0:
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          throw new Daikon.TerminationMessage();
	} else if (Daikon.ppt_regexp_SWITCH.equals(option_name)) {
	  if (Daikon.ppt_regexp != null)
	    throw new Error("multiple --"
			    + Daikon.ppt_regexp_SWITCH
			    + " regular expressions supplied on command line");
	  try {
	    String regexp_string = g.getOptarg();
	    // System.out.println("Regexp = " + regexp_string);
	    Daikon.ppt_regexp = Pattern.compile(regexp_string);
	  } catch (Exception e) {
	    throw new Error(e);
	  }
	  break;
	} else if (Daikon.ppt_omit_regexp_SWITCH.equals(option_name)) {
	  if (Daikon.ppt_omit_regexp != null)
	    throw new Error("multiple --"
			    + Daikon.ppt_omit_regexp_SWITCH
			    + " regular expressions supplied on command line");
	  try {
	    String regexp_string = g.getOptarg();
	    // System.out.println("Regexp = " + regexp_string);
	    Daikon.ppt_omit_regexp = Pattern.compile(regexp_string);
	  } catch (Exception e) {
	    throw new Error(e);
	  }
	  break;
	} else if (Daikon.var_regexp_SWITCH.equals(option_name)) {
	  if (Daikon.var_regexp != null)
	    throw new Error("multiple --"
			    + Daikon.var_regexp_SWITCH
			    + " regular expressions supplied on command line");
	  try {
	    String regexp_string = g.getOptarg();
	    // System.out.println("Regexp = " + regexp_string);
	    Daikon.var_regexp = Pattern.compile(regexp_string);
	  } catch (Exception e) {
	    throw new Error(e);
	  }
	  break;
	} else if (Daikon.var_omit_regexp_SWITCH.equals(option_name)) {
	  if (Daikon.var_omit_regexp != null)
	    throw new Error("multiple --"
			    + Daikon.var_omit_regexp_SWITCH
			    + " regular expressions supplied on command line");
	  try {
	    String regexp_string = g.getOptarg();
	    // System.out.println("Regexp = " + regexp_string);
	    Daikon.var_omit_regexp = Pattern.compile(regexp_string);
	  } catch (Exception e) {
	    throw new Error(e);
	  }
	  break;
	} else if (Daikon.config_SWITCH.equals(option_name)) {
	  String config_file = g.getOptarg();
	  try {
	    InputStream stream =
	      new FileInputStream(config_file);
	    Configuration.getInstance().apply(stream);
	  } catch (IOException e) {
	    throw new RuntimeException("Could not open config file "
				       + config_file);
	  }
	  break;
	} else if (Daikon.config_option_SWITCH.equals(option_name)) {
	  String item = g.getOptarg();
	  Configuration.getInstance().apply(item);
	  break;
        } else {
          throw new RuntimeException("Unknown long option received: " +
                                     option_name);
        }

	//short options
      case 'h':
	System.out.println(usage);
	throw new Daikon.TerminationMessage();

      case '?':
        break; // getopt() already printed an error

      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    for (int i = g.getOptind(); i < args.length; i++) {
      if (args[i].indexOf(".decls") != -1) {
        if (dtracefile1 == null)
          declsfile1.add(new File(args[i]));
        else if (dtracefile2 == null)
          declsfile2.add(new File(args[i]));
        else
          throw new daikon.Daikon.TerminationMessage(usage);
      } else { // presume any other file is a dtrace file
        if (dtracefile1  == null)
          dtracefile1 = args[i];
        else if (dtracefile2 == null)
          dtracefile2 = args[i];
        else
          throw new daikon.Daikon.TerminationMessage(usage);
      }
    }
    if ((dtracefile1 == null) || (dtracefile2 == null))
      throw new daikon.Daikon.TerminationMessage(usage);
    dtraceDiff (declsfile1, dtracefile1, declsfile2, dtracefile2);
  }

  public static void dtraceDiff (Set<File> declsfile1,
				 String dtracefile1,
				 Set<File> declsfile2,
				 String dtracefile2) {

    FileIO.new_decl_format = false;

    try {
      Map<PptTopLevel,PptTopLevel> pptmap = new HashMap<PptTopLevel,PptTopLevel>();  // map ppts1 -> ppts2
      PptMap ppts1 = FileIO.read_declaration_files(declsfile1);
      PptMap ppts2 = FileIO.read_declaration_files(declsfile2);

      FileIO.ParseState state1 =
        new FileIO.ParseState (dtracefile1, false, true, ppts1);
      FileIO.ParseState state2 =
        new FileIO.ParseState (dtracefile2, false, true, ppts2);

      while (true) {
	// *** should do some kind of progress bar here?
	// read from dtracefile1 until we get a data trace record or EOF
	while (true) {
	  FileIO.read_data_trace_record (state1);
	  if (state1.status == FileIO.ParseStatus.SAMPLE)
	    break;
	  else if ((state1.status == FileIO.ParseStatus.EOF)
		   || (state1.status == FileIO.ParseStatus.TRUNCATED))
	    break;
	}
	// read from dtracefile2 until we get a data trace record or EOF
	while (true) {
	  FileIO.read_data_trace_record (state2);
	  if (state2.status == FileIO.ParseStatus.SAMPLE)
	    break;
	  else if ((state2.status == FileIO.ParseStatus.EOF)
		   || (state2.status == FileIO.ParseStatus.TRUNCATED))
	    break;
	}
	// things had better be the same
	if (state1.status == state2.status) {
	  if (state1.status == FileIO.ParseStatus.SAMPLE) {
	    PptTopLevel ppt1 = state1.ppt;
	    PptTopLevel ppt2 = state2.ppt;
	    ValueTuple vt1 = state1.vt;
	    ValueTuple vt2 = state2.vt;
	    VarInfo[] vis1 = ppt1.var_infos;
	    VarInfo[] vis2 = ppt2.var_infos;

	    // Check to see that Ppts match the first time we encounter them
	    PptTopLevel foundppt = pptmap.get(ppt1);
	    if (foundppt == null) {
	      if (!ppt1.name.equals(ppt2.name))
		ppt_mismatch_error (state1, dtracefile1, state2, dtracefile2);
	      for (int i = 0;
		   (i < ppt1.num_tracevars) && (i < ppt2.num_tracevars);
		   i++) {
		// *** what about comparability and aux info?
		if ((!vis1[i].name().equals(vis2[i].name()))
		    || (vis1[i].is_static_constant != vis2[i].is_static_constant)
		    || ((vis1[i].is_static_constant) &&
			!values_are_equal (vis1[i],
					   vis1[i].constantValue(),
					   vis2[i].constantValue()))
		    || ((vis1[i].type != vis2[i].type) ||
			(vis1[i].file_rep_type != vis2[i].file_rep_type)))
		  ppt_var_decl_error (vis1[i], state1, dtracefile1,
				      vis2[i], state2, dtracefile2);
	      }
	      if (ppt1.num_tracevars != ppt2.num_tracevars)
		ppt_decl_error (state1, dtracefile1, state2, dtracefile2);
	      pptmap.put(ppt1, ppt2);
	    } else if (foundppt != ppt2) {
	      ppt_mismatch_error (state1, dtracefile1, state2, dtracefile2);
	    }

	    // check to see that variables on this pair of samples match
	    for (int i = 0; i < ppt1.num_tracevars; i++) {
	      if (vis1[i].is_static_constant)
		continue;
	      boolean missing1 = vt1.isMissingNonsensical(vis1[i]);
	      boolean missing2 = vt2.isMissingNonsensical(vis2[i]);
	      Object val1 = vt1.getValue(vis1[i]);
	      Object val2 = vt2.getValue(vis2[i]);
	      if ((missing1 != missing2)
		  || (! (missing1 || values_are_equal(vis1[i], val1, val2))))
		ppt_var_value_error (vis1[i], val1, state1, dtracefile1,
				     vis2[i], val2, state2, dtracefile2);
	    }
	  }
	  else
	    return;  // EOF on both files ==> normal return
	}
	else if ((state1.status == FileIO.ParseStatus.TRUNCATED)
		 || (state1.status == FileIO.ParseStatus.TRUNCATED))
	  return;  // either file reached truncation limit, return quietly
	else if (state1.status == FileIO.ParseStatus.EOF) {
	  throw new Error(String.format ("ppt %s (%s at line %d) is missing "
                                    + "at end of %s", state2.ppt.name(),
                                    dtracefile2, state2.lineNum, dtracefile1));
	} else {
	  throw new Error(String.format ("ppt %s (%s at line %d) is missing "
                                    + "at end of %s", state1.ppt.name(),
                                    dtracefile1, state1.lineNum, dtracefile2));
    }
      }
    } catch (IOException e) {
      System.out.println();
      e.printStackTrace();
      throw new Error(e);
    }
  }

  private static boolean values_are_equal (VarInfo vi,
					   Object val1,
					   Object val2) {
    ProglangType type = vi.file_rep_type;
    if (type.isArray ()) {
      // array case
      if (type.isPointerFileRep()) {
	long[] v1 = (long[])val1;
	long[] v2 = (long[])val2;
	if (v1.length != v2.length)
	  return false;
	for (int i = 0; i<v1.length; i++)
	  if (((v1[i] == 0) || (v2[i] == 0)) && (v1[i] != v2[i]))
	    return false;
	return true;
      }
      else if (type.baseIsScalar()) {
	long[] v1 = (long[])val1;
	long[] v2 = (long[])val2;
	if (v1.length != v2.length)
	  return false;
	for (int i = 0; i<v1.length; i++)
	  if (v1[i] != v2[i])
	    return false;
	return true;
      }
      else if (type.baseIsFloat()) {
	double[] v1 = (double[])val1;
	double[] v2 = (double[])val2;
	if (v1.length != v2.length)
	  return false;
	for (int i = 0; i<v1.length; i++)
          if (!((Double.isNaN(v1[i]) && Double.isNaN(v2[i]))
                || Global.fuzzy.eq(v1[i], v2[i])))
            return false;
	return true;
      }
      else if (type.baseIsString()) {
	String[] v1 = (String[])val1;
	String[] v2 = (String[])val2;
	if (v1.length != v2.length)
	  return false;
	for (int i = 0; i<v1.length; i++)
	  {
	    if ((v1[i] == null) && (v2[i] == null))
	      ;
	    else if ((v1[i] == null) || (v2[i] == null))
	      return false;
	    else if (!v1[i].equals(v2[i]))
	      return false;
	  }
	return true;
      }
    } else {
      // scalar case
      if (type.isPointerFileRep()) {
	Long v1 = ((Long)val1).longValue();
	Long v2 = ((Long)val2).longValue();
	return !(((v1 == 0) || (v2 == 0)) && (v1 != v2));
      }
      else if (type.isScalar())
	return (((Long)val1).longValue() == ((Long)val2).longValue());
      else if (type.isFloat()) {
        double d1 = ((Double)val1).doubleValue();
        double d2 = ((Double)val2).doubleValue();
	return ((Double.isNaN(d1) && Double.isNaN(d2))
                || Global.fuzzy.eq (d1, d2));
      } else if (type.isString())
	return (((String)val1).equals((String)val2));
    }
    throw new Error ("Unexpected value type found");  // should never happen
  }

  private static void ppt_mismatch_error (FileIO.ParseState state1,
					  String dtracefile1,
					  FileIO.ParseState state2,
					  String dtracefile2) {
    throw new Error
      (String.format("Mismatched program point:%n"
                     + "  ppt %s at %s:%d%n"
                     + "  ppt %s at %s:%d",
                     state1.ppt.name, dtracefile1, state1.lineNum,
                     state2.ppt.name, dtracefile2, state2.lineNum));
  }

  private static void ppt_decl_error (FileIO.ParseState state1,
				      String dtracefile1,
				      FileIO.ParseState state2,
				      String dtracefile2) {
    throw new Error
      (String.format("Mismatched program point declaration:%n"
                     + "  ppt %s at %s:%d%n"
                     + "  ppt %s at %s:%d",
                     state1.ppt.name, dtracefile1, state1.lineNum,
                     state2.ppt.name, dtracefile2, state2.lineNum));
  }

  private static void ppt_var_decl_error (VarInfo vi1,
					  FileIO.ParseState state1,
					  String dtracefile1,
					  VarInfo vi2,
					  FileIO.ParseState state2,
					  String dtracefile2) {
    assert state1.ppt.name.equals(state2.ppt.name);
    throw new Error
      (String.format("Mismatched variable declaration in program point %s:%n"
                     + "  variable %s at %s:%d%n"
                     + "  variable %s at %s:%d",
                     state1.ppt.name,
                     vi1.name(), dtracefile1, state1.lineNum,
                     vi2.name(), dtracefile2, state2.lineNum));
  }

  private static void ppt_var_value_error (VarInfo vi1,
                                           Object val1,
					   FileIO.ParseState state1,
					   String dtracefile1,
					   VarInfo vi2,
                                           Object val2,
					   FileIO.ParseState state2,
					   String dtracefile2) {
    assert vi1.name().equals(vi2.name());
    assert state1.ppt.name.equals(state2.ppt.name);
    throw new Error
      (String.format("Mismatched values for variable %s in program point %s:%n"
                     + "  value %s at %s:%d%n"
                     + "  value %s at %s:%d",
                     vi1.name(), state1.ppt.name,
                     val1, dtracefile1, state1.lineNum,
                     val2, dtracefile2, state2.lineNum));
  }


}
