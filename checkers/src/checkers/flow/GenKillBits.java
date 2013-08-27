package checkers.flow;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;

import checkers.source.SourceChecker;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;

/**
 * Maintains multiple gen-kill sets, "keyed" by a value. For instance, the
 * flow-sensitive inference implemented in {@link Flow} uses a
 * {@link GenKillBits} keyed by {@link AnnotationMirror}s to simultaneously
 * track the gen-kill sets of multiple type qualifiers.
 *
 * <p>
 *
 * This class is essentially an abstraction for maintaining and combining
 * multiple simultaneous bit vectors.
 *
 * @param <K> the type of the key
 */
public class GenKillBits<K> {

  private final Map<K, BitSet> bitsets;

  /**
   * Creates a new {@link GenKillBits} that is a deep copy of the
   * {@link GenKillBits} passed as an argument.
   *
   * @param <K> the key type of the group
   * @param other the {@link GenKillBits} to copy
   * @return a deep copy of the contents of {@code g}
   *
   * @see GenKillBits#GenKillBits(GenKillBits)
   */
  public static <K> GenKillBits<K> copy(GenKillBits<K> other) {
    if (other == null)
      return null;
    else
      return new GenKillBits<K>(other);
  }

  /**
   * Creates a new {@link GenKillBits} with the specified set of keys.
   * (Once specified, the keys may not be changed.)
   *
   * @param keys the keys for the {@link GenKillBits}
   */
  public GenKillBits(Collection<K> keys) {
    bitsets = new HashMap<K, BitSet>(keys.size());
    for (K key : keys) {
      if (key==null) {
        SourceChecker.errorAbort("GenKillBits(keys): No null keys allowed!");
      }
      bitsets.put(key, new BitSet());
    }
  }

  /**
   * Creates a new {@link GenKillBits} that is a deep copy of the
   * {@link GenKillBits} passed as an argument.
   *
   * @param other the {@link GenKillBits} to copy
   *
   * @see GenKillBits#copy(GenKillBits)
   */
  public GenKillBits(GenKillBits<K> other) {
    // System.err.println("Valid in constructor:");
    other.valid();

    bitsets = new HashMap<K, BitSet>(other.bitsets);
    for (K key : bitsets.keySet()) {
      if (key==null) {
        SourceChecker.errorAbort("GenKillBits(other): No null keys allowed!");
      }
      BitSet newbits = (BitSet) bitsets.get(key).clone();
      bitsets.put(key, newbits);
    }
  }

  /**
   * Sets the bit (gen) for the key at the specified index. Adds the key if it
   * does not already exist.
   *
   * @param key the key for which the bit should be set
   * @param index the index at which to set the bit
   */
  public void set(K key, int index) {
    if (key==null) {
      SourceChecker.errorAbort("GenKillBits.set: No null keys allowed!");
    }
    if (!bitsets.containsKey(key)) {
      bitsets.put(key, new BitSet());
    }
    bitsets.get(key).set(index);
  }

  /**
   * Retrieves the bit for the key at the specified index.
   *
   * @param key
   * @param index
   * @return the value of the bit for the key at the index or false if the key
   *         does not exist.
   */
  public boolean get(K key, int index) {
    // System.err.println("Valid in get: " + key + " idx: " + index);
    valid();

    if (bitsets.containsKey(key))
      return bitsets.get(key).get(index);
    return false;
  }

  public boolean contains(K key) {
    return bitsets.containsKey(key);
  }

  /**
   * Clears the bit (kill) for the key at the specified index. Does nothing if
   * the key does not exist.
   *
   * @param key the key for which the bit should be set
   * @param index the index at which to set the bit
   */
  public void clear(K key, int index) {
    if (bitsets.containsKey(key))
      bitsets.get(key).clear(index);
  }

  /**
   * For all keys, clear the bit at the given index.
   */
  public void clearInAll(int index) {
    for(K key : bitsets.keySet()) {
      bitsets.get(key).clear(index);
    }
  }

  @Override
  public String toString() {
    return "[GenKill: " + bitsets + "]";
  }

  /**
   * Merges each gen-kill set in outarg1 with the one corresponding to the
   * same key in {@code arg2} via boolean "and" on each bit. Modifies outarg1's
   * gen-kill set. If arg2 is missing a key in outarg1 then an empty set is
   * added to arg2 for the key.
   * TODO: lub
   *
   * @param outarg1 the group to modify
   * @param arg2 the group to "and" with
   */
  public static void andlub(GenKillBits<AnnotationMirror> outarg1,
                            GenKillBits<AnnotationMirror> arg2, QualifierHierarchy annoRelations) {
    // System.err.print("Valid in andlub outarg1 ");
    outarg1.valid();
    // System.err.print("Valid in andlub arg2 ");
    arg2.valid();
    // System.err.println("");

    // TODO: compute this once somewhere
    int length = 0;
    for (BitSet bs : outarg1.bitsets.values()) {
      if (bs.length()>length) length = bs.length();
    }
    for (BitSet bs : arg2.bitsets.values()) {
      if (bs.length()>length) length = bs.length();
    }

    for (int var = 0; var < length; ++var) {
      // Make a clone of the keySet so we can add new keys to outarg1.bitsets
      // if needed without a ConcurrentModificationException. This is important
      // for annotations with values, where the lub we get is an annotation that
      // we haven't seen yet.
      Set<AnnotationMirror> arg1KeySet = AnnotationUtils.createAnnotationSet();
      arg1KeySet.addAll(outarg1.bitsets.keySet());
      for (AnnotationMirror key1 : arg1KeySet) {
        if (!arg2.bitsets.containsKey(key1))
          arg2.bitsets.put(key1, new BitSet());
        BitSet lhs = outarg1.bitsets.get(key1);
        boolean notfound = true;

        for (AnnotationMirror key2 : arg2.bitsets.keySet()) {
          BitSet rhs = arg2.bitsets.get(key2);

          if (lhs.get(var) && rhs.get(var)) {
            AnnotationMirror lub = annoRelations.leastUpperBound(key1, key2);
            if (lub==null) {
              continue;
            }
            lhs.clear(var);
            outarg1.set(lub, var);
            notfound = false;
          }
        }

        if (notfound) {
          lhs.clear(var);
        }
      }
    }
    // System.err.print("Valid in andlub result ");
    outarg1.valid();
    // System.err.println("");
  }

  /**
   * Merges each gen-kill set outarg1 with the one corresponding to the
   * same key in {@code arg2} via boolean "or" on each bit. Modifies outarg1
   * gen-kill set. If arg2 is missing a key in outarg1 then an empty set is
   * added to arg2 for the key.
   * TODO: lub.
   *
   * @param outarg1 the group to modify
   * @param arg2 the group to "or" with
   */
  public static void orlub(GenKillBits<AnnotationMirror> outarg1,
                           GenKillBits<AnnotationMirror> arg2, QualifierHierarchy annoRelations) {
    // System.err.print("Valid in orlub outarg1 ");
    outarg1.valid();
    // System.err.print("Valid in orlub arg2");
    arg2.valid();
    // System.err.println("");

    // Copy the keySet so it can be modified without a ConcurrentModificationException.
    // This is important for annotations with values where an annotation with a value
    // not seen yet may need to be added.
    Set<AnnotationMirror> arg1KeySet = AnnotationUtils.createAnnotationSet();
    arg1KeySet.addAll(outarg1.bitsets.keySet());
    for (AnnotationMirror key1 : arg1KeySet) {
      if (!arg2.bitsets.containsKey(key1))
        arg2.bitsets.put(key1, new BitSet());

      for(AnnotationMirror key2 : arg2.bitsets.keySet()) {
        BitSet lhs = outarg1.bitsets.get(key1);
        BitSet rhs = arg2.bitsets.get(key2);

        int length = lhs.length();
        if(rhs.length() > length) length = rhs.length();

        for(int var=0; var < length; ++var) {
          if ( rhs.get(var) ) {
            if( lhs.get(var) ) {
              AnnotationMirror glb = annoRelations.leastUpperBound(key1, key2);
              if (glb==null) {
                continue;
              }
              lhs.clear(var);
              outarg1.set(glb, var);
            } else {
              /* If the rhs has the bit set, but the lhs has not, there _might_ be a different
               * modifier in the lhs that already has the bit set.
               * If we find it, remove it and set the lub.
               * If we do not find it, set key2.
               */
              boolean found = false;

              // Copy the keySet so it can be modified without a ConcurrentModificationException.
              // This is important for annotations with values where an annotation with a value
              // not seen yet may need to be added.
              Set<AnnotationMirror> arg1KeySet2 = AnnotationUtils.createAnnotationSet();
              arg1KeySet2.addAll(outarg1.bitsets.keySet());
              for (AnnotationMirror key3 : arg1KeySet2) {
                if ( outarg1.bitsets.get(key3).get(var) ) {
                  AnnotationMirror glb = annoRelations.leastUpperBound(key3, key2);
                  if (glb==null) {
                      continue;
                  }
                  if (!glb.equals(key3) ) {
                    outarg1.bitsets.get(key3).clear(var);
                    outarg1.set(glb, var);
                  }
                  found = true;
                  break;
                }
              }
              if (!found) {
                // we do not need to calculate a lub, because the variable is not set on
                // the lhs and there is no other modifier that has the bit set.
                outarg1.set(key2, var);
              }
            }
          }
        }
      }
    }
    // System.err.print("Valid in orlub result ");
    outarg1.valid();
    // System.err.println("");
  }

  public boolean valid() {
    BitSet xorres = new BitSet();
    BitSet orres = new BitSet();

    for (K key : bitsets.keySet()) {
      xorres.xor(bitsets.get(key));
      orres.or(bitsets.get(key));
    }

    // Careful: take the length of the orres! Otherwise you might miss
    // elements at the end, b/c length() doesn't count zeros at the end!
    for(int i=0; i<orres.length(); ++i) {
      if (orres.get(i) && !xorres.get(i)) {
        // System.err.println("More than one variable true: " + this);
        return false;
      }
    }
    return true;
  }
}
