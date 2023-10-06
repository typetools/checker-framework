import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.*;

/** Test for postcondition support via @EnsureCalledMethodsOnException. */
public abstract class EnsuresCalledMethodsOnExceptionTest {

  static class Resource {
    void a() {}

    void b() throws IOException {}
  }

  abstract boolean arbitraryChoice();

  abstract void throwArbitraryException() throws Exception;

  @EnsuresCalledMethodsOnException(value = "#1", methods = "b")
  void blanketCase(Resource r) throws IOException {
    // OK: r.b() counts as called even if it itself throws an exception.
    r.b();
  }

  @EnsuresCalledMethodsOnException(value = "#1", methods = "a")
  void noCall(Resource r) {
    // OK: this method does not throw exceptions.
  }

  @EnsuresCalledMethodsOnException(value = "#1", methods = "a")
  // :: error: (contracts.postcondition)
  void callAfterThrow(Resource r) throws Exception {
    if (arbitraryChoice()) {
      // Not OK: r.a() has not been called yet
      throwArbitraryException();
    }
    r.a();
  }

  @EnsuresCalledMethodsOnException(value = "#1", methods = "a")
  void callInFinallyBlock(Resource r) throws Exception {
    try {
      if (arbitraryChoice()) {
        // OK: r.a() will be called in the finally block
        throwArbitraryException();
      }
    } finally {
      r.a();
    }
  }

  @EnsuresCalledMethodsOnException(value = "#1", methods = "a")
  void callInCatchBlock(Resource r) throws Exception {
    try {
      if (arbitraryChoice()) {
        // OK: r.a() will be called in the catch block
        throwArbitraryException();
      }
    } catch (Exception e) {
      r.a();
      throw e;
    }
  }

  @EnsuresCalledMethodsOnException(value = "#1", methods = "a")
  // :: error: (contracts.postcondition)
  void callInSpecificCatchBlock(Resource r) throws Exception {
    try {
      if (arbitraryChoice()) {
        // Not OK: the catch block only catches IOException
        throwArbitraryException();
      }
    } catch (IOException e) {
      r.a();
      throw e;
    }
  }

  @EnsuresCalledMethodsOnException(value = "#1", methods = "a")
  abstract void callMethodOnException(Resource r) throws Exception;

  @EnsuresCalledMethodsOnException(value = "#1", methods = "a")
  void propagateSubtypeOfException(Resource r) throws Exception {
    // OK: the call satisfies our contract
    callMethodOnException(r);
  }

  @EnsuresCalledMethods(value = "#1", methods = "a")
  void exploitCalledMethodsOnException(Resource r) throws Exception {
    try {
      callMethodOnException(r);
    } catch (Exception e) {
      // OK: the other call ensured the contract
      return;
    }
    // OK: although r.a() was not called, this method promises nothing on exceptional return
    throw new Exception("Phooey");
  }

  @EnsuresCalledMethods(value = "#1", methods = "a")
  // :: error: (contracts.postcondition)
  void exceptionalCallsDoNotSatisfyNormalPaths(Resource r) throws Exception {
    // Not OK: this call is not enough to satisfy our contract, since it only promises something
    // on exceptional return.
    callMethodOnException(r);
  }
}
