// The type parameter bounds of a method that overrides a method in unchecked bytecode are not
// checked.  The overridden method's type parameters have the conservative bounds (top for the
// upper bound, bottom for the lower bound), which no overriding method could contain.

import java.util.EventListener;
import javax.swing.AbstractListModel;

public abstract class OverrideUncheckedGenericMethod extends AbstractListModel<String> {
  @Override
  public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
    throw new Error("not implemented");
  }
}
