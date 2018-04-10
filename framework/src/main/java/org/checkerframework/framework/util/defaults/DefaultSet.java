package org.checkerframework.framework.util.defaults;

import java.util.Comparator;
import java.util.TreeSet;
import org.checkerframework.javacutil.PluginUtil;

/**
 * An ordered set of Defaults (see {@link org.checkerframework.framework.util.defaults.Default}).
 * This class provides a little syntactic sugar and a better toString over TreeSet.
 */
@SuppressWarnings("serial")
class DefaultSet extends TreeSet<Default> {

    private static final Comparator<Default> comparator =
            new Comparator<Default>() {
                @Override
                public int compare(Default d1, Default d2) {
                    return d1.compareTo(d2);
                }
            };

    public DefaultSet() {
        super(comparator);
    }

    @Override
    public String toString() {
        return "DefaultSet( " + PluginUtil.join(", ", this) + " )";
    }

    public static final DefaultSet EMPTY = new DefaultSet();
}
