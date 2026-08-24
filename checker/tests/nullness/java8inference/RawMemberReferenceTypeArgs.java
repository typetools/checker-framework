import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * For a method reference {@code ReferenceType :: Identifier} with a raw {@code ReferenceType}, JLS
 * 15.13.1 takes the class's type arguments from capture(G&lt;...&gt;), where G&lt;...&gt; is the
 * parameterization of {@code ReferenceType} that is a supertype of P1. They are not inferred.
 *
 * <p>The all-systems tests for this only check that inference does not crash. These assignments
 * check that the type arguments are the ones JLS 15.13.1 specifies: if they were inferred, they
 * would resolve to {@code Object} and these assignments would not type-check.
 */
public class RawMemberReferenceTypeArgs {

  static <T> void typeVariableElement(Stream<? extends Iterable<? extends T>> iterables) {
    Stream<? extends Iterator<? extends T>> s = iterables.map(Iterable::iterator);
  }

  static void concreteElement(Stream<? extends List<? extends Number>> lists) {
    Stream<? extends Iterator<? extends Number>> s = lists.map(List::iterator);
  }
}
