package daikon.derive.binary;

import daikon.*;
import daikon.derive.*;

/**
 * Factory to produce BinaryDerivation.
 **/

public abstract class BinaryDerivationFactory implements DerivationFactory {

  /**
   * Create a set of derivations from two base variables.  If the base variables
   * aren't worth deriving from, returns null.
   * @param vi1
   * @param vi2 the two base variables.
   * @return a set of derivations based on two base variables.  We
   * allow more than one because two base variables may have multiple
   * derived variables, per type of derivation.  Can also be null if the
   * two variables have nothing to derive from.
   **/
  public abstract BinaryDerivation[] instantiate(VarInfo vi1, VarInfo vi2);

}
