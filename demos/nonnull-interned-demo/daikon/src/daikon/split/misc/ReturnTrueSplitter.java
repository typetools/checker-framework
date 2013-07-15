package daikon.split.misc;

import daikon.*;
import daikon.inv.DummyInvariant;
import daikon.split.*;

// This splitter tests the condition "return == true".
public final class ReturnTrueSplitter
  extends Splitter
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  VarInfo return_varinfo;

  public ReturnTrueSplitter() {
  }

  public ReturnTrueSplitter(Ppt ppt) {
    return_varinfo = ppt.find_var_by_name ("return");
    instantiated = true;
  }

  public Splitter instantiate(Ppt ppt) {
    return new ReturnTrueSplitter(ppt);
  }

  public boolean valid() {
    return ((return_varinfo != null)
            && (return_varinfo.type == ProglangType.BOOLEAN));
  }

  public boolean test(ValueTuple vt) {
    return (return_varinfo.getIntValue(vt) != 0);
  }

  public String condition() {
    return "return == true";
  }

  public DummyInvariant getDummyInvariant() {
    return null;
  }
}
