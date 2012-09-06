package daikon.inv;

import daikon.*;

import utilMDE.*;

import java.io.Serializable;

// This is the successor to ValueTracker1.
// It is a thin wrapper around LimitedSizeIntSet.
// (Actually, maybe it will just subclass that.)


/**
 * ValueSet stores up to some maximum number of unique non-zero integer
 * values, at which point its rep is nulled.  This is used for efficient
 * justification tests.
 **/
public abstract class ValueSet extends LimitedSizeIntSet
  implements Serializable, Cloneable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public ValueSet(int max_values) {
    super(max_values);
  }

  public static ValueSet factory(VarInfo var_info) {
    ProglangType rep_type = var_info.rep_type;
    boolean is_scalar = rep_type.isScalar();
    if (is_scalar) {
      return new ValueSet.ValueSetScalar(44);
    } else if (rep_type == ProglangType.INT_ARRAY) {
      return new ValueSet.ValueSetScalarArray(44);
    } else if (Daikon.dkconfig_enable_floats
               && rep_type == ProglangType.DOUBLE) {
      return new ValueSet.ValueSetFloat(44);
    } else if (Daikon.dkconfig_enable_floats
               && rep_type == ProglangType.DOUBLE_ARRAY) {
      return new ValueSet.ValueSetFloatArray(44);
    } else if (rep_type == ProglangType.STRING) {
      return new ValueSet.ValueSetString(44);
    } else if (rep_type == ProglangType.STRING_ARRAY) {
      return new ValueSet.ValueSetStringArray(44);
    } else {
      throw new Error("Can't create ValueSet for " + var_info.name()
                      + " with rep type " + rep_type);
    }
  }

  /** Track the specified object. **/
  public abstract void add(Object v1);

  /** Add stats from the specified value set. */
  protected abstract void add_stats (ValueSet other);

  /** Returns a short description of the values seen. **/
  public abstract String repr_short();

  public void add(ValueSet other) {
    if (this.getClass() != other.getClass()) {
      throw new Error("ValueSet type mismatch: " + this.getClass() + " " + other.getClass());
    }
    addAll(other);
    add_stats (other);
  }

  public static class ValueSetScalar extends ValueSet {
    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20031017L;

    long min_val = Long.MAX_VALUE;
    long max_val = Long.MIN_VALUE;

    public ValueSetScalar(int max_values) {
      super(max_values);
    }
    public void add(Object v1) {
      Assert.assertTrue(v1 != null);
      long val = ((Long) v1).longValue();
      if (val < min_val) { min_val = val; }
      if (val > max_val) { max_val = val; }
      add(UtilMDE.hash(val));
    }

    protected void add_stats (ValueSet other) {
      ValueSetScalar vs = (ValueSetScalar) other;
      min_val = Math.min (min_val, vs.min_val);
      max_val = Math.max (max_val, vs.max_val);
    }

    public long min() { return (min_val); }
    public long max() { return (max_val); }

    public String repr_short() {
      if (size() > 0)
        return (size() + " values " + min_val + ".." + max_val);
      else
        return ("0 values");
    }

  }

  public static class ValueSetFloat extends ValueSet {
    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20031017L;

    double min_val = Double.MAX_VALUE;
    double max_val = -Double.MAX_VALUE;
    boolean can_be_NaN = false;

    public ValueSetFloat(int max_values) {
      super(max_values);
    }
    public void add(Object v1) {
      double val = ((Double) v1).doubleValue();
      if (val < min_val) { min_val = val; }
      if (val > max_val) { max_val = val; }
      if (Double.isNaN(val)) { can_be_NaN = true; }
      add(UtilMDE.hash(val));
    }

    protected void add_stats (ValueSet other) {
      ValueSetFloat vs = (ValueSetFloat) other;
      min_val = Math.min (min_val, vs.min_val);
      max_val = Math.max (max_val, vs.max_val);
      can_be_NaN = can_be_NaN || vs.can_be_NaN;
    }

    public double min() { return (min_val); }
    public double max() { return (max_val); }
    public boolean canBeNaN() { return (can_be_NaN); }

    public String repr_short() {
      if (size() > 0)
        return (size() + " values " + min_val + ".." + max_val
                + "; " + (can_be_NaN ? "can be " : "never ") + "NaN");
      else
        return ("0 values");
    }

  }

  public static class ValueSetScalarArray extends ValueSet {
    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20031017L;

    long min_val = Long.MAX_VALUE;
    long max_val = Long.MIN_VALUE;
    int max_length = 0;
    int elem_cnt = 0;
    int multi_arr_cnt = 0;  // number of arrays with 2 or more elements

    public ValueSetScalarArray(int max_values) {
      super(max_values);
    }
    public void add(Object v1) {
      long[] val = (long[]) v1;
      if (val != null) {
        for (int i = 0; i < val.length; i++) {
          if (val[i] < min_val) { min_val = val[i]; }
          if (val[i] > max_val) { max_val = val[i]; }
        }
        elem_cnt += val.length;
        if (val.length > 1)
          multi_arr_cnt++;
        if (val.length > max_length)
          max_length = val.length;
      }
      add(UtilMDE.hash((long[]) v1));
    }

    protected void add_stats (ValueSet other) {
      ValueSetScalarArray vs = (ValueSetScalarArray) other;
      min_val = Math.min (min_val, vs.min_val);
      max_val = Math.max (max_val, vs.max_val);
      elem_cnt += vs.elem_cnt;
      multi_arr_cnt += vs.multi_arr_cnt;
      max_length = Math.max (max_length, vs.max_length);
    }

    public long min() { return (min_val); }
    public long max() { return (max_val); }
    public int elem_cnt() { return (elem_cnt); }
    public int multi_arr_cnt() { return (multi_arr_cnt); }
    public int max_length() { return (max_length); }

    public String repr_short() {
      if (size() > 0)
        return (size() + " values " + min_val + ".." + max_val);
      else
        return ("0 values");
    }

  }

  public static class ValueSetFloatArray extends ValueSet {
    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20031017L;

    double min_val = Long.MAX_VALUE;
    double max_val = Long.MIN_VALUE;
    boolean can_be_NaN = false;
    int max_length = 0;
    int elem_cnt = 0;
    int multi_arr_cnt = 0;  // number of arrays with 2 or more elements

    public ValueSetFloatArray(int max_values) {
      super(max_values);
    }
    public void add(Object v1) {
      double[] val = (double[]) v1;
      if (val != null) {
        for (int i = 0; i < val.length; i++) {
          if (val[i] < min_val) { min_val = val[i]; }
          if (val[i] > max_val) { max_val = val[i]; }
          if (Double.isNaN(val[i])) { can_be_NaN = true; }
        }
        elem_cnt += val.length;
        if (val.length > 1)
          multi_arr_cnt++;
        if (val.length > max_length)
          max_length = val.length;
      }
      add(UtilMDE.hash(val));
    }

    protected void add_stats (ValueSet other) {
      ValueSetFloatArray vs = (ValueSetFloatArray) other;
      min_val = Math.min (min_val, vs.min_val);
      max_val = Math.max (max_val, vs.max_val);
      can_be_NaN = can_be_NaN || vs.can_be_NaN;
      elem_cnt += vs.elem_cnt;
      multi_arr_cnt += vs.multi_arr_cnt;
      max_length = Math.max (max_length, vs.max_length);
    }

    public double min() { return (min_val); }
    public double max() { return (max_val); }
    public boolean canBeNaN() { return (can_be_NaN); }
    public int elem_cnt() { return (elem_cnt); }
    public int multi_arr_cnt() { return (multi_arr_cnt); }
    public int max_length() { return (max_length); }

    public String repr_short() {
      if (size() > 0)
        return (size() + " values " + min_val + ".." + max_val
                + "; " + (can_be_NaN ? "can be " : "never ") + "NaN");
      else
        return ("0 values");
    }

  }

  public static class ValueSetString extends ValueSet {
    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20031017L;

    public ValueSetString(int max_values) {
      super(max_values);
    }
    public void add(Object v1) {
      add(UtilMDE.hash((String) v1));
    }

    protected void add_stats (ValueSet other) {
    }

    public String repr_short() {
      return (size() + " values ");
    }
  }

  public static class ValueSetStringArray extends ValueSet {
    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20031017L;

    int elem_cnt = 0;
    int multi_arr_cnt = 0;  // number of arrays with 2 or more elements

    public ValueSetStringArray(int max_values) {
      super(max_values);
    }
    public void add(Object v1) {
      String[] val = (String[]) v1;
      if (val != null) {
        elem_cnt += val.length;
        if (val.length > 1)
          multi_arr_cnt++;
      }
      add(UtilMDE.hash(val));
    }

    protected void add_stats (ValueSet other) {
      ValueSetStringArray vs = (ValueSetStringArray) other;
      elem_cnt += vs.elem_cnt;
      multi_arr_cnt += vs.multi_arr_cnt;
    }
    public int elem_cnt() { return (elem_cnt); }
    public int multi_arr_cnt() { return (multi_arr_cnt); }

    public String repr_short() {
      return (size() + " values ");
    }

  }



}
