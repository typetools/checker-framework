package org.checkerframework.checker.testchecker.nullnesssub;

import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.SupportedOptions;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A subclass of {@link NullnessChecker} that adds no behavior, other than using {@link
 * NullnessSubclassAnnotatedTypeFactory} as its type factory.
 */
// javax.annotation.processing.SupportedOptions is not inherited, so it must be repeated here.
@SupportedOptions({"assumeKeyFor", "invocationPreservesArgumentNullness"})
public class NullnessSubclassChecker extends NullnessChecker {

  /** Creates a NullnessSubclassChecker. */
  public NullnessSubclassChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new NullnessSubclassVisitor(this);
  }

  @Override
  public List<String> getExtraStubFiles() {
    // org.checkerframework.framework.qual.StubFiles is not inherited, so NullnessChecker's stub
    // files must be obtained here.  They cannot be listed in a @StubFiles annotation on this
    // class, because the basename in such an annotation is resolved relative to the package of
    // the checker class that bears the annotation, and the stub files are in NullnessChecker's
    // package rather than in this class's package.  An absolute resource name works, and
    // `getExtraStubFiles()` accepts one.
    String stubDirectory = "/" + NullnessChecker.class.getPackageName().replace('.', '/') + "/";
    StubFiles stubFiles = NullnessChecker.class.getAnnotation(StubFiles.class);
    return Arrays.stream(stubFiles.value()).map(stubFile -> stubDirectory + stubFile).toList();
  }
}
