package org.checkerframework.afu.scenelib.toys;

public @interface FancyAnnotation {
  int myInt();

  String left();

  SimplerAnnotation[] friends();
}
