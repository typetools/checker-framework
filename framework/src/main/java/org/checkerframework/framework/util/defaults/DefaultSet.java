package org.checkerframework.framework.util.defaults;

import java.util.Collection;
import java.util.TreeSet;
import org.plumelib.util.StringsP;

/**
 * An ordered set of Defaults (see {@link Default}). This class provides a little syntactic sugar
 * and a better toString over TreeSet.
 */
@SuppressWarnings("serial")
class DefaultSet extends TreeSet<Default> {

  /** Creates a DefaultSet. */
  public DefaultSet() {
    super(Default::compareTo);
  }

  @Override
  public String toString() {
    return "DefaultSet( " + StringsP.join(", ", this) + " )";
  }

  /** The empty DefaultSet. It is shared and immutable, because it is handed out to many callers. */
  public static final DefaultSet EMPTY =
      new DefaultSet() {
        @Override
        public boolean add(Default def) {
          throw new UnsupportedOperationException("DefaultSet.EMPTY is immutable");
        }

        // TreeSet.addAll does not always delegate to add, so override addAll too.
        @Override
        public boolean addAll(Collection<? extends Default> defaults) {
          throw new UnsupportedOperationException("DefaultSet.EMPTY is immutable");
        }
      };
}
