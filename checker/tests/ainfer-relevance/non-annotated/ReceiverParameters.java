import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// This class is relevant because it is a subtype of CharSequence, so whole-program inference can
// infer a qualifier for a receiver.  Inference adds a receiver parameter to each method whose
// receiver type it infers.  The goal file shows that an ajava file contains:
//  * no receiver parameter that inference added and that has no annotations, as for `length`;
//  * every receiver parameter that is written in the source code, even one that has no
//    annotations, as for `writtenReceiver`; and
//  * every receiver parameter that inference added and that has an annotation, as for
//    `inferredReceiver`.
public class ReceiverParameters implements CharSequence {

  @Override
  public int length() {
    return 0;
  }

  @Override
  public char charAt(int index) {
    return 'a';
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return this;
  }

  void writtenReceiver(ReceiverParameters this) {}

  void inferredReceiver() {
    // :: warning: [assignment]
    @AinferSibling1 ReceiverParameters r = this;
  }

  static void client(ReceiverParameters r) {
    r.writtenReceiver();
    getSibling1().inferredReceiver();
  }

  @SuppressWarnings("cast.unsafe") // This is how to obtain a @AinferSibling1 ReceiverParameters.
  static @AinferSibling1 ReceiverParameters getSibling1() {
    return (@AinferSibling1 ReceiverParameters) new ReceiverParameters();
  }
}
