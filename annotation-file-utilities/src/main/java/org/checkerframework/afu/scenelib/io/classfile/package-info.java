/**
 * {@code org.checkerframework.afu.scenelib.io.classfile} provides methods for writing {@link
 * org.checkerframework.afu.scenelib.el.AScene}s to Java class files and reading in annotations from
 * a Java class file into an {@link org.checkerframework.afu.scenelib.el.AScene}. This package
 * requires the core ASM package (see <a
 * href="http://asm.objectweb.org/">http://asm.objectweb.org/</a> ). The two main methods of this
 * package are {@link org.checkerframework.afu.scenelib.io.classfile.ClassFileWriter#insert} for
 * writing annotations to a class file, and {@link
 * org.checkerframework.afu.scenelib.io.classfile.ClassFileReader#read} for reading annotations from
 * a class file.
 */
package org.checkerframework.afu.scenelib.io.classfile;
