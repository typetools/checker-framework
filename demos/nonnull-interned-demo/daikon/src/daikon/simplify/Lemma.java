package daikon.simplify;

import java.util.Vector;

/** A lemma is an object that wraps a Simplify formula representing
 * some logical statement. The only other thing it adds is a short
 * human-readable description, suitable for debugging.
 *
 * Members of the Lemma class proper represent general theorems, which
 * we give to Simplify as background, with hand-written descriptions.
 **/

public class Lemma implements Comparable<Lemma> {
  public String summary;
  public String formula;

  public Lemma(String s, String f) {
    summary = s;
    formula = f;
  }

  /** Return a human-readable description. */
  public String summarize() {
    return summary;
  }

  /** If this lemma came from an invariant, get its class. */
  public Class invClass() {
    return null;
  }

  public int compareTo(Lemma other) {
    return summarize().compareTo(other.summarize());
  }

  /** Convenience function to give you lemmas[], but as a vector. */
  public static Vector<Lemma> lemmasVector() {
    Vector<Lemma> v = new Vector<Lemma>();
    for (int i = 0; i < lemmas.length; i++) {
      v.add(lemmas[i]);
    }
    return v;
  }

  /** All the theorems we give Simplify (without proof) to help it
   * reason about predicates, functions, and constants that aren't
   * built-in.
   **/
  public static Lemma[] lemmas =
  { new Lemma("null has type T_null",
              "(EQ (typeof null) |T_null|)"),
    new Lemma("objects with null hashes have type T_null",
              "(FORALL (x) (IMPLIES (EQ (hash x) null) (EQ (typeof x) |T_null|)))"),
    new Lemma("hashcodes other than 0 are not null",
              "(FORALL (x) (IMPLIES (NEQ x 0) (NEQ (hashcode x) null)))"),
    new Lemma("'this' is always unchanged",
               "(EQ (hash |this|) (hash |__orig__this|))"),
    new Lemma("arrayLength is non-negative",
              "(FORALL (a) (>= (arrayLength a) 0))"),
    new Lemma("true != false",
              "(NEQ |@true| |@false|)"),
    new Lemma("definition of lexical-> in terms of lexical-<",
              "(FORALL (a i j b ip jp) (IFF (|lexical->| a i j b ip jp) (|lexical-<| b ip jp a i j)))"),
    new Lemma("definition of lexical-==",
              "(FORALL (a i j b ip jp) (IFF (|lexical-==| a i j b ip jp) (AND (EQ (- j i) (- jp ip)) (<= 0 i) (< j (arrayLength a)) (<= 0 ip) (< jp (arrayLength b)) (FORALL (x xp) (IMPLIES (AND (<= i x) (<= x j) (<= ip xp) (<= xp jp) (EQ (- x i) (- xp ip))) (EQ (select (select elems a) x) (select (select elems b) xp)))))))"),
    new Lemma("definition of lexical-<= as a disjunction",
              "(FORALL (a i j b ip jp) (IFF (|lexical-<=| a i j b ip jp) (OR (|lexical-<| a i j b ip jp) (|lexical-==| a i j b ip jp))))"),
    new Lemma("definition of lexical->= as a disjunction",
              "(FORALL (a i j b ip jp) (IFF (|lexical->=| a i j b ip jp) (OR (|lexical->| a i j b ip jp) (|lexical-==| a i j b ip jp))))"),
    new Lemma("definition of lexical-!= as a negation",
              "(FORALL (a i j b ip jp) (IFF (|lexical-!=| a i j b ip jp) (NOT (|lexical-==| a i j b ip jp))))"),
    new Lemma("simplify lexical-== with matching bounds",
              "(FORALL (a i j b ip jp) (IMPLIES (AND (|lexical-==| a i j b ip jp) (EQ i ip) (EQ j jp)) (FORALL (x) (IMPLIES (AND (<= i x) (<= x j)) (EQ (select (select elems a) x) (select (select elems b) x))))))"),
    new Lemma("lexical comparison with a matching prefix (general)",
              "(FORALL (a i j k b ip jp kp) (IMPLIES (AND (<= i k) (<= ip kp) (< k j) (< kp jp) (|lexical-==| a i k b ip kp)) (IFF (|lexical-<| a i j b ip jp)(|lexical-<| a (+ k 1) j b (+ kp 1) jp))))"),
    new Lemma("lexical->= comparison to a singleton sequence",
              "(FORALL (a i j b ip) (IMPLIES (AND (EQ (select (select elems a) i) (select (select elems b) ip)) (>= j (+ i 1))) (|lexical->=| a i j b ip ip)))"),
    new Lemma("lexical-> comparison to a singleton sequence",
              "(FORALL (a i j b ip) (IMPLIES (AND (EQ (select (select elems a) i) (select (select elems b) ip)) (> j (+ i 1))) (|lexical->| a i j b ip ip)))"),
    new Lemma("lexical-== comparison to a singleton sequence",
              "(FORALL (a i j b ip) (IMPLIES (AND (EQ (select (select elems a) i) (select (select elems b) ip)) (> j (+ i 1))) (|lexical-==| a i j b ip ip)))"),
    new Lemma("the empty sequence is less than any non-empty sequence",
              "(FORALL (a i j b ip jp) (IMPLIES (AND (<= 0 i) (<= 0 ip) (< j (arrayLength a)) (< jp (arrayLength b)) (< j i) (>= jp ip)) (|lexical-<| a i j b ip jp)))"),
    new Lemma("lexical comparison with matching prefix (one-way)",
              "(FORALL (a i j k b ip jp kp) (IMPLIES (AND (<= i k) (<= ip kp) (< k j) (< kp jp) (|lexical-==| a i k b ip kp)) (IMPLIES (< (select (select elems a) (+ k 1)) (select (select elems b) (+ kp 1))) (|lexical-<| a i j b ip jp))))"),
    new Lemma("lexical comparison with matching prefix (one-way, reindexed)",
              "(FORALL (a i j k b ip jp kp) (IMPLIES (AND (<= i k) (<= ip kp) (<= k j) (<= kp jp) (|lexical-==| a i (- k 1) b ip (- kp 1))) (IMPLIES (< (select (select elems a) k) (select (select elems b) kp))(|lexical-<| a i j b ip jp))))"),
    new Lemma("lexical comparison with matching prefix (and matching indexes)",
              "(FORALL (a i j b jp) (IMPLIES (AND (< i j) (<= j jp) (|lexical-==| a i (- j 1) b i (- j 1)) (< (select (select elems a) j) (select (select elems b) j))) (|lexical-<| a i j b i jp)))"),
// ;; (BG_PUSH
// ;;  (FORALL (a i j k b ip jp kp)
// ;;    (IMPLIES (AND (<= i k) (<= ip kp) (EQ k j) (< kp jp)
// ;;            (|lexical-==| a i k b ip kp))
// ;;       (|lexical-<| a i j b ip jp))))
//  A simplifed version of the above, specialized to matching indexes
    new Lemma("comparison with a strict prefix (matching indexes)",
              "(FORALL (a i j b jp) (IMPLIES (AND (< jp (arrayLength b)) (< j jp) (|lexical-==| a i j b i j)) (|lexical-<| a i j b i jp)))"),
    new Lemma("lexical equality of singleton sequences",
              "(FORALL (a i b ip) (IFF (|lexical-==| a i i b ip ip) (EQ (select (select elems a) i) (select (select elems b) ip))))"),
    new Lemma("lexical-< of singleton sequences",
              "(FORALL (a i b ip) (IFF (|lexical-<| a i i b ip ip) (< (select (select elems a) i) (select (select elems b) ip))))"),
    new Lemma("lexical-< by < of initial elements",
              "(FORALL (a i j b ip jp) (IMPLIES (AND (<= 0 i) (<= 0 ip) (<= i j) (<= ip jp) (< j (arrayLength a)) (< jp (arrayLength b)) (< (select (select elems a) i) (select (select elems b) ip))) (|lexical-<| a i j b ip jp)))"),
    new Lemma("elementwise <= implies lexical-<=",
              "(FORALL (a i j b ip jp) (IMPLIES (AND (EQ (- j i) (- jp ip)) (FORALL (x y) (IMPLIES (AND (<= i x) (<= x j)(<= ip y) (<= y jp) (EQ (- x i) (- y ip))) (<= (select (select elems a) x) (select (select elems b) y))))) (|lexical-<=| a i j b ip jp)))"),
    new Lemma("definition of subsequence in terms of lexical-==",
              "(FORALL (a start end b i j) (IFF (subsequence a start end b i j) (OR (EQ start (+ end 1)) (EXISTS (ip jp) (AND (<= i ip) (<= ip jp) (<= jp j) (|lexical-==| a start end b ip jp))))))"),
    new Lemma("definition of is-reverse-of",
              "(FORALL (a i j b ip jp) (IFF (|is-reverse-of| a i j b ip jp) (AND (EQ (- j i) (- jp ip)) (<= 0 i) (< j (arrayLength a)) (<= 0 ip) (< jp (arrayLength b)) (FORALL (x) (IMPLIES (AND (<= 0 x) (< x (- j i))) (EQ (select (select elems a) (+ i x)) (select (select elems b) (- jp x))))))))"),
    new Lemma("definition of subset",
              "(FORALL (a i j b ip jp) (IFF (subset a i j b ip jp) (FORALL (x) (IMPLIES (AND (<= i x) (<= x j)) (EXISTS (y) (AND (<= ip y) (<= y jp) (EQ (select (select elems a) x) (select (select elems b) y))))))))"),
    new Lemma("when && on integers is 1",
              "(FORALL (x y) (IFF (EQ (|java-&&| x y) 1) (AND (NEQ x 0) (NEQ y 0))))"),
    new Lemma("when && on integers is 0",
              "(FORALL (x y) (IFF (EQ (|java-&&| x y) 0) (NOT (AND (NEQ x 0) (NEQ y 0)))))"),
    new Lemma("when || on integers is 1",
              "(FORALL (x y) (IFF (EQ (|java-logical-or| x y) 1) (OR (NEQ x 0) (NEQ y 0))))"),
    new Lemma("when || on integers is 0",
              "(FORALL (x y) (IFF (EQ (|java-logical-or| x y) 0) (NOT (OR (NEQ x 0) (NEQ y 0)))))"),
    // Some of the following lemmas about MOD (the ones with PATS)
    // were borrowed from the esc.ax file in the Simplify source
    new Lemma("relation between DIV and MOD",
              "(FORALL (x y) (PATS (DIV x y)) (EQ (+ (MOD x y) (* y (DIV x y))) x))"),
    new Lemma("MOD with positive modulus is non-negative",
              "(FORALL (x y) (PATS (MOD x y)) (IMPLIES (> y 0) (<= 0 (MOD x y))))"),
    new Lemma("MOD with positive modulus m is < m",
              "(FORALL (x y) (PATS (MOD x y)) (IMPLIES (> y 0) (< (MOD x y) y)))"),
    new Lemma("MOD with negative modulus m is > m",
              "(FORALL (x y) (PATS (MOD x y)) (IMPLIES (< y 0) (< y (MOD x y))))"),
    new Lemma("MOD with negative modulus is non-positive",
              "(FORALL (x y) (PATS (MOD x y)) (IMPLIES (< y 0) (<= (MOD x y) 0)))"),
    new Lemma("Removing + m inside MOD m (on right)",
              "(FORALL (x y) (PATS (MOD (+ x y) y)) (EQ (MOD (+ x y) y) (MOD x y)))"),
    new Lemma("Removing + m inside MOD m (on left)",
              "(FORALL (x y) (PATS (MOD (+ y x) y)) (EQ (MOD (+ y x) y) (MOD x y)))"),
    new Lemma("Removing - m inside MOD m",
              "(FORALL (x y) (PATS (MOD (- x y) y)) (EQ (MOD (+ y x) y) (MOD x y)))"),
    new Lemma("When MOD by a positive modulus is the identity",
              "(FORALL (m x) (IMPLIES (AND (> m 0) (>= x 0) (< x m)) (EQ (MOD x m) x)))"),
    new Lemma("A sum is even iff the terms have the same parity",
              "(FORALL (x y) (IFF (EQ (MOD (+ x y) 2) 0) (IFF (EQ (MOD x 2) 0) (EQ (MOD y 2) 0))))"),
    new Lemma("-1 is odd", "(EQ (MOD -1 2) 1)"),
//     new Lemma("x | (x + 1) == x + 1 if x is even",
//               "(FORALL (x) (IMPLIES (EQ (MOD x 2) 0) (EQ (+ x 1) (|java-bitwise-or| x (+ x 1)))))"),
//     new Lemma("x & (x + 1) == x if x is even",
//               "(FORALL (x) (IMPLIES (EQ (MOD x 2) 0) (EQ x (|java-&| x (+ x 1)))))"),
    // Facts about max and min, also from the Simplify source
    new Lemma("max(a,b) >= a",
              "(FORALL (a b) (PATS (max a b)) (>= (max a b) a))"),
    new Lemma("max(a,b) >= b",
              "(FORALL (a b) (PATS (max a b)) (>= (max a b) b))"),
    new Lemma("max(a,b) is either a or b",
              "(FORALL (a b) (PATS (max a b)) (OR (EQ (max a b) a) (EQ (max a b) b)))"),
    new Lemma("min(a,b) <= a",
              "(FORALL (a b) (PATS (min a b)) (<= (min a b) a))"),
    new Lemma("min(a,b) <= b",
              "(FORALL (a b) (PATS (min a b)) (<= (min a b) b))"),
    new Lemma("min(a,b) is either a or b",
              "(FORALL (a b) (PATS (min a b)) (OR (EQ (min a b) a) (EQ (min a b) b)))"),
    new Lemma("m > 0 and 0 < x < m => x % m = x",
              "(FORALL (m x) (IMPLIES (AND (> m 0) (< x m) (>= x (- 1 m))) (EQ (|java-%| x m) x)))"),
  };
}
