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
        if (!bitsets.containsKey(key))
            throw new IllegalArgumentException();
        return bitsets.get(key).get(index);
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

    /**
     * Merges each gen-kill set in this group with the one corresponding to the
     * same key in {@code other} via boolean "and" on each bit. Modifies this
     * gen-kill set.
     *
     * @param other the group to "and" with
     * @throws IllegalArgumentException if the other group is missing a key from
     *         this group
     */
    /*
    public void and(GenKillBits<K> other) {
        for (K key : bitsets.keySet()) {
            if (!other.bitsets.containsKey(key))
                throw new IllegalArgumentException();
            bitsets.get(key).and(other.bitsets.get(key));
        }
    }*/

    /**
     * Merges each gen-kill set in this group with the one corresponding to the
     * same key in {@code other} via boolean "or" on each bit. Modifies this
     * gen-kill set.
     *
     * @param other the group to "or" with
     * @throws IllegalArgumentException if the other group is missing a key from
     *         this group
     */
    /*
    public void or(GenKillBits<K> other) {
        for (K key : bitsets.keySet()) {
            if (!other.bitsets.containsKey(key))
                throw new IllegalArgumentException();
            bitsets.get(key).or(other.bitsets.get(key));
        }
    }*/

    public String toString() {
        return "[GenKill: " + bitsets + "]";
    }

	public static void andWMD(GenKillBits<AnnotationMirror> outarg1,
			GenKillBits<AnnotationMirror> arg2, QualifierHierarchy annoRelations) {
		// outarg1.and(arg2);
		
		for (AnnotationMirror key1 : outarg1.bitsets.keySet()) {
			if (!arg2.bitsets.containsKey(key1))
				throw new IllegalArgumentException();

			for(AnnotationMirror key2 : arg2.bitsets.keySet()) {
				BitSet lhs = outarg1.bitsets.get(key1);
				BitSet rhs = arg2.bitsets.get(key2);
				
				int length = lhs.length();
				if(rhs.length() > length) length = rhs.length();
				
				for(int var=0; var < length; ++var) {
					if( lhs.get(var) && rhs.get(var) ) {
						AnnotationMirror lub = annoRelations.leastUpperBound(key1, key2);
						lhs.clear(var);
						outarg1.bitsets.get(lub).set(var);
					}
				}
			}
		}
	}
	
	
	public static void orWMD(GenKillBits<AnnotationMirror> outarg1,
			GenKillBits<AnnotationMirror> arg2, QualifierHierarchy annoRelations) {
		// outarg1.or(arg2);
		
		for (AnnotationMirror key1 : outarg1.bitsets.keySet()) {
			if (!arg2.bitsets.containsKey(key1))
				throw new IllegalArgumentException();

			for(AnnotationMirror key2 : arg2.bitsets.keySet()) {
				BitSet lhs = outarg1.bitsets.get(key1);
				BitSet rhs = arg2.bitsets.get(key2);
				
				int length = lhs.length();
				if(rhs.length() > length) length = rhs.length();
				
				for(int var=0; var < length; ++var) {
					if( lhs.get(var) || rhs.get(var) ) {
						AnnotationMirror glb = annoRelations.greatestLowerBound(key1, key2);
						lhs.clear(var);
						outarg1.bitsets.get(glb).set(var);
					}
				}
			}
		}
	}
}