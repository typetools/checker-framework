package org.checkerframework.framework.util.defaults;

import org.checkerframework.framework.util.PluginUtil;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * An ordered set of Defaults (see {@link org.checkerframework.framework.util.defaults.Default}
 * This class provides a little syntactic sugar and a better toString over TreeSet.
 */
@SuppressWarnings("serial")
class DefaultSet extends TreeSet<Default> {
    public DefaultSet() {
        super( new Comparator<Default>() {
            @Override
            public int compare(Default d1, Default d2) { return d1.compareTo(d2); }
        });
    }

    @Override
    public String toString() {
        return "DefaultSet( " + PluginUtil.join(", ", this) + " )";
    }

    public static final DefaultSet EMPTY = new DefaultSet();
}
