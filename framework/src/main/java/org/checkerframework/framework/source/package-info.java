/**
 * Contains the essential functionality for interfacing a compile-time (source) type-checker plug-in
 * to the Java compiler. This allows a checker to use the compiler's error reporting mechanism and
 * to access abstract syntax trees and compiler utility classes.
 *
 * <p>Most classes won't want to extend the classes in this package directly; the classes in the
 * {@code org.checkerframework.common.basetype} package provide subtype checking functionality.
 *
 * @checker_framework.manual #creating-a-checker Writing a Checker
 */
package org.checkerframework.framework.source;
