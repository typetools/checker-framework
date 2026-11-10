import java.io.*;
import java.util.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests that omitted ownership annotations still lead to the expected field and collection
 * obligation errors.
 */
class MissingCollectionOwnershipAnnotations {
  // :: error: unfulfilled.field.obligations
  Set<Resource> resSet = new HashSet<>();

  /*
   * Building a local collection of streams without discharging or transferring ownership
   * should report an unfulfilled collection obligation.
   */
  private InputStream[] responseSequenceBinary(List<String> fileNames) throws IOException {
    List<InputStream> response = new ArrayList<>();
    for (var fileName : fileNames) {
      // :: error: unfulfilled.collection.obligations
      response.add(new FileInputStream(fileName));
    }
    return response.toArray(new InputStream[0]);
  }
}
