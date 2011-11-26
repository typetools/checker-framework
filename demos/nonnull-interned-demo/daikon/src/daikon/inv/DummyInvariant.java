package daikon.inv;

import daikon.*;
import utilMDE.Assert;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This is a special invariant used internally by Daikon to represent
 * invariants whose meaning Daikon doesn't understand. The only
 * operation that can be performed on a DummyInvariant is to print it.
 * For instance, dummy invariants can be created to correspond to
 * splitting conditions, when no other invariant in Daikon's grammar
 * is equivalent to the condition.
 *
 * To use dummy invariants for splitting conditions, the configuration
 * option <samp>daikon.PptTopLevel.dummy_invariant_level</samp> must be set,
 * and formatting information must be supplied in the splitter info file.
 **/
public class DummyInvariant
  extends Invariant
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030220L;

  private String daikonFormat;
  private String javaFormat;
  private String escFormat;
  private String simplifyFormat;
  private String ioaFormat;
  private String jmlFormat;
  private String dbcFormat;

  private boolean negated = false;

  // Pre-instatiate(), set to true if we have reason to believe the user
  // explicitly wanted this invariant to appear in the output.
  // After instantiation, also requires that we've found an appropriate
  // slice for the invariant to live in.
  public boolean valid = false;

  public DummyInvariant(PptSlice ppt) {
    super(ppt);
  }

  public void setFormats(String daikonStr, String java, String esc,
                         String simplify, String ioa, String jml,
                         String dbc, boolean desired) {
    if (daikonStr != null)
      daikonFormat = daikonStr;
    if (java != null)
      javaFormat = java;
    if (esc != null)
      escFormat = esc;
    if (simplify != null)
      simplifyFormat = simplify;
    if (ioa != null)
      ioaFormat = ioa;
    if (jml != null)
      jmlFormat = jml;
    if (dbc != null)
      dbcFormat = dbc;

    valid |= desired;
  }

  public DummyInvariant instantiate(PptTopLevel parent, VarInfo[] vars) {
    DummyInvariant inv = new DummyInvariant(ppt);
    inv.daikonFormat = this.daikonFormat;
    inv.javaFormat = this.javaFormat;
    inv.escFormat = this.escFormat;
    inv.simplifyFormat = this.simplifyFormat;
    inv.ioaFormat = this.ioaFormat;
    inv.valid = false; // Not valid until we find a slice for it
    Assert.assertTrue(!this.negated, "Only instantiated invariants " +
                      "should be negated");

    // Find between 1 and 3 unique variables, to pick a slice to put
    // this in.
    HashSet<VarInfo> uniqVarsSet = new HashSet<VarInfo>();
    for (int i = 0; i < vars.length; i++)
      uniqVarsSet.add(vars[i].canonicalRep());
    int sliceSize = uniqVarsSet.size();
    if (sliceSize > 3)
      sliceSize = 3;
    VarInfo[] newVars = new VarInfo[sliceSize];
    {
      Iterator<VarInfo> it = uniqVarsSet.iterator();
      int i = 0;
      while (it.hasNext()) {
        newVars[i++] = it.next();
        if (i == sliceSize)
          break;
      }
    }
    vars = newVars;
    Assert.assertTrue(vars.length >= 1 && vars.length <= 3);
    if (vars.length == 1) {
      PptSlice1 slice = parent.findSlice(vars[0]);
      if (slice == null) {
        slice = new PptSlice1(parent, vars);
        parent.addSlice(slice);
      }
      inv.ppt = slice;
    } else if (vars.length == 2) {
      if (vars[0] == vars[1])
        return inv;
      else if (vars[0].varinfo_index > vars[1].varinfo_index) {
        VarInfo tmp = vars[0];
        vars[0] = vars[1];
        vars[1] = tmp;
      }
      PptSlice2 slice = parent.findSlice(vars[0], vars[1]);
      if (slice == null) {
        slice = new PptSlice2(parent, vars);
        parent.addSlice(slice);
      }
      inv.ppt = slice;
    } else if (vars.length == 3) {
      if (vars[0] == vars[1] || vars[1] == vars[2] || vars[0] == vars[2])
        return inv;
      // bubble sort
      VarInfo tmp;
      if (vars[0].varinfo_index > vars[1].varinfo_index) {
        tmp = vars[0]; vars[0] = vars[1]; vars[1] = tmp;
      }
      if (vars[1].varinfo_index > vars[2].varinfo_index) {
        tmp = vars[1]; vars[1] = vars[2]; vars[2] = tmp;
      }
      if (vars[0].varinfo_index > vars[1].varinfo_index) {
        tmp = vars[0]; vars[0] = vars[1]; vars[1] = tmp;
      }
      PptSlice3 slice = parent.findSlice(vars[0], vars[1], vars[2]);
      if (slice == null) {
        slice = new PptSlice3(parent, vars);
        parent.addSlice(slice);
      }
      inv.ppt = slice;
    }
    inv.valid = this.valid;
    return inv;
  }

  protected double computeConfidence() {
    return Invariant.CONFIDENCE_JUSTIFIED;
  }

  public void negate() {
    negated = !negated;
  }

  public String format_using(OutputFormat format) {
    if (format == OutputFormat.DAIKON) return format_daikon();
    if (format == OutputFormat.IOA) return format_ioa();
    if (format == OutputFormat.JAVA) return format_java();
    if (format == OutputFormat.ESCJAVA) return format_esc();
    if (format == OutputFormat.SIMPLIFY) return format_simplify();
    if (format == OutputFormat.JML) return format_jml();
    if (format == OutputFormat.DBCJAVA) return format_dbc();

    return format_unimplemented(format);
  }

  public String format_daikon() {
    String df;
    if (daikonFormat == null)
      df = "<dummy>";
    else
      df = daikonFormat;
    if (negated)
      return "not " + df;
    else
      return df;
  }

  public String format_java() {
    if (javaFormat == null)
      return "format_java not implemented for dummy invariant";
    if (negated)
      return "!(" + javaFormat + ")";
    else
      return javaFormat;
  }

  public String format_esc() {
    if (escFormat == null)
      return "format_esc not implemented for dummy invariant";
    if (negated)
      return "!(" + escFormat + ")";
    else
      return escFormat;
  }

  public String format_simplify() {
    if (simplifyFormat == null)
      return "format_simplify not implemented for dummy invariant";
    if (negated)
      return "(NOT " + simplifyFormat + ")";
    else
      return simplifyFormat;
  }

  public String format_ioa() {
    if (ioaFormat == null)
      return "format_ioa not implemented for dummy invariant";
    if (negated)
      return "~(" + ioaFormat + ")";
    else
      return ioaFormat;
  }

  public String format_jml() {
    if (jmlFormat == null)
      return "format_jml not implemented for dummy invariant";
    if (negated)
      return "!(" + jmlFormat + ")";
    else
      return jmlFormat;
  }

  public String format_dbc() {
    if (dbcFormat == null)
      return "format_dbc not implemented for dummy invariant";
    if (negated)
      return "!(" + dbcFormat + ")";
    else
      return dbcFormat;
  }

  protected Invariant resurrect_done(int[] permutation) {
    throw new Error("Not implemented");
  }
}
