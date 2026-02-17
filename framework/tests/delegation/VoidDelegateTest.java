import java.util.ArrayList;
import org.checkerframework.common.delegation.qual.*;

// :: warning: (delegate.override)
public class VoidDelegateTest<E> extends ArrayList<E> {

  @Delegate private ArrayList<E> array;

  public VoidDelegateTest(ArrayList<E> array) {
    this.array = array;
  }

  @Override
  public void clear() {
    this.array.clear(); // This should be OK
  }
}
