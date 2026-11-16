import java.net.Socket;
import java.util.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests:
 *  - assigning an owning collection to an existing @NotOwningCollection variable keeps ownership
 *    with the source;
 *  - passing one owning collection to two @OwningCollection parameters is an error;
 *  - an argument whose must-call type is unknown may be owning.
 */
class CollectionOwnershipReviewFixes {

  void consume(@OwningCollection List<Socket> l) {
    for (Socket s : l) {
      try {
        s.close();
      } catch (Exception e) {
      }
    }
  }

  void consumeTwo(@OwningCollection List<Socket> a, @OwningCollection List<Socket> b) {
    consume(a);
    consume(b);
  }

  void borrowThenConsume(@OwningCollection List<Socket> sockets) {
    @NotOwningCollection List<Socket> borrowed;
    borrowed = sockets;
    // `sockets` is still owning, so it may be passed to an @OwningCollection parameter, and doing
    // so fulfills its obligation.
    consume(sockets);
  }

  void consumeSameTwice(@OwningCollection List<Socket> sockets) {
    // :: error: transfer.owningcollection.multiple
    consumeTwo(sockets, sockets);
    // Ownership was not transferred, so `sockets` is still owning.
    consume(sockets);
  }

  void consumeDistinct(@OwningCollection List<Socket> a, @OwningCollection List<Socket> b) {
    consumeTwo(a, b);
  }

  @MustCallUnknown Object acquire() {
    throw new Error();
  }

  void insertUnknown(@NotOwningCollection List<@MustCallUnknown Object> list) {
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    list.add(acquire());
  }
}
