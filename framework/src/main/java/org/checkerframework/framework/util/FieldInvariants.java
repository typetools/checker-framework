package org.checkerframework.framework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents field invariants, which the user states by writing {@code @FieldInvariant}. Think of
 * this as a set of (field, qualifier) pairs.
 *
 * <p>A FieldInvariants object may be malformed (inconsistent number of fields and qualifiers). In
 * this case, the BaseTypeVisitor will issue an error.
 */
public class FieldInvariants {

    /**
     * A list of simple field names. A field may appear more than once in this list. This list has
     * the same length as {@code qualifiers}.
     */
    private final List<String> fields;

    /**
     * A list of qualifiers that apply to the field at the same index in {@code fields}. In a
     * well-formed FieldInvariants, has the same length as {@code fields}.
     */
    private final List<AnnotationMirror> qualifiers;

    /**
     * Creates a new FieldInvariants object. The result is well-formed if length of qualifiers is
     * either 1 or equal to length of {@code fields}.
     *
     * @param fields list of fields
     * @param qualifiers list of qualifiers
     */
    public FieldInvariants(List<String> fields, List<AnnotationMirror> qualifiers) {
        this(null, fields, qualifiers);
    }

    /**
     * Creates a new object with all the invariants in {@code other}, plus those specified by {@code
     * fields} and {@code qualifiers}. The result is well-formed if length of qualifiers is either 1
     * or equal to length of {@code fields}.
     *
     * @param other other invariant object, may be null
     * @param fields list of fields
     * @param qualifiers list of qualifiers
     */
    public FieldInvariants(
            FieldInvariants other, List<String> fields, List<AnnotationMirror> qualifiers) {
        if (qualifiers.size() == 1) {
            while (fields.size() > qualifiers.size()) {
                qualifiers.add(qualifiers.get(0));
            }
        }
        if (other != null) {
            fields.addAll(other.fields);
            qualifiers.addAll(other.qualifiers);
        }

        this.fields = Collections.unmodifiableList(fields);
        this.qualifiers = qualifiers;
    }

    /** The simple names of the fields that have a qualifier. May contain duplicates. */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Returns a list of qualifiers for {@code field}. If {@code field} has no qualifiers, returns
     * an empty list.
     *
     * @param field simple field name
     * @return a list of qualifiers for {@code field}, possibly empty
     */
    public List<AnnotationMirror> getQualifiersFor(CharSequence field) {
        if (!isWellFormed()) {
            throw new BugInCF("malformed FieldInvariants");
        }
        String fieldString = field.toString();
        int index = fields.indexOf(fieldString);
        if (index == -1) {
            return Collections.emptyList();
        }
        List<AnnotationMirror> list = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).equals(fieldString)) {
                list.add(qualifiers.get(i));
            }
        }
        return list;
    }

    /**
     * Returns true if there is a qualifier for each field in {@code fields}.
     *
     * @return true if there is a qualifier for each field in {@code fields}
     */
    public boolean isWellFormed() {
        return qualifiers.size() == fields.size();
    }

    /**
     * Returns null if {@code superInvar} is a super invariant, otherwise returns the error message.
     *
     * @param superInvar the value to check for being a super invariant
     * @param factory the type factory
     * @return null if {@code superInvar} is a super invariant, otherwise returns the error message
     */
    public DiagMessage isSuperInvariant(FieldInvariants superInvar, AnnotatedTypeFactory factory) {
        QualifierHierarchy qualifierHierarchy = factory.getQualifierHierarchy();
        if (!this.fields.containsAll(superInvar.fields)) {
            List<String> missingFields = new ArrayList<>(superInvar.fields);
            missingFields.removeAll(fields);
            return new DiagMessage(
                    Kind.ERROR,
                    "field.invariant.not.found.superclass",
                    String.join(", ", missingFields));
        }

        for (String field : superInvar.fields) {
            List<AnnotationMirror> superQualifiers = superInvar.getQualifiersFor(field);
            List<AnnotationMirror> subQualifiers = this.getQualifiersFor(field);
            for (AnnotationMirror superA : superQualifiers) {
                AnnotationMirror sub =
                        qualifierHierarchy.findAnnotationInSameHierarchy(subQualifiers, superA);
                if (sub == null || !qualifierHierarchy.isSubtype(sub, superA)) {
                    return new DiagMessage(
                            Kind.ERROR,
                            "field.invariant.not.subtype.superclass",
                            field,
                            sub,
                            superA);
                }
            }
        }
        return null;
    }
}
