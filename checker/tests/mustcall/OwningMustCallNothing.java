import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall({})
// :: error: (inconsistent.mustcall.subtype)
public class OwningMustCallNothing implements Closeable {

  protected @Owning AnnotationClassLoader loader;

  @CreatesMustCallFor("this")
  private void loadTypeAnnotationsFromQualDir() {
    if (loader != null) {
      loader.close();
    }
    loader = createAnnotationClassLoader();
  }

  AnnotationClassLoader createAnnotationClassLoader() {
    return null;
  }

  public void close() {}
}

// :: error: (inconsistent.mustcall.subtype)
@MustCall({}) class OwningMustCallNothing2 implements Closeable {

  protected @Owning AnnotationClassLoader loader;

  @CreatesMustCallFor("this")
  private void loadTypeAnnotationsFromQualDir() {
    if (loader != null) {
      loader.close();
    }
    loader = createAnnotationClassLoader();
  }

  AnnotationClassLoader createAnnotationClassLoader() {
    return null;
  }

  public void close() {}
}

@InheritableMustCall("close")
// :: error: (declaration.inconsistent.with.extends.clause)
class SubclassMustCallClose1 extends OwningMustCallNothing {}

// :: error: (declaration.inconsistent.with.extends.clause)
@MustCall("close") class SubclassMustCallClose2 extends OwningMustCallNothing {}

@InheritableMustCall("close")
// :: error: (declaration.inconsistent.with.extends.clause)
class SubclassMustCallClose3 extends OwningMustCallNothing2 {}

// :: error: (declaration.inconsistent.with.extends.clause)
@MustCall("close") class SubclassMustCallClose4 extends OwningMustCallNothing2 {}

@InheritableMustCall({}) // Don't check whether AnnotationClassLoaders are closed.
// :: error: (inconsistent.mustcall.subtype)
class AnnotationClassLoader implements Closeable {
  public void close() {}
}
