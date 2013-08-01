package daikon.tools;

import daikon.*;
import daikon.inv.*;
import utilMDE.*;

import java.util.*;
import java.io.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Provides a variable translation over an invariant at one program
 * point (perhaps in a different program) to a similar inariant at a
 * second program point.  In general, on order for a translation to be
 * possible, the invariants must be of the same class.  For example,
 * consider the invariants (x>y) at ppt1 and (p>q) at ppt2.  Since the
 * invariants are the same, the translation is (x -> p) and (y -> q)
 *
 * The quality of the translation is also determined (approximately on
 * a scale of 0 to 100).  If the invariants are not of the same class,
 * no translation is possible and the quality is zero.  If the class
 * and the formula are the same, the match is excellent (80).  If the
 * class is the same and the formula is different, the match is mediocre (40).
 * The quality is increased for variables with exactly the same derivation
 * and decreased for  those with different derivations.
 *
 * Other checks could be added to further specify the quality.  For example,
 * if one invariant is a precondition and the other is a postcondition,
 * the quality should be reduced.
 */
public class InvTranslate {

  /**
   * The quality of the mapping.  0 indicates no mapping is possible.
   * 100 indicates that there is a perfect translation
   */
  int quality = 0;

  /** Ppt of source invariant **/
  public PptTopLevel ppt = null;

  /** Map of variables from inv to inv **/
  Map<String,String> var_map = new LinkedHashMap<String,String>();

  /** source invariant **/
  Invariant inv1;

  /** destination invariant **/
  Invariant inv2;

  /** an empty translation **/
  public InvTranslate () {
    quality = 0;
  }

  /**
   * Setup a translation from i1 to i2.  The quality and the variable
   * map is set accordingly
   */
  public void translate (Invariant i1, Invariant i2) {

    inv1 = i1;
    inv2 = i2;

    // For now if the classes don't match, there is no translation
    if (i1.getClass() != i2.getClass()) {
      quality = 0;
      return;
    }

    if (i1.isSameFormula (i2))
      quality = 80;
    else
      quality = 40;

    // Create the simple mapping and adjust for the quality of the variable
    // mapping (variables of the same derivation are better than those of
    // different derivations)
    for (int i = 0; i < i1.ppt.var_infos.length; i++) {
      VarInfo v1 = i1.ppt.var_infos[i];
      VarInfo v2 = i2.ppt.var_infos[i];
      add_variable_map (v1.name(), v2.name());
      if ((v1.derived == null) && (v2.derived == null))
        quality += 5;
      else if ((v1.derived != null) && (v2.derived != null)
               && (v1.derived.getClass() == v2.derived.getClass()))
        quality += 5;
      else /* variables have different derivations */
        quality -= 5;
    }
  }

  private static InvTranslate no_translate = new InvTranslate();
  static InvTranslate no_translate() {
    return no_translate;
  }

  /**
   * Add the specified variable names to the variable translation
   */
  private void add_variable_map (String v1_name, String v2_name) {

    Assert.assertTrue (!var_map.containsKey (v1_name));

    var_map.put (v1_name, v2_name);
  }

  /**
   * Returns a somewhat verbose description of the translation
   */
  public String toString () {

    String out = "";
    for (String key : var_map.keySet()) {
      String value = var_map.get (key);
      if (out != "")            // interned
        out += ", ";
      out += key + "->" + value;
    }
    out += " [Quality=" + quality + "]";
    if ((inv1 != null) && (inv2 != null))
      out += " [" + inv1.format() + " -> " + inv2.format() + "]";
    return (out);
  }
}
