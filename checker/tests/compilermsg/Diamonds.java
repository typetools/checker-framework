import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.checkerframework.checker.compilermsgs.qual.UnknownCompilerMessageKey;

public class Diamonds {

  void method(List<String> arrays) {
    arrays = new ArrayList<>(new HashSet<>(arrays));
    arrays = newArrayList(new HashSet<>(arrays));
    arrays = new ArrayList<>(new HashSet<@UnknownCompilerMessageKey String>(arrays));
  }

  <F> ArrayList<F> newArrayList(Collection<? extends F> param) {
    return new ArrayList<>(param);
  }
}
