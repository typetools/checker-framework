package utilMDE;

/**
 * Interface for things that make boolean decisions.
 * This is inspired by java.io.FilenameFilter.
 **/
public interface Filter<T> {
  /** Tests whether a specified Object satisfies the filter. */
  boolean accept(T o);
}
