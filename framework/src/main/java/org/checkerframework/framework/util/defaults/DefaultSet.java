package org.checkerframework.framework.util.defaults;

import org.plumelib.util.StringsPlume;

import java.util.TreeSet;

/**
 * An ordered set of Defaults (see {@link org.checkerframework.framework.util.defaults.Default}).
 * This class provides a little syntactic sugar and a better toString over TreeSet.
 */
@SuppressWarnings("serial")
class DefaultSet extends TreeSet<Default> {

    /** Creates a DefaultSet. */
    public DefaultSet() {
        super(Default::compareTo);
    }

    @Override
    public String toString() {
        return "DefaultSet( " + StringsPlume.join(", ", this) + " )";
    }

    public static final DefaultSet EMPTY = new DefaultSet();
}
