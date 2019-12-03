package org.checkerframework.framework.util.defaults;

import java.util.TreeSet;
import org.checkerframework.javacutil.PluginUtil;

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
        return "DefaultSet( " + PluginUtil.join(", ", this) + " )";
    }

    public static final DefaultSet EMPTY = new DefaultSet();
}
