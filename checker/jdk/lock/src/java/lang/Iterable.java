package java.lang;

public interface Iterable<T extends Object> {
  java.util.Iterator<T> iterator();
}
