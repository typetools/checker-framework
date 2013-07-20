package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.inv.unary.UnaryInvariant;
import daikon.inv.binary.twoSequence.*;
import daikon.derive.binary.SequenceSubsequence;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Invariants on a single sequence.
 **/
public abstract class SingleSequence
  extends UnaryInvariant
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20031024L;

  /**
   * Boolean.  Set to true to disable all SeqIndex invariants
   * (SeqIndexIntEqual, SeqIndexFloatLessThan, etc).  This overrides the
   * settings of the individual SeqIndex enable configuration options.
   * To disable only some options, the options must be disabled
   * individually.
   */
  public static boolean dkconfig_SeqIndexDisableAll = false;

  protected SingleSequence(PptSlice ppt) {
    super(ppt);
    // System.out.println("Created SingleSequence invariant " + this + " at " + ppt);
  }

  public VarInfo var() {
    return ppt.var_infos[0];
  }


}
