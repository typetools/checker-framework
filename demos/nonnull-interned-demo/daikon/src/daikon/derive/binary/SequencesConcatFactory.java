package daikon.derive.binary;

import daikon.*;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Factory for SequencesConcat derived variables.
 **/

public final class SequencesConcatFactory extends BinaryDerivationFactory {

  /**
   * Debug tracer.
   **/
  public static final Logger debug =
    Logger.getLogger("daikon.derive.binary.SequencesConcatFactory");

  public BinaryDerivation[] instantiate(VarInfo var1, VarInfo var2) {

    boolean enabled = SequencesConcat.dkconfig_enabled;
    if (!enabled) return null;

    if (var1.type != var2.type || var1.rep_type != var2.rep_type) {
      // Is this really necessary since we're checking comparability?
      return null;
    }

    if (var1.rep_type != ProglangType.INT_ARRAY &&
        var1.rep_type != ProglangType.STRING_ARRAY &&
        var1.rep_type != ProglangType.DOUBLE_ARRAY) {
      return null;
    }

    if (!VarComparability.comparable (var1, var2)) return null;

    if (var1.derived != null || var2.derived != null) {
      // From derived variables.  Don't derive.
      return null;
    }

    // We don't want concats of arrays with themselves
    if (var1.name().equals (var2.name())) return null;

    if (debug.isLoggable(Level.FINE)) {
      debug.fine (var1.ppt + ": " + var1.name() + " and " + var2.name()
                  + " are worth deriving from");
      debug.fine ("Types are: " + var1.type + " " + var2.type);
      debug.fine ("Comparabilities are: " + var1.comparability + " "
                  + var2.comparability);
    }

    return new BinaryDerivation[] {
      new SequencesConcat (var1, var2),
    };
  }

}
