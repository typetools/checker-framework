package utilMDE;

import java.io.*;

/**
 * This class extends IOException by also reporting a file name and line
 * number at which the exception occurred.
 **/
public class FileIOException extends IOException {
  static final long serialVersionUID = 20050923L;

  public final String fileName;
  public final int lineNumber;

  public FileIOException() {
    super();
    fileName = null;
    lineNumber = -1;
  }

  public FileIOException(Throwable cause) {
    super(cause.getMessage());
    initCause(cause);
    fileName = null;
    lineNumber = -1;
  }

  public FileIOException(String s) {
    this(s, null, (String)null);
  }

  public FileIOException(String s, Throwable cause) {
    this(s, null, (String)null, cause);
  }

  public FileIOException(String s, LineNumberReader reader, String fileName) {
    super(s);
    this.fileName = fileName;
    if (reader != null) {
      this.lineNumber = reader.getLineNumber();
    } else {
      this.lineNumber = -1;
    }
  }

  public FileIOException(String s, LineNumberReader reader, String fileName, Throwable cause) {
    super(s);
    initCause(cause);
    this.fileName = fileName;
    if (reader != null) {
      this.lineNumber = reader.getLineNumber();
    } else {
      this.lineNumber = -1;
    }
  }

  public FileIOException(LineNumberReader reader, String fileName, Throwable cause) {
    super(cause.getMessage());
    initCause(cause);
    this.fileName = fileName;
    if (reader != null) {
      this.lineNumber = reader.getLineNumber();
    } else {
      this.lineNumber = -1;
    }
  }

  public FileIOException(String s, LineNumberReader reader, File file) {
    this(s, reader, file.getName());
  }

  public FileIOException(String s, LineNumberReader reader, File file, Throwable cause) {
    this(s, reader, file.getName(), cause);
  }

  public FileIOException(LineNumberReader reader, File file, Throwable cause) {
    this(cause.getMessage(), reader, file.getName(), cause);
  }

  public String toString() {
    if (fileName == null || lineNumber == -1) {
      return super.toString();
    } else {
      return super.toString()
        + " on line " + lineNumber
        + " of file " + fileName;
    }
  }

}
