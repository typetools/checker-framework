// Reading an owning collection field yields a non-owning reference, and the field's ownership
// cannot be given away, even through a cast or a conditional expression.

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class OwningCollectionFieldEscape implements Closeable {

  List<Resource> resList = new ArrayList<>();
  boolean b;

  @CreatesMustCallFor("this")
  void add(@Owning Resource r) {
    resList.add(r);
  }

  void overwriteAfterCastRead() {
    List<Resource> l = (List<Resource>) resList;
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  List<Resource> returnCast() {
    // :: error: transfer.owningcollection.field.ownership
    return (List<Resource>) resList;
  }

  List<Resource> returnConditional() {
    // :: error: transfer.owningcollection.field.ownership
    return b ? resList : resList;
  }

  @Override
  @CollectionFieldDestructor("resList")
  public void close() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
  }
}
