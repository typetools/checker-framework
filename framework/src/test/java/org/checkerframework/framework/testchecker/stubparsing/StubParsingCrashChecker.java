package org.checkerframework.framework.testchecker.stubparsing;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.stub.AnnotationFileElementTypes;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

/**
 * Tests that {@link AnnotationFileElementTypes#parseStubFiles} resets its {@code parsing} field
 * when parsing throws an exception. If the field were left set, then every later query of the
 * {@link AnnotationFileElementTypes} would silently return no annotations, discarding every
 * annotation that had been read from a stub file.
 *
 * <p>This checker throws an exception from {@link #getExtraStubFiles}, which {@code parseStubFiles}
 * calls.
 */
public class StubParsingCrashChecker extends BaseTypeChecker {

  /** Creates a StubParsingCrashChecker. */
  public StubParsingCrashChecker() {}

  /** Thrown by {@link StubParsingCrashChecker#getExtraStubFiles}. */
  static class StubParsingCrashException extends RuntimeException {

    /** Unique identifier for serialization. */
    private static final long serialVersionUID = 20260919L;

    /** Creates a StubParsingCrashException. */
    StubParsingCrashException() {
      super("Thrown by StubParsingCrashChecker, to test recovery from a stub parsing failure.");
    }
  }

  @Override
  public List<String> getExtraStubFiles() {
    throw new StubParsingCrashException();
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new BaseTypeVisitor<StubParsingCrashAnnotatedTypeFactory>(this) {
      @Override
      protected StubParsingCrashAnnotatedTypeFactory createTypeFactory() {
        return new StubParsingCrashAnnotatedTypeFactory(checker);
      }
    };
  }

  /** The type factory for {@link StubParsingCrashChecker}. */
  class StubParsingCrashAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a StubParsingCrashAnnotatedTypeFactory.
     *
     * @param checker the checker that uses this type factory
     */
    public StubParsingCrashAnnotatedTypeFactory(BaseTypeChecker checker) {
      super(checker);
      postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
      return new LinkedHashSet<>(Arrays.asList(SubQual.class, SuperQual.class));
    }

    @Override
    protected void parseAnnotationFiles() {
      try {
        stubTypes.parseStubFiles();
        throw new AssertionError("getExtraStubFiles() should have thrown an exception");
      } catch (StubParsingCrashException e) {
        // This is expected; StubParsingCrashChecker.getExtraStubFiles() always throws.
      }
      if (stubTypes.isParsing()) {
        throw new AssertionError(
            "AnnotationFileElementTypes.parseStubFiles() left `parsing` set after it threw an"
                + " exception");
      }
      ajavaTypes.parseAjavaFiles();
    }
  }
}
