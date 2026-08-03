// JLS 18.2.5: for a constraint formula <MethodReference -> throws T>, the X1, ..., Xm are the
// checked exceptions of the *compile-time declaration*'s throws clause, not the E1, ..., En of the
// function type's throws clause.  Using the function type's throws clause makes the inference
// variable's lower bound be the functional interface's own type variable, so the inferred type
// argument is that type variable rather than an exception type.
//
// Each invocation below is assigned to a `var` so that the invocation has no target type and the
// inference variable is therefore determined only by the throws-clause constraints.

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MethodRefThrownTypes {

  interface Thrower<E extends Throwable> {
    void run() throws E;
  }

  interface ThrowerAndIOException<E extends Throwable> {
    void run() throws E, IOException;
  }

  <E extends Throwable> List<E> infer(Thrower<E> t) throws E {
    throw new RuntimeException();
  }

  <E extends Throwable> List<E> inferIO(ThrowerAndIOException<E> t) throws E, IOException {
    throw new RuntimeException();
  }

  void throwsNothing() {}

  void throwsOne() throws IOException {}

  void throwsTwo() throws IOException, InterruptedException {}

  void throwsSubtypeAndOther() throws FileNotFoundException, InterruptedException {}

  void testThrowsNothing() throws Throwable {
    var l = infer(this::throwsNothing);
    List<RuntimeException> l2 = l;
  }

  // The compile-time declaration and the function type each have one thrown type, so the two lists
  // have the same length and the mismatch is not caught by the length check in
  // InferenceFactory.getCheckedExceptionConstraints.  This is the case that the assertion in
  // ProperType.verifyTypeKinds catches.
  void testThrowsOne() throws Throwable {
    var l = infer(this::throwsOne);
    List<IOException> l2 = l;
  }

  void testThrowsTwo() throws Throwable {
    var l = infer(this::throwsTwo);
    List<Exception> l2 = l;
  }

  // FileNotFoundException is a subtype of the function type's proper thrown type IOException, so it
  // contributes no constraint; only InterruptedException does.
  void testThrowsSubtypeAndOther() throws Throwable {
    var l = inferIO(this::throwsSubtypeAndOther);
    List<InterruptedException> l2 = l;
  }
}
