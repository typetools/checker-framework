package daikon.derive.unary;

import daikon.*;
import daikon.derive.*;

import utilMDE.*;

public abstract class UnaryDerivation
  extends Derivation
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public VarInfo base;

  public UnaryDerivation(VarInfo vi) { base = vi; }

  public UnaryDerivation clone() {
    try {
      return (UnaryDerivation) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new Error("This can't happen", e);
    }
  }

  public Derivation switchVars(VarInfo[] old_vars, VarInfo[] new_vars) {
    UnaryDerivation result = this.clone();
    result.base = new_vars[ArraysMDE.indexOf(old_vars, result.base)];
    return result;
  }

  public ValueAndModified computeValueAndModified(ValueTuple vt) {
    int source_mod = base.getModified(vt);
    if (source_mod == ValueTuple.MISSING_NONSENSICAL)
      return ValueAndModified.MISSING_NONSENSICAL;
    if (source_mod == ValueTuple.MISSING_FLOW)
      return ValueAndModified.MISSING_FLOW;

    return computeValueAndModifiedImpl(vt);
  }

  /**
   * Actual implementation once mods are handled.
   **/
  protected abstract ValueAndModified computeValueAndModifiedImpl(ValueTuple vt);

  public VarInfo base() {
    return base;
  }

  public VarInfo[] getBases() {
    return new VarInfo[] { base() };
  }

  protected boolean isParam() {
    return base.isParam();
    // VIN
    // return base.aux.getFlag(VarInfoAux.IS_PARAM);
  }

  public boolean isDerivedFromNonCanonical() {
    return ! base.isCanonical();
  }

  public int derivedDepth() {
    return 1 + base.derivedDepth();
  }

  public boolean canBeMissing() {
    return base.canBeMissing;
  }
}
