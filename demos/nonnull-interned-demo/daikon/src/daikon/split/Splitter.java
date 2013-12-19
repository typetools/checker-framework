package daikon.split;

import daikon.*;
import daikon.inv.DummyInvariant;
import java.io.Serializable;

/**
 * A Splitter represents a test that can be used to separate all samples
 * into two parts.  For instance, a Splitter might represent the condition
 * "x > 0".  The Splitter is used to divide a collection of variable values
 * into sub-sets.  Invariant detection can then occur for the two subsets
 * independently.
 *
 * This class Splitter is the superclass for all the classes we
 * dynamically compile; there will be one subclass for each condition
 * that's checked. Other information about the splitting condition is
 * kept in a SplitterObject object, which keeps reference to the
 * corresponding Splitter. One instance of each Splitter subclass is
 * then created for each program point at which the splitting
 * condition is applicable.
 **/

// Should not be "implements Serializable":  the classes are created on
// demand, so the class doesn't exist when a serialized object is being
// re-read.
public abstract class Splitter
  implements Serializable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  /**
   * Creates a splitter "factory" that should only be used for creating new
   * copies via instantiate(Ppt).  (That is, the result of "new Splitter()"
   * should not do any splitting itself.)  There is no need for subclasses
   * to override this (but most will have to, since they will add
   * their own constructors as well).
   **/
  public Splitter() { }

  /**
   * Creates a valid splitter than can be used for testing the condition
   * via test(ValueTuple).  The implementation should always set
   * the "instantiated" protected field to true, if that field is present
   * in the Splitter class.
   */
  public abstract Splitter instantiate(Ppt ppt);

  protected boolean instantiated = false;
  /**
   * Returns true for an instantiated (non-"factory") splitter.
   * Clients also need to check valid().
   **/
  public boolean instantiated() {
    return instantiated;
  }

  /**
   * Returns true or false according to whether this was instantiated
   * correctly and test(ValueTuple) can be called without error.
   * An alternate design would have instantiate(Ppt) check this,
   * but it's a bit easier on implementers of subclasses of Splitter
   * for the work to be done (in just one place) by the caller.
   */
  public abstract boolean valid();

  /**
   * Returns true or false according to whether the values in the specified
   * ValueTuple satisfy the condition represented by this Splitter.
   */
  public abstract boolean test(ValueTuple vt);

  // This method could be static; but don't bother making it so.
  /** Returns the condition being tested, as a String. */
  public abstract String condition();

  /** Set up the static ('factory') DummyInvariant for this kind of
   * splitter. This only modifies static data, but it can't be static
   * because subclasses must override it. */
  public void makeDummyInvariant(DummyInvariant inv) { }

  /** Make an instance DummyInvariant for this instance of the
   * splitter, if possible on an appropriate slice from ppt. */
  public void instantiateDummy(PptTopLevel ppt) { }

  /** On an instantiated Splitter, give back an appropriate instantiated
   * DummyInvariant. */
  public abstract DummyInvariant getDummyInvariant();
}
