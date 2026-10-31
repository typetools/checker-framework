import java.io.IOException;
import java.util.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests mutation policy for non-owning and owning collections.
 *
 * <p>The non-owning cases check that only definitely non-owning elements may be inserted.
 * The owning cases check that inserted obligations are tracked and later discharged.
 */
class NotOwningCollectionMutationTest {

  @InheritableMustCall("close")
  static class TrackedResource {
    void close() throws IOException {}
  }

  TrackedResource owning() {
    return new TrackedResource();
  }

  @NotOwning
  TrackedResource notOwning() {
    // :: error: required.method.not.called
    return new TrackedResource();
  }

  /*
   * Non-owning collection receivers may accept only definitely non-owning elements.
   */
  void okNocAddNotOwningParam(
      @NotOwningCollection List<TrackedResource> list, @NotOwning TrackedResource resource) {
    list.add(resource);
  }

  void okNocAddNotOwningMethod(@NotOwningCollection List<TrackedResource> list) {
    list.add(notOwning());
  }

  /*
   * Inserting owning elements into a non-owning collection is illegal even if the caller
   * closes the value afterward.
   */
  void errNocAddOwningLocalThenClose(@NotOwningCollection List<TrackedResource> list) {
    TrackedResource resource = new TrackedResource();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    list.add(resource);
    try {
      resource.close();
    } catch (IOException e) {
      // ignore
    }
  }

  void errNocAddOwningParamThenClose(
      @NotOwningCollection List<TrackedResource> list, @Owning TrackedResource resource) {
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    list.add(resource);
    try {
      resource.close();
    } catch (IOException e) {
      // ignore
    }
  }

  void errNocAddAliasOfOwningThenClose(@NotOwningCollection List<TrackedResource> list) {
    TrackedResource resource = new TrackedResource();
    TrackedResource alias = resource;
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    list.add(alias);
    try {
      resource.close();
    } catch (IOException e) {
      // ignore
    }
  }

  /*
   * Reassignment turns the old owner name into a non-owning alias. Inserting through that
   * old name should therefore be rejected.
   */
  void errNocReceiverAfterMoveThenClose() {
    @OwningCollection List<TrackedResource> owner = new ArrayList<>();
    List<TrackedResource> newOwner = owner;
    TrackedResource resource = new TrackedResource();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    owner.add(resource);
    try {
      resource.close();
    } catch (IOException e) {
      // ignore
    }

    for (TrackedResource item : newOwner) {
      try {
        item.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  final @NotOwningCollection List<TrackedResource> cache = new ArrayList<>();

  /*
   * Non-owning collection fields follow the same mutation rule as local non-owning
   * collection references.
   */
  void okNocFieldAddNotOwning(@NotOwning TrackedResource resource) {
    cache.add(resource);
  }

  void okNocFieldAddDefaultParam(TrackedResource resource) {
    cache.add(resource);
  }

  void errNocFieldAddOwningThenClose() {
    TrackedResource resource = new TrackedResource();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    cache.add(resource);
    try {
      resource.close();
    } catch (IOException e) {
      // ignore
    }
  }

  /*
   * Owning collections may accumulate element obligations, which must later be discharged
   * by a certified loop.
   */
  void errOcAddOwningLocalNoDispose() {
    @OwningCollection List<TrackedResource> list = new ArrayList<>();
    TrackedResource resource = new TrackedResource();
    // :: error: unfulfilled.collection.obligations
    list.add(resource);
  }

  void errOcAddOwningCallNoDispose() {
    List<TrackedResource> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(owning());
  }

  void errOcAddNotOwningCallNoDispose() {
    @OwningCollection List<TrackedResource> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(notOwning());
  }

  void errOcAddOwningAliasNoDispose() {
    @OwningCollection List<TrackedResource> list = new ArrayList<>();
    TrackedResource resource = new TrackedResource();
    TrackedResource alias = resource;
    // :: error: unfulfilled.collection.obligations
    list.add(alias);
  }

  void errOcAddOwningNewExprNoDispose() {
    @OwningCollection List<TrackedResource> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new TrackedResource());
  }

  void okOcAddOwningThenDisposeLoop() {
    @OwningCollection List<TrackedResource> list = new ArrayList<>();
    TrackedResource resource = new TrackedResource();
    list.add(resource);

    for (TrackedResource item : list) {
      try {
        item.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  void okOcAddTwoThenDisposeLoop() {
    @OwningCollection List<TrackedResource> list = new ArrayList<>();
    TrackedResource first = new TrackedResource();
    TrackedResource second = new TrackedResource();
    list.add(first);
    list.add(second);

    for (TrackedResource item : list) {
      try {
        item.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /*
   * Inserting non-owning elements into an owning collection is allowed, because it does not
   * create a new collection obligation.
   */
  void okOcInsertNotOwning(
      @OwningCollection List<TrackedResource> list, @NotOwning TrackedResource resource) {
    list.add(resource);
    for (TrackedResource item : list) {
      try {
        item.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }
}
