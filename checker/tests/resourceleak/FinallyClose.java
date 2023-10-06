import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

abstract class FinallyClose {

  abstract Closeable alloc() throws IOException;

  abstract Closeable derive(Closeable r) throws IOException;

  abstract String compute(Closeable resource) throws IOException;

  abstract void makeNotes() throws IOException;

  String run1() throws IOException {
    Closeable resource = null;
    try {
      resource = alloc();
      return compute(resource);
    } finally {
      try {
        makeNotes();
      } finally {
        closeResource(resource);
      }
    }
  }

  String run2() throws IOException {
    Closeable resource = null;
    Closeable subresource = null;
    try {
      resource = alloc();
      subresource = derive(resource);
      return compute(subresource);
    } finally {
      try {
        makeNotes();
      } finally {
        try {
          closeResource(subresource);
        } finally {
          closeResource(resource);
        }
      }
    }
  }

  @EnsuresCalledMethods(
      value = "#1",
      methods = {"close"})
  @EnsuresCalledMethodsOnException(
      value = "#1",
      methods = {"close"})
  void closeResource(@Nullable Closeable resource) throws IOException {
    if (resource != null) {
      try {
        resource.close();
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }
}
