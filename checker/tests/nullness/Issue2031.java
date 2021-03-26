import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2031 {
  public interface InterfaceA<A> {}

  public interface InterfaceB<B> {}

  abstract static class OperatorSection<C extends InterfaceA<C> & InterfaceB<C>> {
    C makeExpression(Map<String, C> expressions) {
      @Nullable C e = expressions.get("");
      if (e != null) {
        return e;
      } else {
        throw new RuntimeException("");
      }
    }
  }

  static class RecursiveTypes {
    public interface A<EXPRESSION> {}

    public interface B<EXPRESSION> {}

    abstract static class OperatorSection<EXPRESSION extends A<EXPRESSION> & B<EXPRESSION>> {
      abstract EXPRESSION makeExpression(Map<String, EXPRESSION> expressions);
    }

    static class BinaryOperatorSection<EXPRESSION extends A<EXPRESSION> & B<EXPRESSION>>
        extends OperatorSection<EXPRESSION> {
      @Override
      EXPRESSION makeExpression(Map<String, EXPRESSION> expressions) {
        @Nullable EXPRESSION e = expressions.get("");
        if (e != null) {
          return e;
        } else {
          throw new RuntimeException("");
        }
      }
    }
  }
}
