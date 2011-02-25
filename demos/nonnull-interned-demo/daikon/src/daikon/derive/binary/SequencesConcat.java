package daikon.derive.binary;

import daikon.*;
import daikon.derive.*;

import utilMDE.*;

import java.util.logging.Logger;

/**
 * Represents the concatenation of two base variables.  This derived
 * variable works for both sequences of numbers and strings.
 **/

public final class SequencesConcat
  extends BinaryDerivation
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  /**
   * Debug tracer.
   **/
  public static final Logger debug = Logger.getLogger("daikon.derive.binary.SequencesConcat");

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff SequencesConcat derived variables should be created.
   **/
  public static boolean dkconfig_enabled = false;

  public VarInfo var1() { return base1; }
  public VarInfo var2() { return base2; }


  /**
   * Create a new SequenceScarlarConcat that represents the concatenation
   * of two base variables.
   * @param vi1 base variable 1
   * @param vi2 base variable 2
   **/
  public SequencesConcat (VarInfo vi1, VarInfo vi2) {
    super(vi1, vi2);
  }

  public ValueAndModified computeValueAndModifiedImpl(ValueTuple full_vt) {
    Object val1 = var1().getValue(full_vt);
    Object val2 = var2().getValue(full_vt);

    int mod = ValueTuple.UNMODIFIED;
    int mod1 = base1.getModified(full_vt);
    int mod2 = base2.getModified(full_vt);

    if (mod1 == ValueTuple.MODIFIED) mod = ValueTuple.MODIFIED;
    if (mod1 == ValueTuple.MISSING_NONSENSICAL) mod = ValueTuple.MISSING_NONSENSICAL;
    if (mod2 == ValueTuple.MODIFIED) mod = ValueTuple.MODIFIED;
    if (mod2 == ValueTuple.MISSING_NONSENSICAL) mod = ValueTuple.MISSING_NONSENSICAL;

    if (val1 == null && val2 == null) {
      return new ValueAndModified (null, mod);
    }
    if (var1().rep_type == ProglangType.INT_ARRAY) {
      // val1 instanceof long[] || val2 instanceof long[]
      long[] result = ArraysMDE.concat (val1 == null ? null : (long[]) val1,
                                        val2 == null ? null : (long[]) val2);
      return new ValueAndModified(Intern.intern(result), mod);
    } else if (var1().rep_type == ProglangType.DOUBLE_ARRAY) {
       double[] result = ArraysMDE.concat(val1 == null ? null : (double[]) val1,
                                        val2 == null ? null : (double[]) val2);
       return new ValueAndModified(Intern.intern(result), mod);

    } else if (var1().rep_type == ProglangType.STRING_ARRAY) {
      // val1 instanceof String[] || val2 instanceof String[]
      String[] result = ArraysMDE.concat (val1 == null ? null : (String[]) val1,
                                          val2 == null ? null : (String[]) val2);
      return new ValueAndModified(Intern.intern(result), mod);
    } else {
      throw new Error ("Attempted to concatenate unknown arrays");
    }

  }

  protected VarInfo makeVarInfo() {
    return VarInfo.make_function ("concat", var1(), var2());
  }

  public String toString() {
    return "[SequencesConcat of " + var1().name() + " " + var2().name() + "]";

  }

  public  boolean isSameFormula(Derivation other) {
    return (other instanceof SequencesConcat);
  }

  /** Returns the ESC name for sequence subsequence **/
  public String esc_name (String index) {
    return String.format ("SequencesConcat[%s,%s]", var1().esc_name(),
                          var2().esc_name());
  }

}
