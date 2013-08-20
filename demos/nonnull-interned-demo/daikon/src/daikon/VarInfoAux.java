package daikon;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import checkers.quals.Interned;

/**
 * Represents additional information about a VarInfo that frontends
 * tell Daikon.  For example, whether order matters in a collection.
 * This is immutable and interned.
 **/

public final class VarInfoAux
  implements Cloneable, Serializable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020614L;

  /**
   * General debug tracer.
   **/
  public static final Logger debug = Logger.getLogger("daikon.VarInfoAux");

  /**
   * Whether the elements in this collection are all the meaningful
   * elements, or whether there is a null at the end of this
   * collection that ends the collection.
   **/
  public static final String NULL_TERMINATING = "nullTerminating";

  /**
   * Whether this variable is a parameter to a method, or derived from
   * a parameter to a method.  By default, if p is a parameter, then
   * some EXIT invariants related to p aren't printed.  Frontends are
   * responsible for setting if p is a parameter and if p.a is a
   * parameter.  In Java, p.a is not a parameter, whereas in IOA, it
   * is.
   **/
  public static final String IS_PARAM = "isParam";

  /**
   * Whether repeated elements can exist in this collection.
   **/
  public static final String HAS_DUPLICATES = "hasDuplicates";

  /**
   * Whether order matters.
   **/
  public static final String HAS_ORDER = "hasOrder";

  /**
   * Whether taking the size of this matters.
   **/
  public static final String HAS_SIZE = "hasSize";

  /**
   * Java-specific. The package name of the class that declares this
   * variable, if the variable is a field. If it's not a field of some
   * class, the value of this key is "no_package_name_string".
   */
  public static final String PACKAGE_NAME = "declaringClassPackageName";

  public static final String NO_PACKAGE_NAME = "no_package_name_string";

  /**
   * Whether null has a special meaning for this variable or its members.
   **/
  public static final String HAS_NULL = "hasNull";

  public static final String TRUE = "true";
  public static final String FALSE = "false";

  /**
   * Whether this variable is an inline structure or a reference to
   * a structure (class).  By default it is a reference.  If it is
   * an inlined structure (or array), it doesn't make sense to look
   * for invariants over its hashcode.  Front ends include references
   * to inlined structures as variables because some tools that follow
   * daikon need other information about the variable.
   */
  public static final String IS_STRUCT = "isStruct";

  /**
   * Return an interned VarInfoAux that represents a given string.
   * Elements are separated by commas, in the form:
   *
   * <li> x = a, "a key" = "a value"
   * <br>
   * Parse allow for quoted elements.  White space to the left and
   * right of keys and values do not matter, but inbetween does.
   **/
  public static /*@Interned*/ VarInfoAux parse (String inString) throws IOException {
    Reader inStringReader = new StringReader(inString);
    StreamTokenizer tok = new StreamTokenizer (inStringReader);
    tok.resetSyntax();
    tok.wordChars(0, Integer.MAX_VALUE);
    tok.quoteChar('\"');
    tok.ordinaryChars(',', ',');
    tok.ordinaryChars('=', '=');
    Map</*@Interned*/ String,/*@Interned*/ String> map = theDefault.map;

    String key = "";
    String value = "";
    boolean seenEqual = false;
    for (int tokInfo = tok.nextToken(); tokInfo != StreamTokenizer.TT_EOF;
         tokInfo = tok.nextToken()) {
      @SuppressWarnings("interned") // initialization-checking pattern
      boolean mapUnchanged = (map == theDefault.map);
      if (mapUnchanged) {
        // We use default values if none are specified.  We initialize
        // here rather than above to save time when there are no tokens.

        map = new HashMap</*@Interned*/ String,/*@Interned*/ String>(theDefault.map);
      }

      /*@Interned*/ String token;
      if (tok.ttype == StreamTokenizer.TT_WORD || tok.ttype == '\"') {
        token = tok.sval.trim().intern();
      } else {
        token = ((char) tok.ttype + "").intern();
      }

      debug.fine ("Token info: " + tokInfo + " " + token);

      if (token == ",") {       // interned
        if (!seenEqual)
          throw new IOException ("Aux option did not contain an '='");
        map.put (key.intern(), value.intern());
        key = "";
        value = "";
        seenEqual = false;
      } else if (token == "=") { // interned
        if (seenEqual)
          throw new IOException ("Aux option contained more than one '='");
        seenEqual = true;
      } else {
        if (!seenEqual) {
          key = (key + " " + token).trim();
        } else {
          value = (value + " " + token).trim();
        }
      }
    }

    if (seenEqual) {
      map.put (key.intern(), value.intern());
    }

    // Interning
    VarInfoAux resultUninterned = new VarInfoAux(map);
    /*@Interned*/ VarInfoAux result = resultUninterned.intern();
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("New parse " + result);
      debug.fine ("Intern table size: " + new Integer(interningMap.size()));
    }
    return result;
  }


  /**
   * Interned default options.
   **/
  private static /*@Interned*/ VarInfoAux theDefault = new VarInfoAux().intern();

  /**
   * Create a new VarInfoAux with default options.
   **/
  public static /*@Interned*/ VarInfoAux getDefault () {
    return theDefault;
  }



  /**
   * Map for interning.
   **/
  private static Map<VarInfoAux,/*@Interned*/ VarInfoAux> interningMap = null;



  /**
   * Special handler for deserialization.
   **/
  private /*@Interned*/ Object readResolve() throws ObjectStreamException {
    return this.intern();
  }


  /**
   * Contains the actual hashMap for this.
   **/
  private Map</*@Interned*/ String, /*@Interned*/ String> map;


  /**
   * Whether this is interned.
   **/
  private boolean isInterned = false;

  /**
   * Make the default map here.
   **/
  private VarInfoAux () {
    HashMap</*@Interned*/ String, /*@Interned*/ String> defaultMap = new HashMap</*@Interned*/ String,/*@Interned*/ String>();
    // The following are default values.
    defaultMap.put (HAS_DUPLICATES, TRUE);
    defaultMap.put (HAS_ORDER, TRUE);
    defaultMap.put (HAS_SIZE, TRUE);
    defaultMap.put (HAS_NULL, TRUE);
    defaultMap.put (NULL_TERMINATING, TRUE);
    defaultMap.put (IS_PARAM, FALSE);
    defaultMap.put (PACKAGE_NAME, NO_PACKAGE_NAME);
    defaultMap.put (IS_STRUCT, FALSE);
    this.map = defaultMap;
    this.isInterned = false;
  }

  /**
   * Create a new VarInfoAux with default options.
   **/
  private VarInfoAux (Map</*@Interned*/ String,/*@Interned*/ String> map) {
    this.map = map;
    this.isInterned = false;
  }

  /** Creates and returns a copy of this. **/
  // Default implementation to quiet Findbugs.
  public VarInfoAux clone() throws CloneNotSupportedException {
    return (VarInfoAux) super.clone();
  }

  public String toString() {
    return map.toString();
  }

  public int hashCode() {
    return map.hashCode();
  }


  public boolean equals(Object o) {
    if (o instanceof VarInfoAux) {
      return equals((VarInfoAux) o);
    } else {
      return false;
    }
  }

  public boolean equals(VarInfoAux o) {
    return this.map.equals(o.map);
  }

  /**
   * Returns canonical representation of this.  Doesn't need to be
   * called by outside classes because these are always interned.
   **/
  @SuppressWarnings("interned") // intern method
  private /*@Interned*/ VarInfoAux intern() {
    if (this.isInterned) return (/*@Interned*/ VarInfoAux) this; // cast is redundant (except in JSR 308)

    if (interningMap == null) {
      interningMap = new HashMap<VarInfoAux,/*@Interned*/ VarInfoAux>();
    }

    /*@Interned*/ VarInfoAux result;
    if (interningMap.containsKey(this)) {
      result = interningMap.get(this);
    } else {
      // Intern values in map
      interningMap.put (this, (/*@Interned*/ VarInfoAux) this); // cast is redundant (except in JSR 308)
      result = (/*@Interned*/ VarInfoAux) this; // cast is redundant (except in JSR 308)
      this.isInterned = true;
    }
    return result;
  }


  /**
   * Returns the value for the given key.
   **/
  public String getValue(String key) {
    return map.get(key);
  }

  public boolean getFlag(String key) {
    Object value = map.get(key);
    return value.equals(TRUE);
  }

  /** Returns whether or not this is a parameter **/
  public boolean isParam() {
    return getFlag (IS_PARAM);
  }

  /**
   * Return a new VarInfoAux with the desired value set.
   * Does not modify this.
   **/
  public /*@Interned*/ VarInfoAux setValue (String key, String value) {
    HashMap<String,String> newMap = new HashMap<String,String> (this.map);
    newMap.put (key.intern(), value.intern());
    return new VarInfoAux(newMap).intern();
  }

}
