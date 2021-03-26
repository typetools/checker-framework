import java.util.ArrayList;

public class OverrideCrash extends ArrayList {
  @Override
  public Object[] toArray(Object[] o) {
    return null;
  }
}
