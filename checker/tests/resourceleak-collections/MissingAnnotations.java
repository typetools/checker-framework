import java.io.*;
import java.nio.channels.*;
import java.util.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class SetTest {
  // :: error: unfulfilled.field.obligations
  Set<Resource> resSet = new HashSet<>();

  private InputStream[] responseSequenceBinary(List<String> fileNames) throws IOException {
    List<InputStream> response = new ArrayList<>();
    for (var fileName : fileNames) {
      // :: error: unfulfilled.collection.obligations
      response.add(new FileInputStream(fileName));
    }
    return response.toArray(new InputStream[0]);
  }
}
//
// @InheritableMustCall("close")
// class DestructorWithThis {
//  private final Queue<RandomAccessFile> files;
//
//  DestructorWithThis(int size) {
//    this.files = new ConcurrentLinkedQueue<RandomAccessFile>();
//  }
//
//  @CreatesMustCallFor("this")
//  public void release(@Owning RandomAccessFile file) {
//    this.files.add(file);
//  }
//
//  @CollectionFieldDestructor("this.files")
//  public void close() throws IOException {
//    try {
//      while (!this.files.isEmpty()) {
//        RandomAccessFile pooledFile = this.files.poll();
//        if (pooledFile != null) {
//          try {
//            pooledFile.close();
//          } catch (IOException e) {
//          }
//        }
//      }
//    } finally {
//      this.files.clear();
//    }
//  }
// }
