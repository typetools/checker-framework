package checkers.flow;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;

import checkers.types.QualifierHierarchy;

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
        for (K key : keys)
            bitsets.put(key, new BitSet());
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
            BitSet newbits = (BitSet) bitsets.get(key).clone();
            bitsets.put(key, newbits);
        }
    }

    /**
     * Sets the bit (gen) for the key at the specified index.
     *
     * @param key the key for which the bit should be set
     * @param index the index at which to set the bit
     * @throws IllegalArgumentException if the key is not one of the keys for
     *         this group
     */
    public void set(K key, int index) {
        if (!bitsets.containsKey(key))
            throw new IllegalArgumentException();
        bitsets.get(key).set(index);
    }

    /**
     * Retrieves the bit for the key at the specified index.
     *
     * @param key
     * @param index
     * @return the value of the bit for the key at the index
     * @throws IllegalArgumentException if the key is not one of the keys for
     *         this group
     */
    public boolean get(K key, int index) {
    	// System.err.println("Valid in get: " + key + " idx: " + index);
    	valid();
    	
        if (!bitsets.containsKey(key))
            throw new IllegalArgumentException();
        return bitsets.get(key).get(index);
    }

    public boolean contains(K key) {
    	return bitsets.containsKey(key);
    }
    
    /**
     * Clears the bit (kill) for the key at the specified index.
     *
     * @param key the key for which the bit should be set
     * @param index the index at which to set the bit
     * @throws IllegalArgumentException if the key is not one of the keys for
     *         this group
     */
    public void clear(K key, int index) {
        if (!bitsets.containsKey(key))
            throw new IllegalArgumentException();
        bitsets.get(key).clear(index);
    }

    public String toString() {
        return "[GenKill: " + bitsets + "]";
    }

    /**
     * Merges each gen-kill set in this group with the one corresponding to the
     * same key in {@code other} via boolean "and" on each bit. Modifies this
     * gen-kill set.
     * TODO: lub
     * 
     * @param other the group to "and" with
     * @throws IllegalArgumentException if the other group is missing a key from
     *         this group
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
			for (AnnotationMirror key1 : outarg1.bitsets.keySet()) {
				if (!arg2.bitsets.containsKey(key1))
					throw new IllegalArgumentException();
				BitSet lhs = outarg1.bitsets.get(key1);
				boolean notfound = true;

				for (AnnotationMirror key2 : arg2.bitsets.keySet()) {
					BitSet rhs = arg2.bitsets.get(key2);

					if (lhs.get(var) && rhs.get(var)) {
						AnnotationMirror lub = annoRelations.leastUpperBound(
								key1, key2);
						lhs.clear(var);
						outarg1.bitsets.get(lub).set(var);
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
     * Merges each gen-kill set in this group with the one corresponding to the
     * same key in {@code other} via boolean "or" on each bit. Modifies this
     * gen-kill set.
     * TODO: lub.
     * 
     * @param other the group to "or" with
     * @throws IllegalArgumentException if the other group is missing a key from
     *         this group
     */	
	public static void orlub(GenKillBits<AnnotationMirror> outarg1,
			GenKillBits<AnnotationMirror> arg2, QualifierHierarchy annoRelations) {
		// System.err.print("Valid in orlub outarg1 ");
		outarg1.valid();
		// System.err.print("Valid in orlub arg2");
		arg2.valid();
		// System.err.println("");
		
		for (AnnotationMirror key1 : outarg1.bitsets.keySet()) {
			if (!arg2.bitsets.containsKey(key1))
				throw new IllegalArgumentException();

			for(AnnotationMirror key2 : arg2.bitsets.keySet()) {
				BitSet lhs = outarg1.bitsets.get(key1);
				BitSet rhs = arg2.bitsets.get(key2);
				
				int length = lhs.length();
				if(rhs.length() > length) length = rhs.length();
				
				for(int var=0; var < length; ++var) {
					if ( rhs.get(var) ) {
						if( lhs.get(var) ) {
							AnnotationMirror glb = annoRelations.leastUpperBound(key1, key2);
							lhs.clear(var);
							outarg1.bitsets.get(glb).set(var);
						} else {
							/* If the rhs has the bit set, but the lhs has not, there _might_ be a different
							 * modifier in the lhs that already has the bit set.
							 * If we find it, remove it and set the lub.
							 * If we do not find it, set key2.
							 */
							boolean found = false;
							for (AnnotationMirror key3 : outarg1.bitsets.keySet()) {
								if ( outarg1.bitsets.get(key3).get(var) ) {
									AnnotationMirror glb = annoRelations.leastUpperBound(key3, key2);
									if (!glb.equals(key3) ) {
										outarg1.bitsets.get(key3).clear(var);
										outarg1.bitsets.get(glb).set(var);
									}
									found = true;
									break;
								}
							}
							if (!found) {
								// we do not need to calculate a lub, because the variable is not set on
								// the lhs and there is no other modifier that has the bit set.
								outarg1.bitsets.get(key2).set(var);
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