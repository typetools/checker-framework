// JLS 18.2.5: for a constraint formula <MethodReference -> throws T>, the X1, ..., Xm are the
// checked exceptions of the *compile-time declaration*'s throws clause, not the E1, ..., En of the
// function type's throws clause.  When the two lists happen to have the same length, using the
// function type's list pairs each javac type with the annotated type of a *different* exception,
// so the wrong exception's qualifiers are used for the inference variable's lower bound.
//
// The invocation below is assigned to a `var` so that the invocation has no target type and the
// inference variable is therefore determined only by the throws-clause constraints.

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.checkerframework.checker.tainting.qual.Untainted;

public class MethodRefThrownTypes {

  interface ThrowerAndIOException<E extends Throwable> {
    void run() throws E, IOException;
  }

  <E extends Throwable> List<E> inferIO(ThrowerAndIOException<E> t) throws E, IOException {
    throw new RuntimeException();
  }

  // FileNotFoundException is a subtype of the function type's proper thrown type IOException, so it
  // contributes no constraint; only InterruptedException does.  The function type's throws clause
  // also has two elements, so the length check in
  // InferenceFactory.getCheckedExceptionConstraints does not detect a misuse of it.
  void throwsSubtypeAndOther() throws FileNotFoundException, @Untainted InterruptedException {}

  void testThrowsSubtypeAndOther() throws Throwable {
    var l = inferIO(this::throwsSubtypeAndOther);
    List<@Untainted InterruptedException> l2 = l;
  }
}
