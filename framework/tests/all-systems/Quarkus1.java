@SuppressWarnings("all") // Just check for crashes.
public class Quarkus1 {
  void method(ModifiableModelEx libraryModel) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              WriteAction.run(libraryModel::commit);
            });
  }

  public static class ApplicationManager {
    protected static Application ourApplication;

    public static Application getApplication() {
      return ourApplication;
    }
  }

  public interface Application { // extends ComponentManager {}
    void invokeAndWait(Runnable runnable);
  }

  public abstract static class WriteAction<T> extends BaseActionRunnable<T> {
    public static <E extends Throwable> void run(ThrowableRunnable<E> action) throws E {
      throw new RuntimeException();
    }
  }

  public abstract static class BaseActionRunnable<T> {}

  public interface ThrowableRunnable<T extends Throwable> {
    void run() throws T;
  }

  interface ModifiableModelEx {
    void commit();
  }
}
