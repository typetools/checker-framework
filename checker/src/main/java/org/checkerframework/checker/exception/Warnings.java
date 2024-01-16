package org.checkerframework.checker.exception;

import java.io.FileNotFoundException;
import java.net.BindException;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.MissingResourceException;
import java.util.jar.JarException;
import javax.naming.InsufficientResourcesException;

/** Warnings for Exception Security Checker. */
public enum Warnings {
  /** Warning for FileNotFoundException */
  FILE_NOT_FOUND(FileNotFoundException.class, "warning.file_not_found"),

  /** Warning for SQLException */
  SQL(SQLException.class, "warning.sql"),

  /** Warning for BindException */
  BIND(BindException.class, "warning.bind"),

  /** Warning for ConcurrentModificationException */
  CONCURRENT_MODIFICATION(ConcurrentModificationException.class, "warning.concurrent_modification"),

  /** Warning for InsufficientResourcesException */
  INSUFFICIENT_RESOURCES(InsufficientResourcesException.class, "warning.insufficient_resources"),

  /** Warning for MissingResourceException */
  MISSING_RESOURCE(MissingResourceException.class, "warning.missing_resource"),

  /** Warning for JarException */
  JAR(JarException.class, "warning.jar"),

  /** Warning for OutOfMemoryError */
  OUT_OF_MEMORY(OutOfMemoryError.class, "warning.out_of_memory"),

  /** Warning for StackOverflowError */
  STACK_OVERFLOW(StackOverflowError.class, "warning.stack_overflow");

  /** Stores the information about the class of the Exception */
  private final Class<?> exceptionClass;

  /** Stores the information about the respective warning */
  private final String warning;

  /**
   * The warning "pair" with the exception and its error message
   *
   * @param clazz The class of the exception
   * @param warning The warning message id in messages.properties
   */
  Warnings(Class<? extends Throwable> clazz, String warning) {
    this.exceptionClass = clazz;
    this.warning = warning;
  }

  /**
   * Getter for the class
   *
   * @return The class of the exception
   */
  public Class<?> getExceptionClass() {
    return exceptionClass;
  }

  /**
   * Getter for the message
   *
   * @return The message of the exception
   */
  public String getWarning() {
    return warning;
  }
}
