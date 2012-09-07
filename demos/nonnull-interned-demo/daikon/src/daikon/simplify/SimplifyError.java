package daikon.simplify;

/**
 * Superclass of all runtime errors in this package.
 **/
public class SimplifyError
  extends RuntimeException
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public SimplifyError() { super(); }
  public SimplifyError(String s) { super(s); }
}
