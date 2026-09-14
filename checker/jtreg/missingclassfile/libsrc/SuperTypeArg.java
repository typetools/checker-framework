package lib;

/**
 * Mentions {@link Missing} in a type argument of its supertype, like Beam's {@code
 * TableRowJsonCoder extends AtomicCoder<TableRow>} and {@code DatastoreV1.Read extends
 * PTransform<PBegin, PCollection<Entity>>}.
 */
public class SuperTypeArg extends Box<Missing> {
  public SuperTypeArg self() {
    return this;
  }

  public long size() {
    return 0;
  }
}
