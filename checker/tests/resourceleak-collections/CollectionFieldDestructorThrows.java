// A collection field destructor must dispose of the elements even if it throws an exception.

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CollectionFieldDestructorThrows {

  @InheritableMustCall("close")
  static class ThrowsInLoop {
    final @OwningCollection List<Socket> sockets = new ArrayList<>();

    @CollectionFieldDestructor("sockets")
    // :: error: contracts.exceptional.postcondition
    public void close() throws IOException {
      for (Socket s : sockets) {
        s.close();
      }
    }
  }

  @InheritableMustCall("close")
  static class CatchesInLoop {
    final @OwningCollection List<Socket> sockets = new ArrayList<>();

    @CollectionFieldDestructor("sockets")
    public void close() {
      for (Socket s : sockets) {
        try {
          s.close();
        } catch (IOException e) {
        }
      }
    }
  }

  @InheritableMustCall("close")
  static class ThrowsAfterLoop {
    final @OwningCollection List<Socket> sockets = new ArrayList<>();

    @CollectionFieldDestructor("sockets")
    public void close() throws IOException {
      for (Socket s : sockets) {
        try {
          s.close();
        } catch (IOException e) {
        }
      }
      throw new IOException();
    }
  }
}
