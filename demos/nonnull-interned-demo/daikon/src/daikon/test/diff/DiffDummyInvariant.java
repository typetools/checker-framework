// A dummy invariant used for testing purposes

package daikon.test.diff;

import daikon.*;
import daikon.inv.*;

/**
 * A dummy invariant used for testing purposes.
 **/
public class DiffDummyInvariant
  extends Invariant
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public String formula;
  public double confidence;
  public boolean interesting;
  public boolean isWorthPrinting;

  public DiffDummyInvariant(PptSlice ppt, String formula, boolean justified) {
    this(ppt, formula, justified, true, true);
  }

  public DiffDummyInvariant(PptSlice ppt, String formula,
                        boolean justified, boolean interesting) {
    this(ppt, formula, justified, interesting, true);
  }

  public DiffDummyInvariant(PptSlice ppt, String formula,
                        boolean justified, boolean interesting,
                        boolean isWorthPrinting) {
    this(ppt, formula, (justified ? Invariant.CONFIDENCE_JUSTIFIED : Invariant.CONFIDENCE_UNJUSTIFIED), interesting, isWorthPrinting);
  }

  public DiffDummyInvariant(PptSlice ppt, String formula, double confidence) {
    this(ppt, formula, confidence, true, true);
  }

  public DiffDummyInvariant(PptSlice ppt, String formula,
                        double confidence, boolean interesting) {
    this(ppt, formula, confidence, interesting, true);
  }

  public DiffDummyInvariant(PptSlice ppt, String formula,
                        double confidence, boolean interesting,
                        boolean isWorthPrinting) {
    super(ppt);
    this.formula = formula;
    this.confidence = confidence;
    this.interesting = interesting;
    this.isWorthPrinting = isWorthPrinting;
  }

  protected Invariant resurrect_done(int[] permutation) {
    throw new UnsupportedOperationException();
  }

  public boolean isInteresting() {
    return interesting;
  }

  public boolean isSameInvariant(Invariant other) {
    return this.isSameFormula(other);
  }

  public boolean isSameFormula(Invariant other) {
    if (other instanceof DiffDummyInvariant) {
      DiffDummyInvariant o = (DiffDummyInvariant) other;
      return this.formula.equals(o.formula);
    } else {
      return false;
    }
  }

  public double computeConfidence() {
    return confidence;
  }

  public String repr() {
    return "DiffDummyInvariant(" + ppt.arity() + "," + formula + "," + confidence + ")";
  }

  public String format_using(OutputFormat format) {
    return repr();
  }

  // IsWorthPrinting should not be overridden by subclasses.
  // But this subclass is special:  it's not really an invariant,
  // but is only used for testing.
  public boolean isWorthPrinting() {
    return isWorthPrinting;
  }

}
