package org.checkerframework.checker.index.upperbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.PolyUpperBound;
import org.checkerframework.checker.index.qual.SubstringIndexFor;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.checker.index.qual.UpperBoundUnknown;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/**
 * Abstraction for Upper Bound annotations.
 *
 * <p>{@link UpperBoundUnknown} is modeled as {@link UpperBoundUnknownQualifier} and {@link
 * UpperBoundBottom} is modeled as {@link UBQualifier.UpperBoundBottomQualifier}.
 *
 * <p>{@link LTLengthOf} is modeled by {@link LessThanLengthOf}. {@link LTEqLengthOf} is equivalent
 * to @{@link LessThanLengthOf} with an offset of -1. {@link LTOMLengthOf} is equivalent to @{@link
 * LessThanLengthOf} with an offset of 1.
 */
public abstract class UBQualifier {

    /**
     * Create a UBQualifier from the given annotation.
     *
     * @param am the annotation to turn into a UBQualifier
     * @return a UBQualifier that represents the same information as the given annotation
     */
    public static UBQualifier createUBQualifier(AnnotationMirror am) {
        return createUBQualifier(am, null);
    }

    /**
     * Create a UBQualifier from the given annotation, with an extra offset.
     *
     * @param am the annotation to turn into a UBQualifier
     * @param offset the extra offset; may be null
     * @return a UBQualifier that represents the same information as the given annotation (plus an
     *     optional offset)
     */
    public static UBQualifier createUBQualifier(AnnotationMirror am, String offset) {
        if (AnnotationUtils.areSameByClass(am, UpperBoundUnknown.class)) {
            return UpperBoundUnknownQualifier.UNKNOWN;
        } else if (AnnotationUtils.areSameByClass(am, UpperBoundBottom.class)) {
            return UpperBoundBottomQualifier.BOTTOM;
        } else if (AnnotationUtils.areSameByClass(am, LTLengthOf.class)
                || AnnotationUtils.areSameByClass(am, SubstringIndexFor.class)) {
            return parseLTLengthOf(am, offset);
        } else if (AnnotationUtils.areSameByClass(am, LTEqLengthOf.class)) {
            return parseLTEqLengthOf(am, offset);
        } else if (AnnotationUtils.areSameByClass(am, LTOMLengthOf.class)) {
            return parseLTOMLengthOf(am, offset);
        } else if (AnnotationUtils.areSameByClass(am, PolyUpperBound.class)) {
            // TODO:  Ignores offset.  Should we check that offset is not set?
            return PolyQualifier.POLY;
        }
        assert false;
        return UpperBoundUnknownQualifier.UNKNOWN;
    }

    private static UBQualifier parseLTLengthOf(AnnotationMirror am, String extraOffset) {
        List<String> sequences =
                AnnotationUtils.getElementValueArray(am, "value", String.class, true);
        List<String> offset =
                AnnotationUtils.getElementValueArray(am, "offset", String.class, true);
        if (offset.isEmpty()) {
            offset = Collections.nCopies(sequences.size(), "");
        }
        return createUBQualifier(sequences, offset, extraOffset);
    }

    private static UBQualifier parseLTEqLengthOf(AnnotationMirror am, String extraOffset) {
        List<String> sequences =
                AnnotationUtils.getElementValueArray(am, "value", String.class, true);
        List<String> offset = Collections.nCopies(sequences.size(), "-1");
        return createUBQualifier(sequences, offset, extraOffset);
    }

    private static UBQualifier parseLTOMLengthOf(AnnotationMirror am, String extraOffset) {
        List<String> sequences =
                AnnotationUtils.getElementValueArray(am, "value", String.class, true);
        List<String> offset = Collections.nCopies(sequences.size(), "1");
        return createUBQualifier(sequences, offset, extraOffset);
    }

    public static UBQualifier createUBQualifier(String sequence, String offset) {
        return createUBQualifier(
                Collections.singletonList(sequence), Collections.singletonList(offset));
    }

    public static UBQualifier createUBQualifier(AnnotatedTypeMirror type, AnnotationMirror top) {
        return createUBQualifier(type.getEffectiveAnnotationInHierarchy(top));
    }

    /**
     * Creates an {@link UBQualifier} from the given sequences and offsets. The list of sequences
     * may not be empty. If the offsets list is empty, then an offset of 0 is used for each
     * sequence. If the offsets list is not empty, then it must be the same size as sequence.
     *
     * @param sequences non-empty list of sequences
     * @param offsets list of offset, if empty, an offset of 0 is used
     * @return an {@link UBQualifier} for the sequences with the given offsets
     */
    public static UBQualifier createUBQualifier(List<String> sequences, List<String> offsets) {
        return createUBQualifier(sequences, offsets, null);
    }

    /**
     * Creates an {@link UBQualifier} from the given sequences and offsets, with the given
     * additional offset. The list of sequences may not be empty. If the offsets list is empty, then
     * an offset of 0 is used for each sequence. If the offsets list is not empty, then it must be
     * the same size as sequence.
     *
     * @param sequences non-empty list of sequences
     * @param offsets list of offset, if empty, an offset of 0 is used
     * @param extraOffset offset to add to each element of offsets; may be null
     * @return an {@link UBQualifier} for the sequences with the given offsets
     */
    public static UBQualifier createUBQualifier(
            List<String> sequences, List<String> offsets, String extraOffset) {
        assert !sequences.isEmpty();

        OffsetEquation extraEq;
        if (extraOffset == null) {
            extraEq = OffsetEquation.ZERO;
        } else {
            extraEq = OffsetEquation.createOffsetFromJavaExpression(extraOffset);
            if (extraEq.hasError()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
        }

        Map<String, Set<OffsetEquation>> map = new HashMap<>();
        if (offsets.isEmpty()) {
            for (String sequence : sequences) {
                map.put(sequence, Collections.singleton(extraEq));
            }
        } else {
            assert sequences.size() == offsets.size();
            for (int i = 0; i < sequences.size(); i++) {
                String sequence = sequences.get(i);
                String offset = offsets.get(i);
                Set<OffsetEquation> set = map.get(sequence);
                if (set == null) {
                    set = new HashSet<>();
                    map.put(sequence, set);
                }
                OffsetEquation eq = OffsetEquation.createOffsetFromJavaExpression(offset);
                if (eq.hasError()) {
                    return UpperBoundUnknownQualifier.UNKNOWN;
                }
                eq = eq.copyAdd('+', extraEq);
                set.add(eq);
            }
        }
        return new LessThanLengthOf(map);
    }

    /**
     * Add the node as an offset to a copy of this qualifier. If this qualifier is UNKNOWN or
     * BOTTOM, then UNKNOWN is returned. Otherwise, see {@link LessThanLengthOf#plusOffset(int)} for
     * an explanation of how node is added as an offset.
     *
     * @param node a Node
     * @param factory an AnnotatedTypeFactory
     * @return a copy of this qualifier with node added as an offset
     */
    public UBQualifier plusOffset(Node node, UpperBoundAnnotatedTypeFactory factory) {
        return UpperBoundUnknownQualifier.UNKNOWN;
    }

    public UBQualifier plusOffset(int value) {
        return UpperBoundUnknownQualifier.UNKNOWN;
    }

    public UBQualifier minusOffset(Node node, UpperBoundAnnotatedTypeFactory factory) {
        return UpperBoundUnknownQualifier.UNKNOWN;
    }

    public UBQualifier minusOffset(int value) {
        return UpperBoundUnknownQualifier.UNKNOWN;
    }

    public boolean isLessThanLengthQualifier() {
        return false;
    }

    public boolean isUnknown() {
        return false;
    }

    public boolean isBottom() {
        return false;
    }

    /**
     * Return true if this is UBQualifier.PolyQualifier.
     *
     * @return true if this is UBQualifier.PolyQualifier
     */
    @Pure
    public boolean isPoly() {
        return false;
    }

    public abstract boolean isSubtype(UBQualifier superType);

    public abstract UBQualifier lub(UBQualifier other);

    public UBQualifier widenUpperBound(UBQualifier obj) {
        return lub(obj);
    }

    public abstract UBQualifier glb(UBQualifier other);

    /**
     * Is the value with this qualifier less than the length of sequence?
     *
     * @param sequence a String sequence
     * @return whether or not the value with this qualifier is less than the length of sequence
     */
    public boolean isLessThanLengthOf(String sequence) {
        return false;
    }

    /**
     * Is the value with this qualifier less than the length of any of the sequences?
     *
     * @param sequences list of sequences
     * @return whether or not the value with this qualifier is less than the length of any of the
     *     sequences
     */
    public boolean isLessThanLengthOfAny(List<String> sequences) {
        return false;
    }

    /**
     * Returns whether or not this qualifier has sequence with the specified offset.
     *
     * @param sequence sequence expression
     * @param offset the offset being looked for
     * @return whether or not this qualifier has sequence with the specified offset
     */
    public boolean hasSequenceWithOffset(String sequence, int offset) {
        return false;
    }

    /**
     * Returns whether or not this qualifier has sequence with the specified offset.
     *
     * @param sequence sequence expression
     * @param offset the offset being looked for
     * @return whether or not this qualifier has sequence with the specified offset
     */
    public boolean hasSequenceWithOffset(String sequence, String offset) {
        return false;
    }

    /**
     * Is the value with this qualifier less than or equal to the length of sequence?
     *
     * @param sequence a String sequence
     * @return whether or not the value with this qualifier is less than or equal to the length of
     *     sequence
     */
    public boolean isLessThanOrEqualTo(String sequence) {
        return false;
    }

    /** The less-than-length-of qualifier (@LTLengthOf). */
    public static class LessThanLengthOf extends UBQualifier {
        /** Maps from sequence name to offset. */
        private final Map<String, Set<OffsetEquation>> map;

        private LessThanLengthOf(Map<String, Set<OffsetEquation>> map) {
            assert !map.isEmpty();
            this.map = map;
        }

        @Override
        public boolean hasSequenceWithOffset(String sequence, int offset) {
            Set<OffsetEquation> offsets = map.get(sequence);
            if (offsets == null) {
                return false;
            }
            return offsets.contains(OffsetEquation.createOffsetForInt(offset));
        }

        @Override
        public boolean hasSequenceWithOffset(String sequence, String offset) {
            Set<OffsetEquation> offsets = map.get(sequence);
            if (offsets == null) {
                return false;
            }
            OffsetEquation target = OffsetEquation.createOffsetFromJavaExpression(offset);
            return offsets.contains(target);
        }

        /**
         * Is a value with this type less than or equal to the length of sequence?
         *
         * @param sequence a String sequence
         * @return true if a value with this type is less than or equal to the length of sequence
         */
        @Override
        public boolean isLessThanOrEqualTo(String sequence) {
            return isLessThanLengthOf(sequence) || hasSequenceWithOffset(sequence, -1);
        }

        /**
         * Is a value with this type less than the length of any of the sequences?
         *
         * @param sequences list of sequences
         * @return true if a value with this type is less than the length of any of the sequences
         */
        @Override
        public boolean isLessThanLengthOfAny(List<String> sequences) {
            for (String sequence : sequences) {
                if (isLessThanLengthOf(sequence)) {
                    return true;
                }
            }
            return false;
        }
        /**
         * Is a value with this type less than the length of the sequence?
         *
         * @param sequence a String sequence
         * @return true if a value with this type is less than the length of the sequence
         */
        @Override
        public boolean isLessThanLengthOf(String sequence) {
            Set<OffsetEquation> offsets = map.get(sequence);
            if (offsets == null) {
                return false;
            }
            if (offsets.isEmpty()) {
                return true;
            }
            for (OffsetEquation offset : offsets) {
                if (offset.isNonNegative()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the AnnotationMirror that represents this qualifier. If possible,
         * AnnotationMirrors using @{@link LTEqLengthOf} or @{@link LTOMLengthOf} are returned.
         * Otherwise, @{@link LTLengthOf} is used.
         *
         * <p>The returned annotation is canonicalized by sorting its arguments by sequence and then
         * offset. This is so that {@link AnnotationUtils#areSame(AnnotationMirror,
         * AnnotationMirror)} returns true for equivalent annotations.
         *
         * @param env a processing environment used to build the returned annotation
         * @return the AnnotationMirror that represents this qualifier
         */
        public AnnotationMirror convertToAnnotation(ProcessingEnvironment env) {
            return convertToAnnotation(env, false);
        }

        /**
         * Returns the @{@link SubstringIndexFor} AnnotationMirror from the Substring Index
         * hierarchy that imposes the same upper bounds on the annotated expression as this
         * qualifier. However, the upper bounds represented by this qualifier do not apply to the
         * value -1 which is always allowed by the returned annotation.
         *
         * @param env a processing environment used to build the returned annotation
         * @return the AnnotationMirror from the Substring Index hierarchy that represents the same
         *     upper bounds as this qualifier
         */
        public AnnotationMirror convertToSubstringIndexAnnotation(ProcessingEnvironment env) {
            return convertToAnnotation(env, true);
        }

        /**
         * Helper method called by {@link #convertToAnnotation} and {@link
         * convertToSubstringIndexAnnotation} that does the real work.
         *
         * @param env a processing environment used to build the returned annotation
         * @param buildSubstringIndexAnnotation if true, act like {@link
         *     #convertToSubstringIndexAnnotation} and return a @{@link SubstringIndexFor}
         *     annotation; if false, act like {@link #convertToAnnotation}
         * @return the AnnotationMirror that represents the same upper bounds as this qualifier
         */
        private AnnotationMirror convertToAnnotation(
                ProcessingEnvironment env, boolean buildSubstringIndexAnnotation) {
            List<String> sortedSequences = new ArrayList<>(map.keySet());
            Collections.sort(sortedSequences);
            List<String> sequences = new ArrayList<>();
            List<String> offsets = new ArrayList<>();
            boolean isLTEq = true;
            boolean isLTOM = true;
            for (String sequence : sortedSequences) {
                List<String> sortOffsets = new ArrayList<>();
                for (OffsetEquation eq : map.get(sequence)) {
                    isLTEq = isLTEq && eq.equals(OffsetEquation.NEG_1);
                    isLTOM = isLTOM && eq.equals(OffsetEquation.ONE);
                    sortOffsets.add(eq.toString());
                }
                Collections.sort(sortOffsets);
                for (String offset : sortOffsets) {
                    sequences.add(sequence);
                    offsets.add(offset);
                }
            }
            AnnotationBuilder builder;
            if (buildSubstringIndexAnnotation) {
                builder = new AnnotationBuilder(env, SubstringIndexFor.class);
                builder.setValue("value", sequences);
                builder.setValue("offset", offsets);
            } else if (isLTEq) {
                builder = new AnnotationBuilder(env, LTEqLengthOf.class);
                builder.setValue("value", sequences);
            } else if (isLTOM) {
                builder = new AnnotationBuilder(env, LTOMLengthOf.class);
                builder.setValue("value", sequences);
            } else {
                builder = new AnnotationBuilder(env, LTLengthOf.class);
                builder.setValue("value", sequences);
                builder.setValue("offset", offsets);
            }
            return builder.build();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LessThanLengthOf qualifier = (LessThanLengthOf) o;
            if (containsSame(map.keySet(), qualifier.map.keySet())) {
                for (Map.Entry<String, Set<OffsetEquation>> entry : map.entrySet()) {
                    Set<OffsetEquation> otherOffset = qualifier.map.get(entry.getKey());
                    Set<OffsetEquation> thisOffset = entry.getValue();
                    if (!containsSame(otherOffset, thisOffset)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        private static <T> boolean containsSame(Set<T> set1, Set<T> set2) {
            return set1.containsAll(set2) && set2.containsAll(set1);
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

        @Override
        public boolean isLessThanLengthQualifier() {
            return true;
        }

        /**
         * If superType is Unknown, return true. If superType is Bottom, return false.
         *
         * <p>Otherwise, return true if this qualifier contains all the sequences in superType, AND
         * for each of the offsets for each sequence in superType, there is an offset in this
         * qualifier for the sequence that is greater than or equal to the super offset.
         *
         * @param superType other qualifier
         * @return whether this qualifier is a subtype of superType
         */
        @Override
        public boolean isSubtype(UBQualifier superType) {
            if (superType.isUnknown()) {
                return true;
            } else if (superType.isBottom()) {
                return false;
            }

            LessThanLengthOf superTypeLTL = (LessThanLengthOf) superType;

            if (!map.keySet().containsAll(superTypeLTL.map.keySet())) {
                return false;
            }
            for (Map.Entry<String, Set<OffsetEquation>> entry : superTypeLTL.map.entrySet()) {
                String sequence = entry.getKey();
                Set<OffsetEquation> superOffsets = entry.getValue();
                Set<OffsetEquation> subOffsets = map.get(sequence);

                if (!isSubtypeOffset(subOffsets, superOffsets)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * One set of offsets is a subtype of another if for every superOffsets, at least one
         * suboffset is greater than or equal to the superOffset.
         */
        private boolean isSubtypeOffset(
                Set<OffsetEquation> subOffsets, Set<OffsetEquation> superOffsets) {
            for (OffsetEquation superOffset : superOffsets) {
                boolean oneIsSubtype = false;
                for (OffsetEquation subOffset : subOffsets) {
                    if (superOffset.lessThanOrEqual(subOffset)) {
                        oneIsSubtype = true;
                        break;
                    }
                }
                if (!oneIsSubtype) {
                    return false;
                }
            }
            return true;
        }

        /**
         * If other is Unknown, return Unknown. If other is Bottom, return this.
         *
         * <p>Otherwise lub is computed as follows:
         *
         * <p>1. Create the intersection of the sets of arrays for this and other.
         *
         * <p>2. For each sequence in the intersection, get the offsets for this and other. If any
         * offset in this is a less than or equal to an offset in other, then that offset is an
         * offset for the sequence in lub. If any offset in other is a less than or equal to an
         * offset in this, then that offset is an offset for the sequence in lub.
         *
         * @param other to lub with this
         * @return the lub
         */
        @Override
        public UBQualifier lub(UBQualifier other) {
            if (other.isUnknown()) {
                return other;
            } else if (other.isBottom()) {
                return this;
            }
            LessThanLengthOf otherLtl = (LessThanLengthOf) other;

            Set<String> sequences = new HashSet<>(map.keySet());
            sequences.retainAll(otherLtl.map.keySet());

            Map<String, Set<OffsetEquation>> lubMap = new HashMap<>();
            for (String sequence : sequences) {
                Set<OffsetEquation> lub = new HashSet<>();
                Set<OffsetEquation> offsets1 = map.get(sequence);
                Set<OffsetEquation> offsets2 = otherLtl.map.get(sequence);
                for (OffsetEquation offset1 : offsets1) {
                    for (OffsetEquation offset2 : offsets2) {
                        if (offset2.lessThanOrEqual(offset1)) {
                            lub.add(offset2);
                        } else if (offset1.lessThanOrEqual(offset2)) {
                            lub.add(offset1);
                        }
                    }
                }
                if (!lub.isEmpty()) {
                    lubMap.put(sequence, lub);
                }
            }
            if (lubMap.isEmpty()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            return new LessThanLengthOf(lubMap);
        }

        @Override
        public UBQualifier widenUpperBound(UBQualifier obj) {
            UBQualifier lub = lub(obj);
            if (!lub.isLessThanLengthQualifier() || !obj.isLessThanLengthQualifier()) {
                return lub;
            }
            Map<String, Set<OffsetEquation>> lubMap = ((LessThanLengthOf) lub).map;
            widenLub((LessThanLengthOf) obj, lubMap);
            if (lubMap.isEmpty()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            return new LessThanLengthOf(lubMap);
        }

        /**
         *
         *
         * <pre>@LTLengthOf("a") int i = ...;
         * while (expr) {
         *   i++;
         * }</pre>
         *
         * <p>Dataflow never stops analyzing the above loop, because the type of i always changes
         * after each analysis of the loop:
         *
         * <p>1. @LTLengthOf(value="a', offset="-1")
         *
         * <p>2. @LTLengthOf(value="a', offset="-2")
         *
         * <p>3. @LTLengthOf(value="a', offset="-3")
         *
         * <p>In order to prevent this, if both types passed to lub include all the same sequences
         * with the same non-constant value offsets and if the constant value offsets are different
         * then remove that sequence-offset pair from lub.
         *
         * <p>For example:
         *
         * <p>LUB @LTLengthOf(value={"a", "b"}, offset={"0", "0") and @LTLengthOf(value={"a", "b"},
         * offset={"-20", "0") is @LTLengthOf("b")
         *
         * <p>This widened lub should only be used in order to break dataflow analysis loops.
         */
        private void widenLub(LessThanLengthOf other, Map<String, Set<OffsetEquation>> lubMap) {
            if (!containsSame(this.map.keySet(), lubMap.keySet())
                    || !containsSame(other.map.keySet(), lubMap.keySet())) {
                return;
            }
            List<Pair<String, OffsetEquation>> remove = new ArrayList<>();
            for (Map.Entry<String, Set<OffsetEquation>> entry : lubMap.entrySet()) {
                String sequence = entry.getKey();
                Set<OffsetEquation> lubOffsets = entry.getValue();
                Set<OffsetEquation> thisOffsets = this.map.get(sequence);
                Set<OffsetEquation> otherOffsets = other.map.get(sequence);
                if (lubOffsets.size() != thisOffsets.size()
                        || lubOffsets.size() != otherOffsets.size()) {
                    return;
                }
                for (OffsetEquation lubEq : lubOffsets) {
                    if (lubEq.isInt()) {
                        int thisInt = OffsetEquation.getIntOffsetEquation(thisOffsets).getInt();
                        int otherInt = OffsetEquation.getIntOffsetEquation(otherOffsets).getInt();
                        if (thisInt != otherInt) {
                            remove.add(Pair.of(sequence, lubEq));
                        }
                    } else if (thisOffsets.contains(lubEq) && otherOffsets.contains(lubEq)) {
                        //  continue;
                    } else {
                        return;
                    }
                }
            }
            for (Pair<String, OffsetEquation> pair : remove) {
                Set<OffsetEquation> offsets = lubMap.get(pair.first);
                offsets.remove(pair.second);
                if (offsets.isEmpty()) {
                    lubMap.remove(pair.first);
                }
            }
        }

        @Override
        public UBQualifier glb(UBQualifier other) {
            if (other.isUnknown()) {
                return this;
            } else if (other.isBottom()) {
                return other;
            }
            LessThanLengthOf otherLtl = (LessThanLengthOf) other;

            Set<String> sequences = new HashSet<>(map.keySet());
            sequences.addAll(otherLtl.map.keySet());

            Map<String, Set<OffsetEquation>> glbMap = new HashMap<>();
            for (String sequence : sequences) {
                Set<OffsetEquation> glb = map.get(sequence);
                Set<OffsetEquation> otherglb = otherLtl.map.get(sequence);
                if (glb == null) {
                    glb = otherglb;
                } else if (otherglb != null) {
                    glb.addAll(otherglb);
                }
                glbMap.put(sequence, simplifyOffsets(glb));
            }
            return new LessThanLengthOf(glbMap);
        }

        /** Keeps only the largest offset equation that is only an int value. */
        private Set<OffsetEquation> simplifyOffsets(Set<OffsetEquation> offsets) {
            Set<OffsetEquation> newOff = new HashSet<>();
            OffsetEquation literal = null;
            for (OffsetEquation eq : offsets) {
                if (eq.isInt()) {
                    if (literal == null) {
                        literal = eq;
                    } else {
                        literal = literal.lessThanOrEqual(eq) ? eq : literal;
                    }
                } else {
                    newOff.add(eq);
                }
            }
            if (literal != null) {
                newOff.add(literal);
            }
            return newOff;
        }

        /**
         * Adds node as an offset to a copy of this qualifier. This is done by creating an offset
         * equation for node and then adding that equation to every offset equation in a copy of
         * this object.
         *
         * @param node a Node
         * @param factory an AnnotatedTypeFactory
         * @return a copy of this qualifier with node add as an offset
         */
        @Override
        public UBQualifier plusOffset(Node node, UpperBoundAnnotatedTypeFactory factory) {
            return pluseOrMinusOffset(node, factory, '+');
        }

        /**
         * Adds node as a negative offset to a copy of this qualifier. This is done by creating a
         * negative offset equation for node and then adding that equation to every offset equation
         * in a copy of this object.
         *
         * @param node a Node
         * @param factory an AnnotatedTypeFactory
         * @return a copy of this qualifier with node add as an offset
         */
        @Override
        public UBQualifier minusOffset(Node node, UpperBoundAnnotatedTypeFactory factory) {
            return pluseOrMinusOffset(node, factory, '-');
        }

        private UBQualifier pluseOrMinusOffset(
                Node node, UpperBoundAnnotatedTypeFactory factory, char op) {
            assert op == '-' || op == '+';

            OffsetEquation newOffset = OffsetEquation.createOffsetFromNode(node, factory, op);
            LessThanLengthOf nodeOffsetQualifier = null;
            if (!newOffset.hasError()) {
                nodeOffsetQualifier = (LessThanLengthOf) addOffset(newOffset);
            }

            OffsetEquation valueOffset =
                    OffsetEquation.createOffsetFromNodesValue(
                            node, factory.getValueAnnotatedTypeFactory(), op);
            LessThanLengthOf valueOffsetQualifier = null;
            if (valueOffset != null && !valueOffset.hasError()) {
                valueOffsetQualifier = (LessThanLengthOf) addOffset(valueOffset);
            }

            if (valueOffsetQualifier == null) {
                if (nodeOffsetQualifier == null) {
                    return UpperBoundUnknownQualifier.UNKNOWN;
                } else {
                    return nodeOffsetQualifier;
                }
            } else {
                if (nodeOffsetQualifier == null) {
                    return valueOffsetQualifier;
                } else {
                    return nodeOffsetQualifier.glb(valueOffsetQualifier);
                }
            }
        }

        /**
         * Adds value as an offset to a copy of this qualifier. This is done by adding value to
         * every offset equation in a copy of this object.
         *
         * @param value int value to add
         * @return a copy of this qualifier with value add as an offset
         */
        @Override
        public UBQualifier plusOffset(int value) {
            OffsetEquation newOffset = OffsetEquation.createOffsetForInt(value);
            return addOffset(newOffset);
        }

        /**
         * Adds the negation of value as an offset to a copy of this qualifier. This is done by
         * adding the negation of {@code value} to every offset equation in a copy of this object.
         *
         * @param value int value to add
         * @return a copy of this qualifier with value add as an offset
         */
        @Override
        public UBQualifier minusOffset(int value) {
            OffsetEquation newOffset = OffsetEquation.createOffsetForInt(-value);
            return addOffset(newOffset);
        }

        /**
         * Returns a copy of this qualifier with sequence-offset pairs where in the original the
         * offset contains an access of an sequence length in {@code sequences}. The sequence length
         * access has been removed from the offset. If the original qualifier has no sequence length
         * offsets, then UNKNOWN is returned.
         *
         * @param sequences access of the length of these sequences are removed
         * @return a copy of this qualifier with some offsets removed
         */
        public UBQualifier removeSequenceLengthAccess(final List<String> sequences) {
            if (sequences.isEmpty()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            OffsetEquationFunction removeSequenceLengthsFunc =
                    new OffsetEquationFunction() {
                        @Override
                        public OffsetEquation compute(OffsetEquation eq) {
                            return eq.removeSequenceLengths(sequences);
                        }
                    };
            return computeNewOffsets(removeSequenceLengthsFunc);
        }
        /**
         * Returns a copy of this qualifier with sequence-offset pairs where in the original the
         * offset contains an access of an sequence length in {@code sequences}. The sequence length
         * access has been removed from the offset. If the offset also has -1 then -1 is also
         * removed.
         *
         * @param sequences access of the length of these sequences are removed
         * @return a copy of this qualifier with some offsets removed
         */
        public UBQualifier removeSequenceLengthAccessAndNeg1(final List<String> sequences) {
            if (sequences.isEmpty()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            OffsetEquationFunction removeSequenceLenFunc =
                    new OffsetEquationFunction() {
                        @Override
                        public OffsetEquation compute(OffsetEquation eq) {
                            OffsetEquation newEq = eq.removeSequenceLengths(sequences);
                            if (newEq == null) {
                                return null;
                            }
                            if (newEq.getInt() == -1) {
                                return newEq.copyAdd('+', OffsetEquation.ONE);
                            }
                            return newEq;
                        }
                    };
            return computeNewOffsets(removeSequenceLenFunc);
        }

        private UBQualifier addOffset(final OffsetEquation newOffset) {
            OffsetEquationFunction addOffsetFunc =
                    new OffsetEquationFunction() {
                        @Override
                        public OffsetEquation compute(OffsetEquation eq) {
                            return eq.copyAdd('+', newOffset);
                        }
                    };
            return computeNewOffsets(addOffsetFunc);
        }

        /**
         * If divisor == 1, return this object.
         *
         * <p>If divisor greater than 1, then return a copy of this object keeping only sequences
         * and offsets where the offset is less than or equal to zero.
         *
         * <p>Otherwise, return UNKNOWN.
         *
         * @param divisor number to divide by
         * @return the result of dividing a value with this qualifier by divisor
         */
        public UBQualifier divide(int divisor) {
            if (divisor == 1) {
                return this;
            } else if (divisor > 1) {
                OffsetEquationFunction divideFunc =
                        new OffsetEquationFunction() {
                            @Override
                            public OffsetEquation compute(OffsetEquation eq) {
                                if (eq.isNegativeOrZero()) {
                                    return eq;
                                }
                                return null;
                            }
                        };
                return computeNewOffsets(divideFunc);
            }
            return UpperBoundUnknownQualifier.UNKNOWN;
        }

        public boolean isValuePlusOffsetLessThanMinLen(String sequence, long value, int minlen) {
            Set<OffsetEquation> offsets = map.get(sequence);
            if (offsets == null) {
                return false;
            }
            for (OffsetEquation offset : offsets) {
                if (offset.isInt()) {
                    // This expression must not overflow
                    return (long) minlen - offset.getInt() > value;
                }
            }
            return false;
        }

        /**
         * Checks whether replacing sequence with replacementSequence in this qualifier creates
         * replacementSequence entry in other.
         */
        public boolean isValidReplacement(
                String sequence, String replacementSequence, LessThanLengthOf other) {
            Set<OffsetEquation> offsets = map.get(sequence);
            if (offsets == null) {
                return false;
            }
            Set<OffsetEquation> otherOffsets = other.map.get(replacementSequence);
            if (otherOffsets == null) {
                return false;
            }
            return containsSame(offsets, otherOffsets);
        }

        @Override
        public String toString() {
            return "LessThanLengthOf{" + "map=" + map + '}';
        }

        public Iterable<? extends String> getSequences() {
            return map.keySet();
        }

        /** Generates a new UBQualifer without the given sequence and offset. */
        public UBQualifier removeOffset(String sequence, int offset) {
            OffsetEquation offsetEq = OffsetEquation.createOffsetForInt(offset);
            List<String> sequences = new ArrayList<>();
            List<String> offsets = new ArrayList<>();
            for (String seq : this.map.keySet()) {
                Set<OffsetEquation> offsetSet = this.map.get(seq);
                for (OffsetEquation off : offsetSet) {
                    if (!sequence.equals(seq) && !off.equals(offsetEq)) {
                        sequences.add(seq);
                        offsets.add(off.toString());
                    }
                }
            }
            if (sequences.isEmpty()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            } else {
                return UBQualifier.createUBQualifier(sequences, offsets);
            }
        }

        /** Functional interface that operates on {@link OffsetEquation}s. */
        interface OffsetEquationFunction {
            /**
             * Returns the result of the computation or null if the passed equation should be
             * removed.
             *
             * @param eq current offset equation
             * @return the result of the computation or null if the passed equation should be
             *     removed
             */
            OffsetEquation compute(OffsetEquation eq);
        }

        /**
         * Returns a new qualifier that is a copy of this qualifier with the OffsetEquationFunction
         * applied to each offset.
         *
         * <p>If the {@link OffsetEquationFunction} returns null, it's not added as an offset. If
         * after all functions have been applied, an sequence has no offsets, then that sequence is
         * not added to the returned qualifier. If no sequences are added to the returned qualifier,
         * then UNKNOWN is returned.
         *
         * @param f function to apply
         * @return a new qualifier that is a copy of this qualifier with the OffsetEquationFunction
         *     applied to each offset
         */
        private UBQualifier computeNewOffsets(OffsetEquationFunction f) {
            Map<String, Set<OffsetEquation>> newMap = new HashMap<>(map.size());
            for (Map.Entry<String, Set<OffsetEquation>> entry : map.entrySet()) {
                Set<OffsetEquation> offsets = new HashSet<>(entry.getValue().size());
                for (OffsetEquation eq : entry.getValue()) {
                    OffsetEquation newEq = f.compute(eq);
                    if (newEq != null) {
                        offsets.add(newEq);
                    }
                }
                if (!offsets.isEmpty()) {
                    newMap.put(entry.getKey(), offsets);
                }
            }
            if (newMap.isEmpty()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            return new LessThanLengthOf(newMap);
        }
    }

    public static class UpperBoundUnknownQualifier extends UBQualifier {
        static final UBQualifier UNKNOWN = new UpperBoundUnknownQualifier();

        private UpperBoundUnknownQualifier() {}

        @Override
        public boolean isSubtype(UBQualifier superType) {
            return superType.isUnknown();
        }

        @Override
        public boolean isUnknown() {
            return true;
        }

        @Override
        public UBQualifier lub(UBQualifier other) {
            return this;
        }

        @Override
        public UBQualifier glb(UBQualifier other) {
            return other;
        }

        @Override
        public String toString() {
            return "UNKNOWN";
        }
    }

    private static class UpperBoundBottomQualifier extends UBQualifier {
        static final UBQualifier BOTTOM = new UpperBoundBottomQualifier();

        @Override
        public boolean isBottom() {
            return true;
        }

        @Override
        public boolean isSubtype(UBQualifier superType) {
            return true;
        }

        @Override
        public UBQualifier lub(UBQualifier other) {
            return other;
        }

        @Override
        public UBQualifier glb(UBQualifier other) {
            return this;
        }

        @Override
        public String toString() {
            return "BOTTOM";
        }
    }

    private static class PolyQualifier extends UBQualifier {
        static final UBQualifier POLY = new PolyQualifier();

        @Override
        @Pure
        public boolean isPoly() {
            return true;
        }

        @Override
        public boolean isSubtype(UBQualifier superType) {
            return superType.isUnknown() || superType.isPoly();
        }

        @Override
        public UBQualifier lub(UBQualifier other) {
            if (other.isPoly() || other.isBottom()) {
                return this;
            }
            return UpperBoundUnknownQualifier.UNKNOWN;
        }

        @Override
        public UBQualifier glb(UBQualifier other) {
            if (other.isPoly() || other.isUnknown()) {
                return this;
            }
            return UpperBoundBottomQualifier.BOTTOM;
        }
    }
}
