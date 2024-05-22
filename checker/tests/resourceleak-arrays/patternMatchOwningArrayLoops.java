import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcallonelements.qual.OwningArray;

class PatternMatchOwningArrayLoops {
  private final int n = 10;
  private final String myHost = "";
  private final int myPort = 1;
  // :: error: owning.array
  @Owning Socket[] s;

  // @OwningArray non-(1dArray/collection) is not allowed
  public void owningArrayNonArray() {
    // :: error: owningarray.nonarray
    @OwningArray Socket s;
    // :: error: owningarray.nonarray
    @OwningArray Socket[][] sMultiDimensional;
  }

  public void illegalOwningArrayAssignment() {
    Socket[] s = new Socket[n];
    // this is a false positive, but we only allow assignment
    // of an @OwningArray to a new [].
    // :: error: illegal.owningarray.assignment
    @OwningArray Socket[] arr = s;
  }

  // test that aliasing is not allowed.
  public void illegalAliasing() {
    @OwningArray Socket[] arr = new Socket[n];
    // :: error: illegal.owningarray.assignment
    @OwningArray Socket[] arr2 = arr;
    // :: error: illegal.aliasing
    Socket[] arr3 = arr;
  }

  // test that declaring an @OwningArray is alright
  public void illegalOwningArrayElementAssignment() {
    @OwningArray Socket[] arr = new Socket[n];
    try {
      // :: error: illegal.owningarray.element.assignment
      // :: error: required.method.not.called
      arr[0] = new Socket(myHost, myPort);
    } catch (Exception e) {
    }
    // :: error: illegal.owningarray.element.assignment
    arr[0] = null;
  }

  public void unfulfilledAllocationLoop() {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
  }

  public void reassignOwningArrayWithOpenObligations() {
    // try to trick the checker by closing the elements of the array
    // after reassigning it.
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
      } catch (Exception e) {
      }
    }
  }

  // test that opening and subsequent closing is alright
  public void validClosing() {
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
      } catch (Exception e) {
      }
    }
  }

  public void validDeallocationLoop() {
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    // this deallocation loop is legal and should be pattern-matched
    for (int i = 0; i < n; i++) {
      try {
        try {
          arr[i].close();
        } catch (Exception e) {
        }
      } catch (Exception e) {
      }
    }
  }

  public void invalidDeallocationLoop() {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    // this deallocation loop is illegal and is not pattern-matched
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
        i++;
      } catch (Exception e) {
      }
    }
  }

  public void invalidDeallocationLoop2() {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    // this deallocation loop is illegal and is not pattern-matched
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
      } catch (Exception e) {
      } finally {
        break;
      }
    }
  }

  public void invalidDeallocationLoop3() throws Exception {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    // this deallocation loop is illegal and is not pattern-matched
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
      } catch (Exception e) {
        throw new Exception("this prevents a pattern-match");
      }
    }
  }

  public void invalidDeallocationLoop4() {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    // this deallocation loop is illegal and is not pattern-matched
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
      } catch (Exception e) {
      } finally {
        i += 2;
      }
    }
  }

  // test that opening and subsequent closing is alright
  public void invalidClosingAndReopening() {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
      } catch (Exception e) {
      }
    }
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
  }

  public void invalidReallocateElements() {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    for (int i = 0; i < n; i++) {
      try {
        // :: error: illegal.owningarray.allocation
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
  }

  // test that passing ownership works
  public void validClosingByEnsuresCmoeMethod() {
    @OwningArray Socket[] arr = new Socket[n];
    for (int i = 0; i < n; i++) {
      try {
        arr[i] = new Socket(myHost, myPort);
      } catch (Exception e) {
      }
    }
    close(arr);
  }

  public void close(@OwningArray Socket[] arr) {
    for (int i = 0; i < n; i++) {
      try {
        arr[i].close();
      } catch (Exception e) {
      }
    }
  }
}
