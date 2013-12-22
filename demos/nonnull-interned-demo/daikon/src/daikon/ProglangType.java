package daikon;

import java.util.*;
import java.io.*;

import utilMDE.*;

import checkers.interning.quals.Interned;

/**
 * Represents the type of a variable, for its declared, dtrace file
 * representation, and internal representations.  ProgLangTypes are
 * interned, so they can be == compared.
 **/


// I could also consider using Class; however:
//  * that ties this to a Java front end, as Class can't represent types of
//    (say) C variables.  (not a compelling problem)
//  * that loads the class, which requies that all classes available at
//    runtime be available at inference time.  (not a compelling problem)
//  * Class does not represent inheritance (but I can do that myself);
//    and see isAssignableFrom, which might do all I need.
//  * Class has no "dimensions" field.  isArray() exists, however, as does
//    getComponentType().  I can always determine the number of dimensions
//    from the number of leading "[" in the name, and dimensions is only
//    really needed for parse_value.  parse_value can do literal checks and
//    elementType() can use the name, I suppose.  Or maybe try to
//    instantiate something, though that seems dicier.


// Problem:  this doesn't currently represent inheritance, coercability, or
// other relationships over the base types.

// integral_types = ("int", "char", "float", "double", "integral", "boolean")
// known_types = integral_types + ("pointer", "address")

public final /*@Interned*/ class ProglangType
  implements Serializable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // With Vector search, this func was a hotspot (38%), so use a Map.
  private static HashMap</*@Interned*/ String,Vector<ProglangType>> all_known_types = new HashMap</*@Interned*/ String,Vector<ProglangType>>();

  // The set of (interned) names of classes that implement java.util.List.
  public static HashSet<String> list_implementors = new HashSet<String>();

  static {
    // XXX for now, hard-code these list-implementing types. We should
    // make the front-end dump the language-specific ones into .decls.
    list_implementors.add("java.util.List");
    list_implementors.add("java.util.AbstractList");
    list_implementors.add("java.util.Vector");
    list_implementors.add("java.util.ArrayList");
    list_implementors.add("java.util.AbstractSequentialList");
    list_implementors.add("java.util.LinkedList");
    list_implementors.add("java.util.Stack");
  }

  private /*@Interned*/ String base;          // interned name of base type
  public /*@Interned*/ String base() { return base; }
  private int dimensions;       // number of dimensions
  public int dimensions() { return dimensions; }
  public boolean isArray() { return dimensions > 0; }

  /**
   * The pseudo-dimensions is always at least as large as the
   * dimensions.  Dimensions is always the array depth of the type,
   * while pseudo also incorporates knowledge of Lists.  So List
   * has dims 0 but pseudo-dims as 1.
   **/
  public int pseudoDimensions() { return pseudoDimensions; }
  private int pseudoDimensions;

  public boolean isPseudoArray() { return pseudoDimensions > 0; }

  /**
   * No public constructor:  use parse() instead to get a canonical
   * representation.
   * basetype should be interned.
   **/
  private ProglangType(/*@Interned*/ String basetype, int dimensions) {
    Assert.assertTrue(basetype == basetype.intern());
    this.base = basetype;
    this.dimensions = dimensions;
    pseudoDimensions = dimensions;

    /*
     * We support the rendering of objects as lists for a range of
     * classes derived from specific list abstraction classes.  This
     * requires us to know whether a class is in fact some kind of
     * list, so that we can treat is as a higher-dimensioned object.
     *
     * This is dual to the Implements(List) predicate in DFEJ.
     *
     * The list is supplied in the top of each .decls file, and also
     * as --list_type <type> switches to Daikon.
     */

    if (list_implementors.contains(basetype))
      pseudoDimensions++;
  }

  /**
   * This can't be a constructor because it returns a canonical
   * representation (that can be compared with ==), not necessarily a new
   * object.
   *  @param rep the name of the type, optionally suffixed by
   *  (possibly multiple) "[]"
   **/
  public static ProglangType parse(String rep) {
    String new_base = rep;
    int dims = 0;
    while (new_base.endsWith("[]")) {
      dims++;
      new_base = new_base.substring(0, new_base.length() - 2);
    }
    /*@Interned*/ String new_base_interned = new_base.intern();
    return intern(new_base_interned, dims);
  }

  /**
   * Like parse, but does certain conversions for representation types, in
   * order to return real file representation types even if the
   * file contains something slightly different than the prescribed format.
   **/
  public static ProglangType rep_parse(String rep) {
    ProglangType candidate = parse(rep);
    if ((candidate.base == "address") || (candidate.base == "pointer")) { // interned
      return intern("hashcode", candidate.dimensions);
    } else if (candidate.base == "float") { // interned
      return intern("double", candidate.dimensions);
    } else if (candidate.base == "string") { // interned
      return intern ("java.lang.String", candidate.dimensions);
    } else {
      return candidate;
    }
  }

  /** Convert a file representation type to an internal representation type. **/
  public ProglangType fileTypeToRepType() {
    if ((base == BASE_HASHCODE) || (base == BASE_BOOLEAN)
        || (base == BASE_LONG) || (base == BASE_SHORT))
      return intern("int", dimensions);
    return this;
  }

  /**
   * For serialization; indicates which object to return instead of the
   * one that was just read from the file.  This obviates the need to write
   * a readObject method that interns the interned fields (just "base").
   **/
  public Object readResolve() throws ObjectStreamException {
    return intern(base.intern(), dimensions);
  }

  // Is this necessary?  It will be inhereited from Object.
  // public boolean equals(Object o) {
  //   return this == o;
  // }

  private boolean internal_equals(ProglangType other) {
    return ((base == other.base)
            && (dimensions == other.dimensions));
  }


  // THIS CODE IS A HOT SPOT (~33% of runtime) [as of January 2002].
  /** @param t_base must be interned **/
  private static ProglangType find(/*@Interned*/ String t_base, int t_dims) {
// Disabled for performance reasons! this assertion is sound though:
//    Assert.assertTrue(t_base == t_base.intern());

    // the string maps us to a vec of all plts with that base
    Vector<ProglangType> v = all_known_types.get(t_base);
    if (v == null)
      return null;

    // now search for the right dimension
    for (ProglangType candidate : v) {
      if (candidate.dimensions() == t_dims)
        return candidate;
    }

    return null;
  }

  // private ProglangType intern() {
  //   return ProglangType.intern(this);
  // }

  // We shouldn't even get to the point of interning; we should not
  // have created the ProglangType if it isn't canonical.
  // private static ProglangType intern(ProglangType t) {
  //   ProglangType candidate = find(t.base, t.dimensions);
  //   if (candidate != null)
  //     return candidate;
  //   all_known_types.add(t);
  //   return t;
  // }

  private static ProglangType intern(/*@Interned*/ String t_base, int t_dims) {
    // Disabled for performance reasons! this assertion is sound though:
    //    Assert.assertTrue(t_base == t_base.intern());
    ProglangType result = find(t_base, t_dims);
    if (result != null) {
      return result;
    }
    result = new ProglangType(t_base, t_dims);
    // result = (ProglangType) new ProglangType(t_base, t_dims); // cast is redundant (except in JSR 308)

    Vector<ProglangType> v = all_known_types.get(t_base);
    if (v == null) {
      v = new Vector<ProglangType>();
      all_known_types.put(t_base, v);
    }

    v.add(result);

    return result;
  }

//     def comparable(self, other):
//         base1 = self.base
//         base2 = other.base
//         return ((self.dimensionality == other.dimensionality)
//                 and ((base1 == base2)
//                      or ((base1 == "integral") and (base2 in integral_types)) // interned strings
//                      or ((base2 == "integral") and (base1 in integral_types)))) // interned strings

  /**
   * Returns the type of elements of this.
   * They may themselves be arrays if this is multidimensional.
   **/
  public ProglangType elementType() {
    if (pseudoDimensions > dimensions)
      return OBJECT;
    if (dimensions == 0)
      throw new Error("Called elementType on non-array type " + format());
    Assert.assertTrue(base == base.intern(), "Uninterned base " + base);
    return ProglangType.intern(base, dimensions-1);
  }

  // All these variables are public because the way to check the base of an
  // array is type.base() == ProglangType.BASE_CHAR.

  // Primitive types
  static final /*@Interned*/ String BASE_BOOLEAN = "boolean";
  static final /*@Interned*/ String BASE_BYTE = "byte";
  static final /*@Interned*/ String BASE_CHAR = "char";
  static final /*@Interned*/ String BASE_DOUBLE = "double";
  static final /*@Interned*/ String BASE_FLOAT = "float";
  static final /*@Interned*/ String BASE_INT = "int";
  static final /*@Interned*/ String BASE_LONG = "long";
  static final /*@Interned*/ String BASE_SHORT = "short";

  // Nonprimitive types
  static final /*@Interned*/ String BASE_OBJECT = "java.lang.Object";
  static final /*@Interned*/ String BASE_STRING = "java.lang.String";
  static final /*@Interned*/ String BASE_INTEGER = "java.lang.Integer";
  // "hashcode", "address", and "pointer" are identical;
  // "hashcode" is preferred.
  static final /*@Interned*/ String BASE_HASHCODE = "hashcode";
  // static final /*@Interned*/ String BASE_ADDRESS = "address";
  // static final /*@Interned*/ String BASE_POINTER = "pointer";

  // avoid duplicate allocations
  // No need for the Integer versions; use Long instead.
  // static final /*@Interned*/ Integer IntegerZero = Intern.internedInteger(0);
  // static final /*@Interned*/ Integer IntegerOne = Intern.internedInteger(1);
  static final /*@Interned*/ Long LongZero = Intern.internedLong(0);
  static final /*@Interned*/ Long LongOne = Intern.internedLong(1);
  static final /*@Interned*/ Double DoubleZero = Intern.internedDouble(0);
  static final /*@Interned*/ Double DoubleNaN = Intern.internedDouble(Double.NaN);
  static final /*@Interned*/ Double DoublePositiveInfinity = Intern.internedDouble(Double.POSITIVE_INFINITY);
  static final /*@Interned*/ Double DoubleNegativeInfinity = Intern.internedDouble(Double.NEGATIVE_INFINITY);

  /*
   *  Now that all other static initialisers are done, it is safe to
   *  construct some instances, also statically.  Note that these are
   *  down here to delay their instantiation until after we have
   *  created (e.g.) the list_implementors object, etc.  */

  // Use == to compare, because ProglangType objects are interned.
  public static final ProglangType INT = ProglangType.intern("int", 0);
  public static final ProglangType LONG_PRIMITIVE = ProglangType.intern("long", 0);
  public static final ProglangType DOUBLE = ProglangType.intern("double", 0);
  public static final ProglangType CHAR = ProglangType.intern("char", 0);
  public static final ProglangType STRING = ProglangType.intern("java.lang.String", 0);
  public static final ProglangType INT_ARRAY = ProglangType.intern("int", 1);
  public static final ProglangType LONG_PRIMITIVE_ARRAY = ProglangType.intern("long", 1);
  public static final ProglangType DOUBLE_ARRAY = ProglangType.intern("double", 1);
  public static final ProglangType CHAR_ARRAY = ProglangType.intern("char", 1);
  public static final ProglangType STRING_ARRAY = ProglangType.intern("java.lang.String", 1);
  public static final ProglangType CHAR_ARRAY_ARRAY = ProglangType.intern("char", 2);

  public static final ProglangType INTEGER = ProglangType.intern("java.lang.Integer", 0);
  public static final ProglangType LONG_OBJECT = ProglangType.intern("java.lang.Long", 0);

  public static final ProglangType OBJECT = ProglangType.intern("java.lang.Object", 0);

  public static final ProglangType BOOLEAN = ProglangType.intern("boolean", 0);
  public static final ProglangType HASHCODE = ProglangType.intern("hashcode", 0);
  public static final ProglangType BOOLEAN_ARRAY = ProglangType.intern("boolean", 1);
  public static final ProglangType HASHCODE_ARRAY = ProglangType.intern("hashcode", 1);

  // Like Long.parseLong(), but transform large unsigned longs (as
  // from C's unsigned long long) into the corresponding negative Java
  // longs.  Also handles hex values that begin with 0x
  private static long myParseLong(String value) {
    if (value.length() == 20 && value.charAt(0) == '1' ||
        value.length() == 19 && value.charAt(0) == '9' &&
        value.compareTo("9223372036854775808") >= 0) {
      // Oops, we got a large unsigned long, which Java, having
      // only signed longs, will refuse to parse. We'll have to
      // turn it into the corresponding negative long value.
      String rest; // A substring that will be a valid positive long
      long subtracted; // The amount we effectively subtracted to make it
      if (value.length() == 20) {
        rest = value.substring(1);
        subtracted = 100000L*100000*100000*10000; // 10^19
      } else {
        rest = value.substring(1);
        subtracted = 9L*100000*100000*100000*1000; // 9*10^18
      }
      return Long.parseLong(rest) + subtracted;
    } else if ((value.length() > 2) && (value.charAt(0) == '0')
               && (value.charAt(1) == 'x')) {
      return Long.parseLong (value.substring(2), 16);
    } else {
      return Long.parseLong(value);
    }
  }

  /**
   * Given a string representation of a value (of the type represented by
   * this ProglangType), return the (canonicalized) interpretation of that value.
   * If the type is an array and there
   * are any nonsensical elements in the array, the entire array is
   * considered to be nonsensical (indicated by returning null).  This
   * is not really correct, but it is a reasonable path to take for now
   * (jhp, Feb 12, 2005)
   */
  public final /*@Interned*/ Object parse_value(String value) {
    // System.out.println(format() + ".parse(\"" + value + "\")");

    String value_ = value;      // for debugging, I suppose
    // This only needs to deal with representation types, not with all
    // types in the underlying programming language.



    if (dimensions == 0) {
      if (base == BASE_STRING) {
        if (value.equals("null")) {
          // Fmt.pf ("dimensions = 0, base = string");
          return null;
        }
        if (! (value.startsWith("\"") && value.endsWith("\""))) {
          System.out.println("Unquoted string value: " + value);
          Throwable stack = new Throwable("unquoted string");
          stack.fillInStackTrace();
          stack.printStackTrace();
        }
        // Assert.assertTrue(value.startsWith("\"") && value.endsWith("\""));
        if (value.startsWith("\"") && value.endsWith("\""))
          value = value.substring(1, value.length()-1);
        value = UtilMDE.unescapeNonJava(value);
        return value.intern();
      } else if (base == BASE_CHAR) {
        // This will fail if the character is output as an integer
        // (as I believe the C front end does).
        char c;
        if (value.length() == 1)
          c = value.charAt(0);
        else if ((value.length() == 2) && (value.charAt(0) == '\\'))
          c = UtilMDE.unescapeNonJava(value).charAt(0);
        else if ((value.length() == 4) && (value.charAt(0) == '\\')) {
          Byte b = Byte.decode("0" + value.substring(1));
          return Intern.internedLong(b.longValue());
        }
        else
          throw new IllegalArgumentException("Bad character: " + value);
        return Intern.internedLong(Character.getNumericValue(c));
      } else if (base == BASE_INT) {
        // File rep type might be int, boolean, or hashcode.
        // If we had the actual type, we could do error-checking here.
        // (Example:  no hashcode should be negative, nor any boolean > 1.)
        if (value.equals("false") || value.equals("0"))
          return LongZero;
        if (value.equals("true") || value.equals("1"))
          return LongOne;
        if (value.equals("null"))
          return LongZero;
        return Intern.internedLong(myParseLong(value));
      } else if (base == BASE_DOUBLE) {
        // Must ignore case, because dfej outputs "NaN", while dfec
        // outputs "nan".  dfec outputs "nan", because this string
        // comes from the C++ library.
        if (value.equalsIgnoreCase("NaN"))
          return DoubleNaN;
        if (value.equalsIgnoreCase("Infinity") || value.equals("inf"))
          return DoublePositiveInfinity;
        if (value.equalsIgnoreCase("-Infinity") || value.equals("-inf"))
          return DoubleNegativeInfinity;
        return Intern.internedDouble(value);
      } else {
        throw new Error("unrecognized type " + base);
      }
    } else if (dimensions == 1) {
      // variable is an array

      value = value.trim();

      if (value.equals("null")) {
        // Fmt.pf ("dimensions = 1");
        return null;
      }

      // Try requiring the square brackets around arrays (permits
      // distinguishing between null and an array containing just null).
      if (!(value.startsWith("[") && value.endsWith("]"))) {
        throw new IllegalArgumentException("Array values must be " +
                                           "enlosed in square brackets");
      }
      // Deal with [] surrounding Java array output
      if (value.startsWith("[") && value.endsWith("]")) {
        value = value.substring(1, value.length() - 1).trim();
      }

      // This properly handles strings containing embedded spaces.
      String[] value_strings;
      if (value.length() == 0) {
        value_strings = new String[0];
      } else if (base == BASE_STRING) {
        Vector<String> v = new Vector<String>();
        StreamTokenizer parser = new StreamTokenizer(new StringReader(value));
        parser.quoteChar('\"');
        try {
          while ( parser.nextToken() != StreamTokenizer.TT_EOF ) {
            if (parser.ttype == '\"') {
              v.add(parser.sval);
            } else if (parser.ttype == StreamTokenizer.TT_WORD) {
              if (parser.sval.equals ("nonsensical"))
                return (null);
              Assert.assertTrue(parser.sval.equals("null"));
              v.add(null);
            } else if (parser.ttype == StreamTokenizer.TT_NUMBER) {
              v.add(Integer.toString((int)parser.nval));
            } else {
              System.out.println("Bad ttype " + (char)parser.ttype + " [int=" + parser.ttype + "] while parsing " + value_);
              v.add(null);
            }
          }
        } catch (Exception e) {
          throw new Error(e);
        }
        value_strings = v.toArray(new String[0]);
      } else {
        value_strings = Global.ws_regexp.split(value);
      }
      int len = value_strings.length;

      // This big if ... else should deal with all the primitive types --
      // or at least all the ones that can be rep_types.
      // ("long" and "short" cannot be rep_types; for simplicity, variables
      // declared as long or short have the "int" rep_type.)
      if (base == BASE_INT) {
        long[] result = new long[len];
        for (int i=0; i<len; i++) {
          if (value_strings[i].equals ("nonsensical"))
            return (null);
          else if (value_strings[i].equals("null"))
            result[i] = 0;
          else if (value_strings[i].equals("false"))
            result[i] = 0;
          else if (value_strings[i].equals("true"))
            result[i] = 1;
          else
            result[i] = myParseLong(value_strings[i]);
        }
        return Intern.intern(result);
      } else if (base == BASE_DOUBLE) {
        double[] result = new double[len];
        for (int i=0; i<len; i++) {
          if (value_strings[i].equals ("nonsensical"))
            return (null);
          else if (value_strings[i].equals("null"))
            result[i] = 0;
          else if (value_strings[i].equalsIgnoreCase("NaN"))
            result[i] = Double.NaN;
          else if (value_strings[i].equalsIgnoreCase("Infinity") ||
                   value_strings[i].equals("inf"))
            result[i] = Double.POSITIVE_INFINITY;
          else if (value_strings[i].equalsIgnoreCase("-Infinity") ||
                   value_strings[i].equals("-inf"))
            result[i] = Double.NEGATIVE_INFINITY;
          else
            result[i] = Double.parseDouble(value_strings[i]);
        }
        return Intern.intern(result);
      } else if (base == BASE_STRING) {
        // First, intern each String in the array ...
        Intern.internStrings(value_strings);
        // ... then, intern the entire array, and return it
        return Intern.intern(value_strings);
      } else {
        throw new Error("Can't yet parse array of base type " + base);
      }

      // This is a more general technique; but when will we need
      // such generality?
      // // not elementType() because that interns; here, there is no
      // // need to do the work of interning (I think)
      // ProglangType elt_type = elementType();
      // Object[] result = new Object[len];
      // for (int i=0; i<len; i++)
      //   result[i] = ***;

    } else if (dimensions == 2) {
      if (base == BASE_CHAR) {
        // Array of strings
        throw new Error("To implement");
        // value = tuple(eval(value));
      } else {
        throw new Error("Can't parse a value of type " + format());
      }
    } else {
      throw new Error("Can't parse a value of type " + format());
    }
  }

  public boolean baseIsPrimitive() {
    return ((base == BASE_BOOLEAN)
            || (base == BASE_BYTE)
            || (base == BASE_CHAR)
            || (base == BASE_DOUBLE)
            || (base == BASE_FLOAT)
            || (base == BASE_INT)
            || (base == BASE_LONG)
            || (base == BASE_SHORT));
  }

  public boolean isPrimitive() {
    return ((dimensions == 0) && baseIsPrimitive());
  }

  // Does not include boolean.  Is that intentional?  (If it were added,
  // we would need to change isIndex().)
  public boolean baseIsIntegral() {
    return ((base == BASE_BYTE) ||
            (base == BASE_CHAR) ||
            (base == BASE_INT) ||
            (base == BASE_LONG) ||
            (base == BASE_SHORT) ||
            (base == BASE_INTEGER));
  }

  public boolean isIntegral() {
    return ((dimensions == 0) && baseIsIntegral());
  }

  // More efficient than elementType().isIntegral()
  public boolean elementIsIntegral() {
    return ((dimensions == 1) && baseIsIntegral());
  }

  public boolean elementIsFloat() {
    return ((dimensions == 1) && baseIsFloat());
  }

  // Return true if this variable is sensible as an array index.
  public boolean isIndex() {
    return isIntegral();
  }

  public boolean isScalar() {
    // For reptypes, checking against INT is sufficient, rather than
    // calling isIntegral().
    return (isIntegral()
            || (this == HASHCODE)
            || (this == BOOLEAN));
  }

  public boolean baseIsScalar() {
    return (baseIsIntegral()
            || (base == BASE_BOOLEAN)
            || (base == BASE_HASHCODE));
  }

  public boolean baseIsBoolean() {
    return (base == BASE_BOOLEAN);
  }

  public boolean baseIsFloat() {
    return ((base == BASE_DOUBLE) || (base == BASE_FLOAT));
  }

  public boolean isFloat() {
    return ((dimensions == 0) && baseIsFloat());
  }

  public boolean isObject() {
    return ((dimensions == 0)
            && (baseIsObject()));
  }

  public boolean baseIsObject() {
    return ((! baseIsIntegral())
            && (! baseIsFloat())
            && (! (base == BASE_BOOLEAN)));
  }

  public boolean baseIsString() {
    return (base == BASE_STRING);
  }

  public boolean isString() {
    return ((dimensions == 0) && baseIsString());
  }

  public boolean baseIsHashcode() {
    return (base == BASE_HASHCODE);
  }

  public boolean isHashcode() {
    return ((dimensions == 0) && baseIsHashcode());
  }

  /**
   * Does this type represent a pointer? Should only be applied to
   * file_rep types.
   **/
  public boolean isPointerFileRep() {
    return (base == BASE_HASHCODE);
  }

  /**
   * Return true if these two types can be sensibly compared to one
   * another, or if one can be cast to the other.  For instance, int
   * is castable to long, but boolean is not castable to float, and
   * int is not castable to int[].  This is a reflexive relationship,
   * but not a transitive one because it might not be true for two
   * children of a superclass, even though it's true for the
   * superclass.
   **/
  public boolean comparableOrSuperclassEitherWay(ProglangType other) {
    if (this == other)          // ProglangType objects are interned
      return true;
    if (this.dimensions != other.dimensions)
      return false;
    boolean thisIntegral = this.baseIsIntegral();
    boolean otherIntegral = other.baseIsIntegral();
    if (thisIntegral && otherIntegral)
      return true;
    // Make Object castable to everything, except booleans
    if (((this.base == BASE_OBJECT) && other.baseIsObject()) // interned strings
        || ((other.base == BASE_OBJECT) && baseIsObject())) // interned strings
      return true;

    return false;
  }

  /**
   * Return true if these two types can be sensibly compared to one
   * another, and if non-integral, whether this could be a superclass
   * of other.  A List is comparableOrSuperclassOf to a Vector, but
   * not the other way around.  This is a transitive method, but not
   * reflexive.
   **/
  public boolean comparableOrSuperclassOf (ProglangType other) {
    if (this == other)          // ProglangType objects are interned
      return true;
    if (this.dimensions != other.dimensions)
      return false;
    boolean thisIntegral = this.baseIsIntegral();
    boolean otherIntegral = other.baseIsIntegral();
    if (thisIntegral && otherIntegral)
      return true;
    // Make Object castable to everything, except booleans
    if ((this.base == BASE_OBJECT) && other.baseIsObject()) // interned strings
      return true;

    return false;
  }

  public String format() {
    if (dimensions == 0)
      return base;

    StringBuffer sb = new StringBuffer();
    sb.append(base);
    for (int i=0; i<dimensions; i++)
      sb.append("[]");
    return sb.toString();
  }

  public static String toString (ProglangType[] types) {
    StringBuilderDelimited out = new StringBuilderDelimited(", ");
    for (int i = 0; i < types.length; i++) {
      out.append(types[i]);
    }
    return ("[" + out + "]");
  }

  public String toString() {
    return format();
  }

  /**
   * Returns whether or not this declared type is a function pointer
   * Only valid if the front end marks the function pointer with the
   * name '*func'
   **/
  public boolean is_function_pointer() {
    assert base == base.intern();
    return base == "*func";     // interned
  }

}
