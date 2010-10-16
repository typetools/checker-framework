package daikon.derive.ternary;

import daikon.*;
import daikon.derive.*;

import utilMDE.*;

/**
 * Abstract class to represent a derived variable that came from
 * three base variables.
 **/

public abstract class TernaryDerivation
  extends Derivation
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  /**
   * Original variable 1.
   **/
  VarInfo base1;

  /**
   * Original variable 2.
   **/
  VarInfo base2;

  /**
   * Original variable 3.
   **/
  VarInfo base3;

  /**
   * Create a new TernaryDerivation from three varinfos.
   **/
  public TernaryDerivation(VarInfo vi1, VarInfo vi2, VarInfo vi3) {
    base1 = vi1;
    base2 = vi2;
    base3 = vi3;
  }

  public TernaryDerivation clone() {
    try {
      return (TernaryDerivation) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new Error("This can't happen", e);
    }
  }

  public VarInfo[] getBases() {
    return new VarInfo[] { base1, base2, base3 };
  }

  public Derivation switchVars(VarInfo[] old_vars, VarInfo[] new_vars) {
    TernaryDerivation result = this.clone();
    result.base1 = new_vars[ArraysMDE.indexOf(old_vars, result.base1)];
    result.base2 = new_vars[ArraysMDE.indexOf(old_vars, result.base2)];
    result.base3 = new_vars[ArraysMDE.indexOf(old_vars, result.base3)];
    return result;
  }

  public abstract ValueAndModified computeValueAndModified(ValueTuple full_vt);

  protected boolean isParam() {
    return (base1.isParam() || base2.isParam() || base3.isParam());
    // VIN
    // return (base1.aux.getFlag(VarInfoAux.IS_PARAM)
    //         || base2.aux.getFlag(VarInfoAux.IS_PARAM)
    //         || base3.aux.getFlag(VarInfoAux.IS_PARAM));
  }


  public int derivedDepth() {
    return 1 + Math.max(base1.derivedDepth(),
                        Math.max(base2.derivedDepth(), base3.derivedDepth()));
  }

  public boolean canBeMissing() {
    return base1.canBeMissing || base2.canBeMissing || base3.canBeMissing;
  }

  public boolean isDerivedFromNonCanonical() {
    // We insist that both are canonical, not just one.
    return !(base1.isCanonical() && base2.isCanonical()
             && base3.isCanonical());
  }

}
