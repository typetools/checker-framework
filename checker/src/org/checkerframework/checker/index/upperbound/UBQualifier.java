package org.checkerframework.checker.index.upperbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.checker.index.qual.UpperBoundUnknown;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/**
 * Abstraction for Upper Bound annotations.
 *
 * <p>{@link UpperBoundUnknown} is modeled as {@link UpperBoundUnknownQualifier} and {@link
 * UpperBoundBottom} is modeled as {@link UpperBoundBottomQualifier}.
 *
 * <p>{@link LTLengthOf} is modeled by {@link LessThanLengthOf}. {@link LTEqLengthOf} is equivalent
 * to @{@link LessThanLengthOf} with an offset of -1. {@link LTOMLengthOf} is equivalent to @{@link
 * LessThanLengthOf} with an offset of 1.
 */
public abstract class UBQualifier {

    public static UBQualifier createUBQualifier(AnnotationMirror am) {
        if (AnnotationUtils.areSameByClass(am, UpperBoundUnknown.class)) {
            return UpperBoundUnknownQualifier.UNKNOWN;
        } else if (AnnotationUtils.areSameByClass(am, UpperBoundBottom.class)) {
            return UpperBoundBottomQualifier.BOTTOM;
        } else if (AnnotationUtils.areSameByClass(am, LTLengthOf.class)) {
            return parseLTLengthOf(am);
        } else if (AnnotationUtils.areSameByClass(am, LTEqLengthOf.class)) {
            return parseLTEqLengthOf(am);
        } else if (AnnotationUtils.areSameByClass(am, LTOMLengthOf.class)) {
            return parseLTOMLengthOf(am);
        }
        assert false;
        return UpperBoundUnknownQualifier.UNKNOWN;
    }

    private static UBQualifier parseLTLengthOf(AnnotationMirror am) {
        List<String> arrays = AnnotationUtils.getElementValueArray(am, "value", String.class, true);
        List<String> offset =
                AnnotationUtils.getElementValueArray(am, "offset", String.class, true);
        if (offset.isEmpty()) {
            offset = Collections.nCopies(arrays.size(), "");
        }
        return createUBQualifier(arrays, offset);
    }

    private static UBQualifier parseLTEqLengthOf(AnnotationMirror am) {
        List<String> arrays = AnnotationUtils.getElementValueArray(am, "value", String.class, true);
        List<String> offset = Collections.nCopies(arrays.size(), "-1");
        return createUBQualifier(arrays, offset);
    }

    private static UBQualifier parseLTOMLengthOf(AnnotationMirror am) {
        List<String> arrays = AnnotationUtils.getElementValueArray(am, "value", String.class, true);
        List<String> offset = Collections.nCopies(arrays.size(), "1");
        return createUBQualifier(arrays, offset);
    }

    public static UBQualifier createUBQualifier(String array, String offset) {
        return createUBQualifier(
                Collections.singletonList(array), Collections.singletonList(offset));
    }

    public static UBQualifier createUBQualifier(AnnotatedTypeMirror type, AnnotationMirror top) {
        return createUBQualifier(type.getAnnotationInHierarchy(top));
    }

    /**
     * Creates an {@link UBQualifier} from the given arrays and offsets. The list of arrays may not
     * be empty. If the offsets list is empty, then an offset of 0 is used for each array. If the
     * offsets list is not empty, then it must be the same size as array.
     *
     * @param arrays Non-empty list of arrays
     * @param offsets list of offset, if empty, an offset of 0 is used
     * @return an {@link UBQualifier} for the arrays with the given offsets.
     */
    public static UBQualifier createUBQualifier(List<String> arrays, List<String> offsets) {
        assert !arrays.isEmpty();
        Map<String, Set<OffsetEquation>> map = new HashMap<>();
        if (offsets.isEmpty()) {
            for (String array : arrays) {
                map.put(array, Collections.singleton(OffsetEquation.ZERO));
            }
        } else {
            assert arrays.size() == offsets.size();
            for (int i = 0; i < arrays.size(); i++) {
                String array = arrays.get(i);
                String offset = offsets.get(i);
                Set<OffsetEquation> set = map.get(array);
                if (set == null) {
                    set = new HashSet<>();
                    map.put(array, set);
                }
                OffsetEquation eq = OffsetEquation.createOffsetFromJavaExpression(offset);
                if (eq.hasError()) {
                    return UpperBoundUnknownQualifier.UNKNOWN;
                }
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
     * @param node Node
     * @param factory AnnotatedTypeFactory
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

    public final boolean isUnknownOrBottom() {
        return isBottom() || isUnknown();
    }

    public abstract boolean isUnknown();

    public abstract boolean isBottom();

    public abstract boolean isSubtype(UBQualifier superType);

    public abstract UBQualifier lub(UBQualifier other);

    public UBQualifier widenUpperBound(UBQualifier obj) {
        return lub(obj);
    }

    public abstract UBQualifier glb(UBQualifier other);

    /**
     * Is the value with this qualifier less than the length of array?
     *
     * @param array String array
     * @return whether or not the value with this qualifier is less than the length of array
     */
    public boolean isLessThanLengthOf(String array) {
        return false;
    }

    /**
     * Is the value with this qualifier less than the length of any of the arrays?
     *
     * @param arrays list of arrays
     * @return whether or not the value with this qualifier is less than the length of any of the
     *     arrays
     */
    public boolean isLessThanLengthOfAny(List<String> arrays) {
        return false;
    }

    /**
     * Is the value with this qualifier less than or equal to the length of array?
     *
     * @param array String array
     * @return whether or not the value with this qualifier is less than or equal to the length of
     *     array.
     */
    public boolean isLessThanOrEqualTo(String array) {
        return false;
    }

    /** */
    static class LessThanLengthOf extends UBQualifier {
        private final Map<String, Set<OffsetEquation>> map;

        private LessThanLengthOf(Map<String, Set<OffsetEquation>> map) {
            assert !map.isEmpty();
            this.map = map;
        }

        /**
         * Is a value with this type less than or equal to the length of array?
         *
         * @param array String array
         * @return Is a value with this type less than or equal to the length of array?
         */
        @Override
        public boolean isLessThanOrEqualTo(String array) {
            Set<OffsetEquation> offsets = map.get(array);
            if (offsets == null) {
                return false;
            }
            return offsets.contains(OffsetEquation.NEG_1);
        }

        /**
         * Is a value with this type less than the length of any of the arrays?
         *
         * @param arrays String array
         * @return Is a value with this type less than the length of any of the arrays?
         */
        @Override
        public boolean isLessThanLengthOfAny(List<String> arrays) {
            for (String array : arrays) {
                if (isLessThanLengthOf(array)) {
                    return true;
                }
            }
            return false;
        }
        /**
         * Is a value with this type less than the length of the array?
         *
         * @param array String array
         * @return Is a value with this type less than the length of the array?
         */
        @Override
        public boolean isLessThanLengthOf(String array) {
            Set<OffsetEquation> offsets = map.get(array);
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
         * <p>The annotation is sorted by array and then offset. This is so that {@link
         * AnnotationUtils#areSame(AnnotationMirror, AnnotationMirror)} returns true for equivalent
         * annotations.
         *
         * @param env ProcessingEnvironment
         * @return the AnnotationMirror that represents this qualifier
         */
        public AnnotationMirror convertToAnnotationMirror(ProcessingEnvironment env) {
            List<String> sortedArrays = new ArrayList<>(map.keySet());
            Collections.sort(sortedArrays);
            List<String> arrays = new ArrayList<>();
            List<String> offsets = new ArrayList<>();
            boolean isLTEq = true;
            boolean isLTOM = true;
            for (String array : sortedArrays) {
                List<String> sortOffsets = new ArrayList<>();
                for (OffsetEquation eq : map.get(array)) {
                    isLTEq = isLTEq && eq.equals(OffsetEquation.NEG_1);
                    isLTOM = isLTOM && eq.equals(OffsetEquation.ONE);
                    sortOffsets.add(eq.toString());
                }
                Collections.sort(sortOffsets);
                for (String offset : sortOffsets) {
                    arrays.add(array);
                    offsets.add(offset);
                }
            }
            AnnotationBuilder builder;
            if (isLTEq) {
                builder = new AnnotationBuilder(env, LTEqLengthOf.class);
                builder.setValue("value", arrays);
            } else if (isLTOM) {
                builder = new AnnotationBuilder(env, LTOMLengthOf.class);
                builder.setValue("value", arrays);
            } else {
                builder = new AnnotationBuilder(env, LTLengthOf.class);
                builder.setValue("value", arrays);
                builder.setValue("offset", offsets);
            }
            return builder.build();
        }

        @Override
        public boolean equals(Object o) {
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
        public boolean isUnknown() {
            return false;
        }

        @Override
        public boolean isBottom() {
            return false;
        }

        /**
         * If superType is Unknown, return true. If superType is Bottom, return false.
         *
         * <p>Otherwise, this qualifier must contain all the arrays in superType. For each the
         * offsets for each array in superType, there must be an offset in this qualifier for the
         * array that is greater than or equal to the super offset.
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
                String array = entry.getKey();
                Set<OffsetEquation> superOffsets = entry.getValue();
                Set<OffsetEquation> subOffsets = map.get(array);

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
         * <p>2. For each array in the intersection, get the offsets for this and other. If any
         * offset in this is a less than or equal to an offset in other, then that offset is an
         * offset for the array in lub. If any offset in other is a less than or equal to an offset
         * in this, then that offset is an offset for the array in lub.
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

            Set<String> arrays = new HashSet<>(map.keySet());
            arrays.retainAll(otherLtl.map.keySet());

            Map<String, Set<OffsetEquation>> lubMap = new HashMap<>();
            for (String array : arrays) {
                Set<OffsetEquation> lub = new HashSet<>();
                Set<OffsetEquation> offsets1 = map.get(array);
                Set<OffsetEquation> offsets2 = otherLtl.map.get(array);
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
                    lubMap.put(array, lub);
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
            if (lub.isUnknownOrBottom() || obj.isUnknownOrBottom()) {
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
         * <p>In order to prevent this, if both types passed to lub include all the same arrays with
         * the same non-constant value offsets and if the constant value offsets are different then
         * remove that array-offset pair from lub.
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
            for (Entry<String, Set<OffsetEquation>> entry : lubMap.entrySet()) {
                String array = entry.getKey();
                Set<OffsetEquation> lubOffsets = entry.getValue();
                Set<OffsetEquation> thisOffsets = this.map.get(array);
                Set<OffsetEquation> otherOffsets = other.map.get(array);
                if (lubOffsets.size() != thisOffsets.size()
                        || lubOffsets.size() != otherOffsets.size()) {
                    return;
                }
                for (OffsetEquation lubEq : lubOffsets) {
                    if (lubEq.isInt()) {
                        int thisInt = OffsetEquation.getIntOffsetEquation(thisOffsets).getInt();
                        int otherInt = OffsetEquation.getIntOffsetEquation(otherOffsets).getInt();
                        if (thisInt != otherInt) {
                            remove.add(Pair.of(array, lubEq));
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

            Set<String> arrays = new HashSet<>(map.keySet());
            arrays.addAll(otherLtl.map.keySet());

            Map<String, Set<OffsetEquation>> glbMap = new HashMap<>();
            for (String array : arrays) {
                Set<OffsetEquation> glb = map.get(array);
                Set<OffsetEquation> otherglb = otherLtl.map.get(array);
                if (glb == null) {
                    glb = otherglb;
                } else if (otherglb != null) {
                    glb.addAll(otherglb);
                }
                glbMap.put(array, simplifyOffsets(glb));
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
         * @param node Node
         * @param factory AnnotatedTypeFactory
         * @return a copy of this qualifier with node add as an offset
         */
        @Override
        public UBQualifier plusOffset(Node node, UpperBoundAnnotatedTypeFactory factory) {
            OffsetEquation newOffset = OffsetEquation.createOffsetFromNode(node, factory, '+');
            if (newOffset.hasError()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            return addOffset(newOffset);
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
         * Adds node as a negative offset to a copy of this qualifier. This is done by creating a
         * negative offset equation for node and then adding that equation to every offset equation
         * in a copy of this object.
         *
         * @param node Node
         * @param factory AnnotatedTypeFactory
         * @return a copy of this qualifier with node add as an offset
         */
        @Override
        public UBQualifier minusOffset(Node node, UpperBoundAnnotatedTypeFactory factory) {
            OffsetEquation newOffset = OffsetEquation.createOffsetFromNode(node, factory, '-');
            if (newOffset.hasError()) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            return addOffset(newOffset);
        }

        /**
         * Adds the negation of value as an offset to a copy of this qualifier. This is done by
         * adding the negation of value value to every offset equation in a copy of this object.
         *
         * @param value int value to add
         * @return a copy of this qualifier with value add as an offset
         */
        @Override
        public UBQualifier minusOffset(int value) {
            OffsetEquation newOffset = OffsetEquation.createOffsetForInt(-value);
            return addOffset(newOffset);
        }

        private UBQualifier addOffset(OffsetEquation newOffset) {
            Map<String, Set<OffsetEquation>> plusMap = new HashMap<>(map.size());
            for (Entry<String, Set<OffsetEquation>> entry : map.entrySet()) {
                Set<OffsetEquation> plus = new HashSet<>(entry.getValue().size());
                for (OffsetEquation eq : entry.getValue()) {
                    plus.add(eq.copyAdd('+', newOffset));
                }
                plusMap.put(entry.getKey(), plus);
            }
            return new LessThanLengthOf(plusMap);
        }

        /**
         * If divisor == 1, return this object.
         *
         * <p>If divisor greater than 1, then return a copy of this object keeping only arrays and
         * offsets where the offset is less than or equal to zero.
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
                Map<String, Set<OffsetEquation>> divideMap = new HashMap<>(map.size());
                for (Entry<String, Set<OffsetEquation>> entry : map.entrySet()) {
                    Set<OffsetEquation> divide = new HashSet<>(entry.getValue().size());
                    for (OffsetEquation eq : entry.getValue()) {
                        if (eq.isNegativeOrZero()) {
                            divide.add(eq);
                        }
                    }
                    if (!divide.isEmpty()) {
                        divideMap.put(entry.getKey(), divide);
                    }
                }

                if (divideMap.isEmpty()) {
                    return UpperBoundUnknownQualifier.UNKNOWN;
                }
                return new LessThanLengthOf(divideMap);
            }
            return UpperBoundUnknownQualifier.UNKNOWN;
        }

        public boolean isValuePlusOffsetLessThanMinLen(String array, int value, int minlen) {
            Set<OffsetEquation> offsets = map.get(array);
            if (offsets == null) {
                return false;
            }
            for (OffsetEquation offset : offsets) {
                if (offset.isInt()) {
                    return minlen > value + offset.getInt();
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "LessThanLengthOf{" + "map=" + map + '}';
        }

        public Iterable<? extends String> getArrays() {
            return map.keySet();
        }
    }

    public static class UpperBoundUnknownQualifier extends UBQualifier {
        static final UBQualifier UNKNOWN = new UpperBoundUnknownQualifier();

        private UpperBoundUnknownQualifier() {}

        @Override
        public boolean isBottom() {
            return false;
        }

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
        public boolean isUnknown() {
            return false;
        }

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
}
