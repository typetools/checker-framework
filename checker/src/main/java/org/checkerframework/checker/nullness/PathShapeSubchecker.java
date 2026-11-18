package org.checkerframework.checker.nullness;

import java.io.File;
import java.nio.file.Path;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker that determines which paths have a name and which have a parent. Typically used as
 * part of the compound checker for the nullness type system, to determine when {@link
 * Path#getParent()} and related methods return a non-null value.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@RelevantJavaTypes({Path.class, File.class})
@StubFiles("pathshape.astub")
public class PathShapeSubchecker extends BaseTypeChecker {

  /** Creates a PathShapeSubchecker. */
  public PathShapeSubchecker() {}
}
