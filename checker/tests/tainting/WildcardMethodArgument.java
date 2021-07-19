import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.List;

public class WildcardMethodArgument {
  abstract static class MyClass {
    abstract List<? extends ExpressionTree> getArguments();
  }

  void method(MyClass myClass) {
    List<? extends ExpressionTree> javacArgs = new ArrayList<>(myClass.getArguments());
  }
}
