package daikon;

import daikon.inv.*;

import utilMDE.*;

import java.util.*;


// This is a fake PptSlice for use with Implication invariants.

// - The implication invariants at a program point are grouped into a
// single PptSlice0 with no variables

// - In order to output pre-state invariants as if they were
// post-state, or OBJECT invariants as if they applied to a particular
// parameter, we construct a PptSlice0 whose VarInfos have had their
// names tweaked, and temporarily use that as the invariant's ppt.

public class PptSlice0
  extends PptSlice
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  PptSlice0(PptTopLevel parent) {
     super(parent, new VarInfo[0]);
  }

  public final int arity() {
    return 0;
  }

  // Make a fake slice whose variables are the same as the ones in
  // sliceTemplate, but marked as prestate (i.e., orig(x) rather than x).
  public static PptSlice makeFakePrestate(PptSlice sliceTemplate) {
    PptSlice0 fake = new PptSlice0(sliceTemplate.parent);
    fake.var_infos = new VarInfo[sliceTemplate.var_infos.length];
    for (int i=0; i < fake.var_infos.length; i++) {
      fake.var_infos[i] = VarInfo.origVarInfo(sliceTemplate.var_infos[i]);
    }
    return fake;
  }


  // We trade space for time by keeping a hash table of all the
  // implications (they're also stored as a vector in invs) so we can
  // efficiently avoid adding implications more than once.

  // This should not be transient:  more implications can be created during
  // printing, for instance due to guarding.
  private transient HashSet<ImplicationWrapper> invariantsSeen = new HashSet<ImplicationWrapper>();

  // In lieu of a readResolve method.
  private void initInvariantsSeen() {
    if (invariantsSeen == null) {
      invariantsSeen = new HashSet<ImplicationWrapper>();
      for (Invariant inv : invs) {
        invariantsSeen.add(new ImplicationWrapper((Implication) inv));
      }
    }
  }

  public void checkRep() {
    if (invariantsSeen != null && invs.size() != invariantsSeen.size()) {
      Assert.assertTrue(invs.size() == invariantsSeen.size(),
                        "invs.size()=" + invs.size() + ", invariantsSeen.size()=" + invariantsSeen.size());
    }
    Assert.assertTrue(invariantsSeen == null || invs.size() == invariantsSeen.size());
  }

  /**
   * The invariant is typically an Implication; but PptSlice0 can contain
   * other joiners than implications, such as "and" or "or".  That feature
   * isn't used as of November 2003.
   **/
  public void addInvariant(Invariant inv) {
    Assert.assertTrue(inv != null);
    Assert.assertTrue(inv instanceof Implication);
    // checkRep();
    // Assert.assertTrue(! hasImplication((Implication) inv));
    initInvariantsSeen();
    invs.add(inv);
    invariantsSeen.add(new ImplicationWrapper((Implication)inv));
    // checkRep();
  }

  public void removeInvariant(Invariant inv) {
    Assert.assertTrue(inv != null);
    Assert.assertTrue(inv instanceof Implication);
    // checkRep();
    // Assert.assertTrue(hasImplication((Implication) inv));
    initInvariantsSeen();
    invs.remove(inv);
    invariantsSeen.remove(new ImplicationWrapper((Implication)inv));
    // checkRep();
  }

  // This can be called with very long lists by the conditionals code.
  // At least until that's fixed, it's important for it not to be
  // quadratic.
  public void removeInvariants(List<Invariant> to_remove) {
    if (to_remove.size() < 10) {
      for (Invariant trinv : to_remove) {
        removeInvariant(trinv);
      }
    } else {
      invs.removeMany(to_remove);
      if (to_remove.size() > invariantsSeen.size() / 2) {
        // Faster to throw away and recreate
        invariantsSeen = null;
        initInvariantsSeen();
      } else {
        // Faster to update
        for (Invariant trinv : to_remove) {
          invariantsSeen.remove(new
              ImplicationWrapper((Implication)trinv));
        }
      }
    }
  }

  public boolean hasImplication(Implication imp) {
    initInvariantsSeen();
    return invariantsSeen.contains(new ImplicationWrapper(imp));
  }

  // // For debugging only
  // public Implication getImplication(Implication imp) {
  //   initInvariantsSeen();
  //   ImplicationWrapper resultWrapper
  //     = (ImplicationWrapper) UtilMDE.getFromSet(
  //              invariantsSeen, new ImplicationWrapper(imp));
  //   if (resultWrapper == null) {
  //     return null;
  //   }
  //   return (Implication) resultWrapper.theImp;
  // }


  // We'd like to use a more sophisticated equality check and hashCode
  // for implications when they appear in the invariantsSeen HashSet,
  // but not anywhere else, so we make wrapper objects with the
  // desired methods to go directly in the set.

  // Not "implements serializable":  If this is serializable, then the hash
  // set tries to get the hash codes of all the invariants when it
  // reads them in, but their format methods croak when they couldn't
  // get their varInfos.

  private static final class ImplicationWrapper {

    public Implication theImp;
    // hashCode is cached to make equality checks faster.
    private int hashCode;

    public ImplicationWrapper(Implication theImp) {
      this.theImp = theImp;
      // this.format = theImp.format();
      this.hashCode = 0;
    }

    // Abstracted out to permit use of a cached value
    private String format() {
      // return format;
      return theImp.format();
      // return theImp.repr();
    }

    public int hashCode() {
      if (hashCode == 0) {
        hashCode = format().hashCode();
        // hashCode = (theImp.iff ? 1 : 0);
        // hashCode = 37 * hashCode + theImp.predicate().getClass().hashCode();
        // hashCode = 37 * hashCode + theImp.consequent().getClass().hashCode();
      }
      return hashCode;
    }

    // Returns the value of "isSameInvariant()".
    public boolean equals(Object o) {
      if (o == null)
        return false;
      Assert.assertTrue(o instanceof ImplicationWrapper);
      ImplicationWrapper other = (ImplicationWrapper)o;
      if (hashCode() != other.hashCode()) {
        return false;
      }
      boolean same_eq = theImp.isSameInvariant(other.theImp);

      // For debugging, look for differences between the format based check
      // and the isSameInvariant check.  Note that there are certain
      // invariants that print identically but are internally different:
      // "this.theArray[this.topOfStack..] ==
      // this.theArray[this.topOfStack..]" can be either SeqSeqIntEqual or
      // PairwiseLinearBinary, and "(return != null) ==> (return.getClass()
      // != this.theArray.getClass())" can be either an Implication or a
      // guarded invariant.
      if (false) {
        boolean fmt_eq = format().equals(other.format());
        if (! ((!same_eq) || fmt_eq)) {
          System.out.println ("imp1 = " + theImp.format());
          System.out.println ("imp2 = " + other.theImp.format());
          System.out.println ("fmt_eq = " + fmt_eq + " same_eq = " + same_eq);
          System.out.println ("lefteq = "
                             + theImp.left.isSameInvariant(other.theImp.left));
          System.out.println ("righteq = "
                           + theImp.right.isSameInvariant(other.theImp.right));
          System.out.println ("right class = "
                              + theImp.right.getClass() + "/"
                              + other.theImp.right.getClass());
          // Assert.assertTrue (false);
        }
        assert (!same_eq) || fmt_eq;
      }
      return same_eq;
    }

  }

  // I need to figure out how to set these.
  public int num_samples() { return 2222; }
  public int num_mod_samples() { return 2222; }
  public int num_values() { return 2222; }

  void instantiate_invariants() {
    throw new Error("Shouldn't get called");
  }

  public List<Invariant> add(ValueTuple vt, int count) {
    throw new Error("Shouldn't get called");
  }

}
