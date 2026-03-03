/**
 * The Annotation Scene Library provides classes to represent the annotations on a Java program and
 * read and write those annotations in various formats.
 *
 * <h2>Structure</h2>
 *
 * <ul>
 *   <li>An {@link org.checkerframework.afu.scenelib.el.AScene} holds annotations for a set of
 *       classes and packages.
 *   <li>A {@link org.checkerframework.afu.scenelib.el.AElement} represents one particular element
 *       of a Java program within an {@code AScene}.
 *   <li>Package {@link org.checkerframework.afu.scenelib.io} provides routines to read and write
 *       {@link org.checkerframework.afu.scenelib.el.AScene}s in various formats.
 *   <li>An {@link org.checkerframework.afu.scenelib.Annotation} represents an annotation (which
 *       might be a field of another annotation). It can be attached to an {@link
 *       org.checkerframework.afu.scenelib.el.AElement}.
 *   <li>An {@link org.checkerframework.afu.scenelib.el.AnnotationDef} represents an annotation
 *       definition, consisting of a definition name and field names and types ({@link
 *       org.checkerframework.afu.scenelib.field.AnnotationFieldType}s). It also indicates the
 *       annotation's retention policy.
 * </ul>
 *
 * <h2>Example</h2>
 *
 * <p>The example program {@code annotations.tests.Example} demonstrates the library's
 * annotation-processing capabilities. Its source code (and also example input and output) are
 * distributed with the Annotation Scene Library.
 */
package org.checkerframework.afu.scenelib;
