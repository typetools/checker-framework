package org.checkerframework.afu.annotator.tests;

public class FieldNewComplex {
  FieldNewComplex m(FieldNewComplex a, FieldNewComplex b, FieldNewComplex c) {
    return null;
  }

  FieldNewComplex f = m(new FieldNewComplex(), new FieldNewComplex(), new FieldNewComplex());
}
