package org.checkerframework.framework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;

/**
 * Represents field invariants. A list of fields that have a specific qualifier in a class where
 * these field invariants apply.
 */
public class FieldInvariants {
    /** a list of simple filed names. */
    private final List<String> fields;

    /** A list of qualifiers that apply to the field at the same index in {@code fields}. */
    private final List<AnnotationMirror> qualifiers;

    public FieldInvariants(List<String> fields, List<AnnotationMirror> qualifiers) {
        this(null, fields, qualifiers);
    }

    /**
     * Creates a new object with all the invariant in {@code other}, plus those specified by {@code
     * fields} and {@code qualifiers}.
     *
     * @param other other invariant object, may be null
     * @param fields list of fields
     * @param qualifiers list of qualifiers
     */
    public FieldInvariants(
            FieldInvariants other, List<String> fields, List<AnnotationMirror> qualifiers) {
        if (fields.size() > qualifiers.size() && qualifiers.size() == 1) {
            int difference = fields.size() - qualifiers.size();
            for (int i = 0; i < difference; i++) {
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

    public List<String> getFields() {
        return fields;
    }

    /**
     * Returns a list of qualifiers for {@code field}. If field has no qualifiers, then the empty
     * list is returned.
     *
     * @param field simple field name
     * @return a list of qualifiers for {@code field}
     */
    public List<AnnotationMirror> getQualifiersFor(CharSequence field) {
        String fieldString = field.toString();
        if (isWellFormed()) {
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
        return Collections.emptyList();
    }

    /** @return whether or not there is a qualifier for each field */
    public boolean isWellFormed() {
        return qualifiers.size() == fields.size();
    }

    /**
     * @return null if {@code superInvar} is a super invariant, otherwise returns a Result with the
     *     error message
     */
    public Result isSuperInvariant(FieldInvariants superInvar, AnnotatedTypeFactory factory) {
        QualifierHierarchy qualifierHierarchy = factory.getQualifierHierarchy();
        if (!this.fields.containsAll(superInvar.fields)) {
            List<String> missingFields = new ArrayList<>(superInvar.fields);
            missingFields.removeAll(fields);
            return Result.failure(
                    "field.invariant.not.found.superclass", PluginUtil.join(", ", missingFields));
        }

        for (String field : superInvar.fields) {
            List<AnnotationMirror> superQualifiers = superInvar.getQualifiersFor(field);
            List<AnnotationMirror> subQualifiers = this.getQualifiersFor(field);
            for (AnnotationMirror superA : superQualifiers) {
                AnnotationMirror sub =
                        qualifierHierarchy.findAnnotationInSameHierarchy(subQualifiers, superA);
                if (sub == null || !qualifierHierarchy.isSubtype(sub, superA)) {
                    return Result.failure(
                            "field.invariant.not.subtype.superclass", field, sub, superA);
                }
            }
        }
        return null;
    }
}
