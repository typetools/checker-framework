import java.util.ArrayDeque;
import java.util.Deque;

public class DequePushIncrement {

  Deque<Integer> counters = new ArrayDeque<Integer>();

  public void visitNewClass() {
    int i = 0;
    counters.push(++i);
  }
}
