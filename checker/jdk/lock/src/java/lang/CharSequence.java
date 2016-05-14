package java.lang;

import org.checkerframework.checker.lock.qual.GuardSatisfied;

public interface CharSequence{
  int length(@GuardSatisfied CharSequence this);
  char charAt(int a1);
  CharSequence subSequence(int a1, int a2);
  String toString(@GuardSatisfied CharSequence this);
}
