package org.checkerframework.checker.index.samelen;

import static org.checkerframework.checker.index.IndexUtil.getValueOfAnnotationWithStringArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.upperbound.OffsetEquation;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/** Analogue of {@link org.checkerframework.checker.index.upperbound.UBQualifier} for SameLen. */
public class SameLenQualifier {
    private final Map<String, OffsetEquation> offsets;

    /**
     * Produces a SameLen qualifier with the given list of sequences and offsets. The lists must
     * either be the same length, or the second list must be empty (in which case it is populated
     * with all zeroes).
     *
     * <p>May return null if the sequence list is empty.
     */
    public static SameLenQualifier of(List<String> sequenceList, List<String> offsetList) {
        if (offsetList.size() == 0) {
            offsetList = Collections.nCopies(sequenceList.size(), "0");
        }
        return new SameLenQualifier(sequenceList, offsetList);
    }

    /** Produces a SameLen qualifier with the given list of sequences. */
    public static SameLenQualifier of(List<String> sequenceList) {
        return of(sequenceList, Collections.nCopies(sequenceList.size(), "0"));
    }

    /** Produces a SameLen qualifier with the given sequence. */
    public static SameLenQualifier of(String sequence) {
        return of(Collections.singletonList(sequence));
    }

    /**
     * Produces a SameLen qualifier from the given annotation mirror. If the annotation mirror is
     * not an instance of {@link SameLen}, this method will throw an exception.
     */
    public static SameLenQualifier of(AnnotationMirror annotationMirror) {
        if (AnnotationUtils.areSameByClass(annotationMirror, SameLen.class)) {
            List<String> offsets =
                    AnnotationUtils.getElementValueArray(
                            annotationMirror, "offset", String.class, true);
            return of(getValueOfAnnotationWithStringArgument(annotationMirror), offsets);
        } else {
            throw new UnsupportedOperationException(
                    "cannot convert to SameLenQualifier from non-SameLen annotation"
                            + annotationMirror.toString());
        }
    }

    private SameLenQualifier(List<String> sequenceList, List<String> offsetList) {
        assert sequenceList.size() == offsetList.size();
        assert sequenceList.size() != 0;
        offsets = new HashMap<>();
        for (int i = 0; i < sequenceList.size(); i++) {
            String sequence = sequenceList.get(i);
            if (sequence.length() >= 2
                    && sequence.charAt(0) == '"'
                    && sequence.charAt(sequence.length() - 1) == '"') {
                sequence = sequence.substring(1, sequence.length() - 1);
            }
            String offset = offsetList.get(i);
            offsets.put(sequence, OffsetEquation.createOffsetFromJavaExpression(offset));
        }
    }

    /**
     * Returns the AnnotationMirror that represents this qualifier.
     *
     * <p>The returned annotation is canonicalized by sorting its arguments by sequence and then
     * offset. This is so that {@link AnnotationUtils#areSame(AnnotationMirror, AnnotationMirror)}
     * returns true for equivalent annotations.
     *
     * @param env a processing environment used to build the returned annotation
     * @return the AnnotationMirror that represents this qualifier
     */
    public AnnotationMirror convertToAnnotation(ProcessingEnvironment env) {
        List<String> sequences = new ArrayList<>();
        sequences.addAll(offsets.keySet());
        Collections.sort(sequences);
        List<String> offset =
                sequences
                        .stream()
                        .map(sequence -> offsets.get(sequence).toString())
                        .collect(Collectors.toList());
        AnnotationBuilder builder = new AnnotationBuilder(env, SameLen.class);
        builder.setValue("value", sequences);
        builder.setValue("offset", offset);
        return builder.build();
    }

    /** @return The list of sequences with an offset of zero. */
    public List<String> sameLenArrays() {
        return sameLenArrays("0");
    }

    /** @return The list of sequences with an offset of the given string. */
    public List<String> sameLenArrays(String s) {
        List<String> result =
                offsets.keySet()
                        .stream()
                        .filter(sequence -> offsets.get(sequence).toString().equals(s))
                        .collect(Collectors.toList());
        return result;
    }

    /**
     * Returns true iff the given offset plus the value of the offset in this SameLen equation equal
     * zero.
     */
    public boolean isSameLenByOffset(String sequence, OffsetEquation offset) {
        return offset == null || !offsets.containsKey(sequence)
                ? false
                : offset.copyAdd('+', offsets.get(sequence)).equals(OffsetEquation.ZERO);
    }

    /** Returns true iff every sequence in the superType has the same offset in this qualifier. */
    public boolean isSubtype(SameLenQualifier superType) {
        for (String superArray : superType.offsets.keySet()) {
            if (!superType.offsets.get(superArray).equals(offsets.get(superArray))) {
                return false;
            }
        }
        return true;
    }

    /**
     * If the set of sequences in other and in this qualifier are disjoint, returns unknown.
     *
     * <p>Otherwise, returns a new SameLenQualifier with the intersection of the two. Unknown is
     * also returned if the same sequence has different offsets in the two qualifiers.
     */
    public AnnotationMirror lub(
            SameLenQualifier other, AnnotationMirror unknown, ProcessingEnvironment env) {
        if (!Collections.disjoint(this.offsets.keySet(), other.offsets.keySet())) {
            List<String> intersection = new ArrayList<>();
            intersection.addAll(offsets.keySet());
            intersection.retainAll(other.offsets.keySet());
            List<String> intersectionOffsets = new ArrayList<>();
            for (String sequence : intersection) {
                if (offsets.get(sequence).equals(other.offsets.get(sequence))) {
                    intersectionOffsets.add(offsets.get(sequence).toString());
                } else {
                    return unknown;
                }
            }
            return of(intersection, intersectionOffsets).convertToAnnotation(env);
        } else {
            return unknown;
        }
    }

    /**
     * If the sequences are disjoint, returns bottom. Otherwise, returns the a combined SameLen
     * qualifier.
     */
    public AnnotationMirror glb(
            SameLenQualifier other, AnnotationMirror bottom, ProcessingEnvironment env) {
        if (!Collections.disjoint(offsets.keySet(), other.offsets.keySet())) {
            return this.combine(other).convertToAnnotation(env);
        } else {
            return bottom;
        }
    }

    /**
     * Produces a new SameLenQualifier with the union of this qualifier and the argument.
     *
     * <p>If the two qualifiers have different offsets for the same array, this function's behavior
     * is undefined.
     */
    public SameLenQualifier combine(SameLenQualifier other) {
        Set<String> union = new HashSet<>();
        union.addAll(offsets.keySet());
        union.addAll(other.offsets.keySet());
        List<String> unionOffsets = new ArrayList<>();
        for (String sequence : union) {
            if (offsets.containsKey(sequence) && other.offsets.containsKey(sequence)) {
                assert offsets.get(sequence).equals(other.offsets.get(sequence));
            }
            if (offsets.containsKey(sequence)) {
                unionOffsets.add(offsets.get(sequence).toString());
            } else if (other.offsets.containsKey(sequence)) {
                unionOffsets.add(other.offsets.get(sequence).toString());
            } else {
                assert false : "could not find an offset for sequence " + sequence;
            }
        }
        assert union.size() == unionOffsets.size()
                : "union and unionOffsets have different sizes"
                        + union.toString()
                        + " \nand\n "
                        + unionOffsets.toString();
        return of(new ArrayList<>(union), unionOffsets);
    }

    /**
     * Determines whether it is possible to remove the given sequence's mapping and still have a
     * valid SameLenQualifier. Will return false if the only sequence mapped in this qualifier is
     * the passed sequence, and true otherwise.
     */
    public boolean canRemove(String sequence) {
        return !(offsets.keySet().size() == 1 && offsets.keySet().contains(sequence));
    }

    /**
     * Returns a new SameLenQualifier with the mapping for the given sequence removed.
     *
     * <p>If the given sequence is unmapped, returns this qualifier (not a copy).
     *
     * <p>Note that you should always call canRemove(sequence) before calling this method. If
     * canRemove returns false, this method is guaranteed to throw an UnsupportedOperationException.
     */
    public SameLenQualifier remove(String sequence) {
        if (!canRemove(sequence)) {
            throw new UnsupportedOperationException(
                    "cannot remove "
                            + sequence
                            + " from SameLenQualifier, because it is the only mapped sequence");
        } else if (!offsets.keySet().contains(sequence)) {
            return this;
        } else {
            List<String> sequences =
                    offsets.keySet()
                            .stream()
                            .filter(seq -> !seq.equals(sequence))
                            .collect(Collectors.toList());

            List<String> newOffsets =
                    sequences
                            .stream()
                            .map(seq -> offsets.get(seq).toString())
                            .collect(Collectors.toList());
            return of(sequences, newOffsets);
        }
    }

    /** Returns a list of all the strings mapped by this qualifier. */
    public Iterable<? extends String> getSequences() {
        return offsets.keySet();
    }

    /**
     * Adds the offset associated with the given sequence to all elements of the passed set to
     * create the returned set. If the set is modified, this method returns a copy of the set.
     *
     * <p>If the given replacementSequence is not mapped by this SameLenQualifier, then this will
     * return a reference to the passed set of offsets.
     */
    public Set<OffsetEquation> addOffsetToAll(
            String replacementSequence, Set<OffsetEquation> otherOffsets) {
        OffsetEquation sameLenOffset = offsets.get(replacementSequence);
        if (sameLenOffset == null) {
            return otherOffsets;
        } else {
            return otherOffsets
                    .stream()
                    .map(otherOffset -> sameLenOffset.copyAdd('+', otherOffset))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public String toString() {
        return "SameLenQualifier{" + "offsets=" + offsets + '}';
    }
}
