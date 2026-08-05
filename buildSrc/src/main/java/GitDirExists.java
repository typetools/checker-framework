import java.io.File;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

/**
 * Reports whether a checkout contains a {@code .git} file or directory. (It is a file, not a
 * directory, in a worktree or a submodule.)
 *
 * <p>This is a {@link ValueSource} rather than a plain {@code providers.provider { ... }} so that
 * the Gradle configuration cache treats the test as an input. The configuration cache re-runs a
 * value source when deciding whether a cached configuration is still valid, but it never re-runs a
 * plain provider.
 */
public abstract class GitDirExists implements ValueSource<Boolean, GitDirExists.Parameters> {

  /** Parameters for {@link GitDirExists}. */
  public interface Parameters extends ValueSourceParameters {
    /**
     * The path of the {@code .git} file or directory to test for.
     *
     * @return the path of the {@code .git} file or directory to test for
     */
    Property<String> getGitDirPath();
  }

  /** Creates a new GitDirExists. */
  public GitDirExists() {}

  /**
   * Returns true if the {@code .git} file or directory exists.
   *
   * @return true if the {@code .git} file or directory exists
   */
  @Override
  public Boolean obtain() {
    return new File(getParameters().getGitDirPath().get()).exists();
  }
}
