package org.checkerframework.qualframework.base.format;

import java.util.Set;

/**
 * DefaultQualFormatter is a very basic QualFormatter that
 * returns the toString of the qual.
 */
public class DefaultQualFormatter<Q> implements QualFormatter<Q> {

    private final Set<?> invisibleQualifiers;

    public DefaultQualFormatter(Set<?> invisibleQualifiers) {
        this.invisibleQualifiers = invisibleQualifiers;
    }

    @Override
    public String format(Q q) {
        return q.toString();
    }

    @Override
    public String format(Q q, boolean printInvisibles) {
        if (printInvisibles || !invisibleQualifiers.contains(q)) {
            return q.toString();
        } else {
            return null;
        }
    }
}
