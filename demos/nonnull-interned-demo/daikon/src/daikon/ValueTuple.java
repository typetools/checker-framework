package daikon;

import daikon.derive.*;

import java.util.logging.Logger;

import java.util.*;

import utilMDE.*;

import checkers.quals.Interned;


/**
 * This is the data structure that holds the tuples of values seen so far
 * (and how many times each was seen) for a particular program point.  VarInfo
 * objects can use this to get the values of the variables they represent.
 * <br>
 * While the arrays and their elements are interned, the ValueTuple objects
 * themselves are not interned.
 **/
public final class ValueTuple implements Cloneable {

  /** Debug tracer. **/
  public static Logger debug = Logger.getLogger("daikon.ValueTuple");

  // These arrays are interned, and so are their elements.
  public /*@Interned*/ Object[/*@Interned*/] vals;

  // consider putting this in the first slot of "vals", to avoid the Object
  // overhead of a pair of val and mods.  Do I need to worry about trickery
  // such as orderings changing when we add derived values?  I think not...

  // I need to have some kind of access to this representation so that
  // external code can create one of these and pass it in.  Or maybe
  // external code always passes in an ordinary array and I convert it to
  // the packed representation if appropriate.  (That does seem cleaner,
  // although it might be less efficient.)

  /**
   * Modification bit per value, possibly packed into fewer ints than the
   * vals field.  Don't use a single int because that won't scale to (say)
   * more than 16 values.
  **/
  public /*@Interned*/ int[] mods;


  // Right now there are only three meaningful values for a mod:
  /** Not modified.  **/
  public static final int UNMODIFIED = 0;
  /** Modified.  **/
  public static final int MODIFIED = 1;
  /** Missing value because the expression doesn't make sense: x.a
   * when x is null.  Data trace files can contain this modbit. **/
  public static final int MISSING_NONSENSICAL = 2;
  /** Missing value because of data flow: this.x.x isn't available
   * from a ppt.  Data trace files must not contain this modbit. **/
  public static final int MISSING_FLOW = 3;
  /** Maximum mod bit value.  Always set to 1+ last modbit value.  **/
  public static final int MODBIT_VALUES = 4;
  // Out of the range of MODBIT_VALUES because this won't appear in the
  // tables; it gets converted to UNMODIFIED or MODIFIED, depending on
  // whether this is the first sample.  (Not sure whether that is the right
  // strategy in the long term; it does let me avoid changing code in the
  // short term.)
  public static final int STATIC_CONSTANT = 22;

  // implementation for unpacked representation

  public int getModified(VarInfo vi) { return vi.getModified(this); }
  public boolean isUnmodified(VarInfo vi) { return vi.isUnmodified(this); }
  public boolean isModified(VarInfo vi) { return vi.isModified(this); }
  public boolean isMissingNonsensical(VarInfo vi) { return vi.isMissingNonsensical(this); }
  public boolean isMissingFlow(VarInfo vi) { return vi.isMissingFlow(this); }
  public boolean isMissing(VarInfo vi) { return vi.isMissing(this); }

  int getModified(int value_index) { return mods[value_index]; }
  boolean isUnmodified(int value_index) { return mods[value_index] == UNMODIFIED; }
  boolean isModified(int value_index) { return mods[value_index] == MODIFIED; }
  boolean isMissingNonsensical(int value_index) { return mods[value_index] == MISSING_NONSENSICAL; }
  boolean isMissingFlow(int value_index) { return mods[value_index] == MISSING_FLOW; }
  boolean isMissing(int value_index) { return (isMissingNonsensical(value_index)
                                               || isMissingFlow(value_index)); }

  // The arguments ints represent modification information.
  static boolean modIsUnmodified(int mod_value) { return mod_value == UNMODIFIED; }
  static boolean modIsModified(int mod_value) { return mod_value == MODIFIED; }
  static boolean modIsMissingNonsensical(int mod_value) { return mod_value == MISSING_NONSENSICAL; }
  static boolean modIsMissingFlow(int mod_value) { return mod_value == MISSING_FLOW; }

  // A tuplemod is summary modification information about the whole tuple
  // rather than about specific elements of the tuple.
  // There are two potentially useful abstractions for mod bits over an
  // entire tuple in aggregate:
  //  * return missing if any is missing (good for slices;
  //    indicates that we can't use that value)
  //  * return missing is all are missing (good for non-slices;
  //    the number that are guaranteed to be missing in slices
  // The same thing can be argued about "unmodified", actually.
  // So there are 8 states corresponding to 3 booleans:
  // modified, unmodified, missing
  //  * has modified, has unmodified, has missing
  //  * has modified, has unmodified, no missing
  //  * has modified, no unmodified, has missing
  //  * has modified, no unmodified, no missing
  //    ie, all modified
  //  * no modified, has unmodified, has missing
  //  * no modified, has unmodified, no missing
  //    ie, all unmodified
  //  * no modified, no unmodified, has missing
  //    ie, all missing; probably impossible
  //  * no modified, no unmodified, no missing
  //    impossible

  public static final int TUPLEMOD_VALUES = MathMDE.pow(2, MODBIT_VALUES);
  public static final int UNMODIFIED_BITVAL = MathMDE.pow(2, UNMODIFIED);
  public static final int MODIFIED_BITVAL = MathMDE.pow(2, MODIFIED);
  public static final int MISSING_NONSENSICAL_BITVAL = MathMDE.pow(2, MISSING_NONSENSICAL);
  public static final int MISSING_FLOW_BITVAL = MathMDE.pow(2, MISSING_FLOW);
  // Various slices of the 8 (=TUPLEMOD_VALUES) possible tuplemod values.
  // The arrays are filled up in a static block below.
  // (As of 1/9/2000, tuplemod_modified_not_missing is used only in
  // num_mod_samples(), and tuplemod_not_missing is not used.)
  public static final int[] tuplemod_not_missing = new int[TUPLEMOD_VALUES/2];
  public static final int[] tuplemod_modified_not_missing = new int[TUPLEMOD_VALUES/4];

  static {
    int i1 = 0, i2 = 0;
    for (int tm=0; tm<TUPLEMOD_VALUES; tm++) {
      if (!tuplemodHasMissingFlow(tm) && !tuplemodHasMissingNonsensical(tm) ) {
        tuplemod_not_missing[i1] = tm;
        i1++;
      }
      if (tuplemodHasModified(tm) && !tuplemodHasMissingFlow(tm) && !tuplemodHasMissingNonsensical(tm)) {
        tuplemod_modified_not_missing[i2] = tm;
        i2++;
      }
    }
  }

  static int make_tuplemod(boolean unmodified, boolean modified, boolean missingNonsensical, boolean missingFlow) {
    int result = 0;
    if (unmodified) result += UNMODIFIED_BITVAL;
    if (modified) result += MODIFIED_BITVAL;
    if (missingNonsensical) result += MISSING_NONSENSICAL_BITVAL;
    if (missingFlow) result += MISSING_FLOW_BITVAL;
    return result;
  }

  static boolean tuplemodHasModified(int tuplemod) {
    return ((tuplemod & MODIFIED_BITVAL) != 0);
  }
  static boolean tuplemodHasUnmodified(int tuplemod) {
    return ((tuplemod & UNMODIFIED_BITVAL) != 0);
  }
  static boolean tuplemodHasMissingNonsensical(int tuplemod) {
    return ((tuplemod & MISSING_NONSENSICAL_BITVAL) != 0);
  }

  static boolean tuplemodHasMissingFlow(int tuplemod) {
    return ((tuplemod & MISSING_FLOW_BITVAL) != 0);
  }

  /**
   * In output, M=modified, U=unmodified, X=missing.
   * Capital letters indicate the specified modbit does occur,
   * lowercase letters indicate it does not occur.
   **/
  static String tuplemodToStringBrief(int tuplemod) {
    return ((tuplemodHasModified(tuplemod) ? "M" : "m")
            + (tuplemodHasUnmodified(tuplemod) ? "U" : "u")
            + (tuplemodHasMissingNonsensical(tuplemod) ? "X" : "x")
            + (tuplemodHasMissingFlow(tuplemod) ? "F" : "f"));
  }


  static int tupleMod(int[] mods) {
    boolean[] has_modbit_val = new boolean[MODBIT_VALUES];
    // Extraneous, as the array is initialized to all zeroes.
    Arrays.fill(has_modbit_val, false);
    for (int i=0; i<mods.length; i++) {
      has_modbit_val[mods[i]] = true;
    }
    return make_tuplemod(has_modbit_val[UNMODIFIED],
                         has_modbit_val[MODIFIED],
                         has_modbit_val[MISSING_NONSENSICAL],
                         has_modbit_val[MISSING_FLOW]);
  }

  int tupleMod() {
    return ValueTuple.tupleMod(mods);
  }

  public static int parseModified(String raw) {
    int result = Integer.parseInt(raw);
    Assert.assertTrue((result >= 0) && (result < MODBIT_VALUES));
    return result;
  }


  /**
   * Get the value of the variable vi in this ValueTuple.  Note: the
   * VarInfo form is preferred
   * @param vi the variable whose value is to be returned
   * @return the value of the variable at this ValueTuple
   **/
  public /*@Interned*/ Object getValue(VarInfo vi) {
    assert vi.value_index < vals.length : vi;
    return vi.getValue(this);   // this looks like a checker bug
  }

  /**
   * Get the value at the val_index.
   * Note: For clients, getValue(VarInfo) is preferred to getValue(int).
   * @see #getValue(VarInfo)
   **/
  /*@Interned*/ Object getValue(int val_index) { return vals[val_index]; }


  /** Default constructor that interns its argument. */
  public ValueTuple(Object[/*@Interned*/] vals, int[/*@Interned*/] mods) {
    this.vals = Intern.intern(vals); // checker error due to checker weakness.  The type of intern needs to be polymorphic.  It is Intern.intern (Object[]) -> @Interned Object[], but we want Intern.intern (Object[@Interned]) -> @Interned Object[@Interned]
    this.mods = Intern.intern(mods);
  }

  // Private constructor that doesn't perform interning.
  @SuppressWarnings("interned") // interning constructor
  private ValueTuple(Object[] vals, int[] mods, boolean check) {
    Assert.assertTrue((!check) || Intern.isInterned(vals));
    Assert.assertTrue((!check) || Intern.isInterned(mods));
    this.vals = vals;
    this.mods = mods;
  }

  /** Creates and returns a copy of this. **/
  // Default implementation to quiet Findbugs.
  public ValueTuple clone() throws CloneNotSupportedException {
    return (ValueTuple) super.clone();
  }

  /**
   * More convenient name for the constructor that doesn't intern.
   *
   * This is not private because it is used (only) by read_data_trace_file,
   * which makes a partial ValueTuple, fills it in with derived variables,
   * and only then interns it; the alternative would be for derived
   * variables to take separate vals and mods arguments.  No one else
   * should use it!
   **/
  @SuppressWarnings("interned") // interning constructor
  public static ValueTuple makeUninterned(Object[] vals, int[] mods) {
    return new ValueTuple(vals, mods, false);
  }


  /** Constructor that takes already-interned arguments. */
  static ValueTuple makeFromInterned(/*@Interned*/ Object[/*@Interned*/] vals, int[] mods) {
    return new ValueTuple(vals, mods, true);
  }


  // Like clone(), but avoids its problems of default access and returning
  // an Object.
  public ValueTuple shallowcopy() {
    return ValueTuple.makeFromInterned(vals, mods);
  }

  // These definitions are intended to make different ValueTuples with the
  // same contents compare identically.
  public boolean equals(Object obj) {
    if (! (obj instanceof ValueTuple))
      return false;
    ValueTuple other = (ValueTuple) obj;
    return (vals == other.vals) && (mods == other.mods);
  }
  public int hashCode() {
    return vals.hashCode() * 31 + mods.hashCode();
  }


  public int size() {
    Assert.assertTrue(vals.length == mods.length);
    return vals.length;
  }

  /** Return a new ValueTuple containing this one's first len elements. **/
  public ValueTuple trim(int len) {
    Object[] new_vals = ArraysMDE.subarray(vals, 0, len);
    int[] new_mods = ArraysMDE.subarray(mods, 0, len);
    return new ValueTuple(new_vals, new_mods);
  }


  // For debugging
  public String toString() {
    StringBuffer sb = new StringBuffer("[");
    Assert.assertTrue(vals.length == mods.length);
    for (int i=0; i<vals.length; i++) {
      if (i>0)
        sb.append("; ");
      if (vals[i] instanceof String)
        sb.append("\"" + ((String)vals[i]) + "\"");
      else if (vals[i] instanceof long[])
        sb.append(ArraysMDE.toString((long[])vals[i]));
      else if (vals[i] instanceof int[])
        // shouldn't reach this case -- should be long[], not int[]
        sb.append(ArraysMDE.toString((int[])vals[i]));
      else if (vals[i] instanceof double[])
        sb.append(ArraysMDE.toString ((double[])vals[i]));
      else if (vals[i] instanceof String[])
        sb.append(ArraysMDE.toString((String[])vals[i]));
      else
        sb.append(vals[i]);
      sb.append(",");
      sb.append(mods[i]);
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Return the values of this tuple, annotated with the VarInfo that
   * would be associated with the value.
   **/
  public String toString(VarInfo[] vis) {
    StringBuffer sb = new StringBuffer("[");
    Assert.assertTrue(vals.length == mods.length);
    Assert.assertTrue(vals.length == vis.length);
    for (int i=0; i<vals.length; i++) {
      if (i>0)
        sb.append("; ");
      sb.append (vis[i].name() + ": ");
      if (vals[i] instanceof String)
        sb.append("\"" + vals[i] + "\"");
      else if (vals[i] instanceof long[])
        sb.append(ArraysMDE.toString((long[])vals[i]));
      else if (vals[i] instanceof int[])
        // shouldn't reach this case -- should be long[], not int[]
        sb.append(ArraysMDE.toString((int[])vals[i]));
      else
        sb.append(vals[i]);
      sb.append(",");
      sb.append(mods[i]);
    }
    sb.append("]");
    return sb.toString();
  }

  public static String valsToString(Object[] vals) {
    StringBuffer sb = new StringBuffer("[");
    for (int i=0; i<vals.length; i++) {
      if (i>0)
        sb.append(", ");
      sb.append (valToString (vals[i]));
    }
    sb.append("]");
    return sb.toString();
  }

  public static String valToString (Object val) {
    if (val == null) return "null";
    if (val instanceof long[])
      return(ArraysMDE.toString((long[])val));
    else if (val instanceof int[])
      // shouldn't reach this case -- should be long[], not int[]
      return(ArraysMDE.toString((int[])val));
    else
      return(val.toString());
  }

  /**
   * Return a new ValueTuple consisting of the elements of this one with
   * indices listed in indices.
   **/
  public ValueTuple slice(int[] indices) {
    int new_len = indices.length;
    Object[] new_vals = new Object[new_len];
    int[] new_mods = new int[new_len];
    for (int i=0; i<new_len; i++) {
      new_vals[i] = vals[indices[i]];
      new_mods[i] = mods[indices[i]];
    }
    return new ValueTuple(new_vals, new_mods);
  }

}
