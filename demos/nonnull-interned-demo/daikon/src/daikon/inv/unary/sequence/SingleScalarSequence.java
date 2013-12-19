package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import utilMDE.*;

/**
 * Abstract base class used to evaluate single long sequences.
 **/

public abstract class SingleScalarSequence
  extends SingleSequence
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020813;

  protected SingleScalarSequence(PptSlice ppt) {
    super(ppt);
    // System.out.println("Created SingleScalarSequence invariant " + this + " at " + ppt);
  }

  /** Returns whether or not the specified types are valid  **/
  public final boolean valid_types (VarInfo[] vis) {
    return ((vis.length == 1)
            && vis[0].file_rep_type.baseIsScalar()
            && vis[0].file_rep_type.isArray());
  }

  public VarInfo var() {
    return ppt.var_infos[0];
  }

  // Should never be called with modified == ValueTuple.MISSING.
  // Subclasses need not override this except in special cases;
  // just implement @link{add_modified(Object,int)}.
  public InvariantStatus add(Object val, int mod_index, int count) {
    Assert.assertTrue(! falsified);
    Assert.assertTrue((mod_index >= 0) && (mod_index < 2));
    assert Intern.isInterned(val) : "not interned: " + val + "/"
      + val.getClass();
    Assert.assertTrue(Intern.isInterned(val));
    // System.out.println("SingleScalarSequence.add(" + ArraysMDE.toString(value) + ", " + modified + ", " + count + ")");
    long[] value = (long[]) val;
    if (value == null) {
      return InvariantStatus.NO_CHANGE;
    } else if (mod_index == 0) {
      return add_unmodified(value, count);
    } else {
      return add_modified(value, count);
    }
  }


  public InvariantStatus check(Object val, int mod_index, int count) {
    Assert.assertTrue(! falsified);
    Assert.assertTrue((mod_index >= 0) && (mod_index < 2));
    Assert.assertTrue(Intern.isInterned(val));
    long[] value = (long[]) val;
    if (value == null) {
      return InvariantStatus.NO_CHANGE;
    } else if (mod_index == 0) {
      return check_unmodified(value, count);
    } else {
      return check_modified(value, count);
    }
  }

  /**
   * This method need not check for falsified;
   * that is done by the caller.
   **/
  public abstract InvariantStatus add_modified(long[] value, int count);

  /**
   * By default, do nothing if the value hasn't been seen yet.
   * Subclasses can override this.
   **/
  public InvariantStatus add_unmodified(long[] value, int count) {
    return InvariantStatus.NO_CHANGE;
  }

  public abstract InvariantStatus check_modified(long[] value, int count);


  public InvariantStatus check_unmodified(long[] value, int count) {
    return InvariantStatus.NO_CHANGE;
  }

}


//     def format(self, arg_tuple=None):
//         if arg_tuple == None:
//             if self.var_infos:
//                 arg = self.var_infos[0].name
//             # not sure whether this is the right thing, but oh well
//             else:
//                 arg = "var"
//         else:
//             (arg,) = arg_tuple
//
//         as_base = invariant.format(self, arg)
//         if as_base:
//             return as_base
//
//         suffix = " \t(%d values" % (self.values,)
//         if self.can_be_None:
//             suffix += ", can be None)"
//         else:
//             suffix += ")"
//
//         if self.modulus and self.modulus_justified():
//             return arg + " = %d (mod %d)" % self.modulus + suffix
//         elif self.nonmodulus and self.nonmodulus_justified():
//             return arg + " != %d (mod %d)" % self.nonmodulus + suffix
//
//         nonzero = ((self.min < 0) and (self.max > 0)
//                    and (not self.can_be_zero) and self.nonzero_justified())
//
//         if self.min_justified and self.max_justified:
//             result = " in [%s..%s]" % (self.min, self.max)
//             if nonzero:
//                 result = " nonzero" + result
//             return arg + result + suffix
//         if self.min_justified and (self.min != 0 or not self.nonnegative_obvious):
//             result = "%s >= %s" % (arg, self.min)
//             if nonzero:
//                 result += " and nonzero"
//             return result + suffix
//         if self.max_justified:
//             result = "%s <= %s" % (arg, self.max)
//             if nonzero:
//                 result += " and nonzero"
//             return result + suffix
//         if nonzero:
//             return arg + "!= 0" + suffix
//
//         if self.one_of and not self.can_be_None:
//             return "%s in %s" % (arg, util.format_as_set(self.one_of))
//
//         return arg + " unconstrained" + suffix
//
//     def diff(self, other):
//         # print "diff(single_sequence_numeric_invariant)"
//         inv1 = self
//         inv2 = other
//
//         # If they print the same, then make them compare the same
//         if diffs_same_format(inv1, inv2):
//             return None
//
//         as_base = invariant.diff(inv1, inv2)
//         if as_base:
//             return as_base
//
//         min_missing = ((inv1.min_justified and not inv2.min_justified)
//                        or (inv2.min_justified and not inv1.min_justified))
//         min_different = (inv1.min_justified and inv2.min_justified
//                          and inv1.min != inv2.min)
//         max_missing = ((inv1.max_justified and not inv2.max_justified)
//                        or (inv2.max_justified and not inv1.max_justified))
//         max_different = (inv1.max_justified and inv2.max_justified
//                          and (inv1.max != inv2.max))
//         # print "max_different=%s" % (max_different,), inv1.max_justified, inv2.max_justified, inv1.max, inv2.max
//         nzj1 = inv1.nonzero_justified()
//         nzj2 = inv1.nonzero_justified()
//         zero_different = (nzj1 and not nzj2) or (nzj2 and not nzj1)
//
//         modulus_different = (inv1.modulus != inv2.modulus)
//         nonmodulus_different = (inv1.nonmodulus != inv2.nonmodulus)
//
//         if result == []:
//             return None
//         return string.join(result, ", ")
