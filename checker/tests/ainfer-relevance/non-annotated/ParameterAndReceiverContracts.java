import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.framework.qual.EnsuresQualifier;

// Inference infers postconditions about parameters (written "#1") and about the receiver ("this").
// In each method below, a call to `becomeSibling1` refines the parameter or receiver to
// `@AinferSibling1`, so inference infers a postcondition about it.  The goal file shows that
// inference writes the postcondition about a parameter or receiver whose type is relevant, but
// discards the postcondition about one whose type is irrelevant.
//
// `ParameterAndReceiverContracts` and `Irrelevant` are irrelevant, because neither is a subtype or
// supertype of any type in the checker's `@RelevantJavaTypes`.  `String` and `RelevantReceiver`
// are relevant, because they are subtypes of `CharSequence`.  `String[]` is irrelevant, because
// the checker's `@RelevantJavaTypes` lists no array type.
public class ParameterAndReceiverContracts {

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = AinferSibling1.class)
  static void becomeSibling1(Object arg) {}

  static class Irrelevant {}

  void irrelevantParameter(Irrelevant i) {
    becomeSibling1(i);
  }

  void relevantParameter(String s) {
    becomeSibling1(s);
  }

  void irrelevantArrayParameter(String[] a) {
    becomeSibling1(a);
  }

  void irrelevantReceiver() {
    becomeSibling1(this);
  }

  abstract static class RelevantReceiver implements CharSequence {
    void relevantReceiver() {
      becomeSibling1(this);
    }
  }
}
