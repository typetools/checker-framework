package daikon.derive.ternary;

import daikon.*;
import daikon.derive.*;

/**
 * Factory to produce TernaryDerivations.
 **/

public abstract class TernaryDerivationFactory implements DerivationFactory {

  /**
   * Create a set of derivations from three base variables.  If the
   * base variables aren't worth deriving from, returns null.
   * @param vi1
   * @param vi2
   * @param vi3 the three base variables.
   * @return a set of derivations based on three base variables.  We
   * allow more than one because the base variables may have multiple
   * derived variables, per type of derivation.  Can also be null if the
   * variables have nothing to derive from.
   **/
  public abstract TernaryDerivation[] instantiate(VarInfo vi1, VarInfo vi2,
                                                  VarInfo vi3);


  /**
   * Check if vi1 and vi2 can be a part of a VarInfo triple based on
   * their types.
   * @param vi1
   * @param vi2
   * @return a boolean which is true if vi1 and vi2 can belong to
   * a valid VarInfo triple. (false otherwise)
   */
  public static boolean checkType (VarInfo vi1, VarInfo vi2) {
    if ((vi1.rep_type == ProglangType.INT_ARRAY)
        && (vi2.rep_type == ProglangType.INT))
      return true;
    else if ((vi1.rep_type == ProglangType.INT)
             && (vi2.rep_type == ProglangType.INT_ARRAY))
      return true;
    else if ((vi1.rep_type == ProglangType.INT)
             && (vi2.rep_type == ProglangType.INT))
      return true;
    else
      return false;
  }

  /**
   * Check if vi1 and vi2 can be a part of a VarInfo triple based on
   * their types.
   * @param vi1
   * @param vi2
   * @return a ProglangType which is the type of VarInfo needed to make
   * a valid VarInfo triple out of vi1 and vi2 (false otherwise)
   */
  public static ProglangType returnType (VarInfo vi1, VarInfo vi2) {
    if ((vi1.rep_type == ProglangType.INT_ARRAY)
        && (vi2.rep_type == ProglangType.INT))
      return ProglangType.INT;
    else if ((vi1.rep_type == ProglangType.INT)
             && (vi2.rep_type == ProglangType.INT_ARRAY))
      return ProglangType.INT;
    else if ((vi1.rep_type == ProglangType.INT)
             && (vi2.rep_type == ProglangType.INT))
      return ProglangType.INT_ARRAY;
    else
      return null;
  }

  /**
   * Checks if two base variables are comparable to each other.
   *
   * @param vi1
   * @param vi2
   * @return a boolean which is true if vi1 and vi2 are comparable
   * (false otherwise).
   */
  public static boolean checkComparability(VarInfo vi1, VarInfo vi2) {
    return (vi1.indexCompatible(vi2));
  }
}
