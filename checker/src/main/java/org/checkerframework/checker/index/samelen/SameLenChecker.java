package org.checkerframework.checker.index.samelen;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * An internal checker that collects information about arrays that have the same length. It is used
 * by the Upper Bound Checker.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@RelevantJavaTypes({CharSequence.class, Object[].class})
@SuppressWarningsPrefix({"index", "samelen"})
public class SameLenChecker extends BaseTypeChecker {}
