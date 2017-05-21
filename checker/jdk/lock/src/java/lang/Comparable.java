package java.lang;


import org.checkerframework.checker.lock.qual.*;

public interface Comparable<T extends Object> {
   int compareTo(@GuardSatisfied Comparable<T> this,T a1);
}
