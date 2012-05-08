package daikon.split;

import daikon.*;
import daikon.inv.*;

// This splitter tests the condition "X>0".
public final class SplitterExample
  extends Splitter
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030218L;

  static DummyInvariant dummyInvFactory;
  DummyInvariant dummyInv;

  VarInfo x_varinfo;

  public SplitterExample() {
  }

  public SplitterExample(Ppt ppt) {
    x_varinfo = ppt.find_var_by_name ("X");
    instantiated = true;
  }

  public Splitter instantiate(Ppt ppt) {
    return new SplitterExample(ppt);
  }

  public boolean valid() {
    return (x_varinfo != null);
  }

  public boolean test(ValueTuple vt) {
    // Alternately, if x represents an array, use
    //   vt.getIntArrayValue(x_varinfo);
    return (x_varinfo.getIntValue(vt) > 0);
  }

  public String condition() {
    return "X > 0";
  }

  public void makeDummyInvariant(DummyInvariant inv) {
    assert dummyInvFactory == null;
    dummyInvFactory = inv;
  }

  public void instantiateDummy(PptTopLevel ppt) {
    dummyInv = null;
    VarInfo x_vi = ppt.find_var_by_name ("X");
    if (x_vi != null) {
      dummyInv = dummyInvFactory.instantiate(ppt, new VarInfo[] { x_vi });
    }
  }

  public DummyInvariant getDummyInvariant() {
    return dummyInv;
  }
}
