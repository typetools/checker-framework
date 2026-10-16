package org.checkerframework.afu.scenelib.field;

public interface AFTVisitor<R, T> {
  R visitAnnotationAFT(AnnotationAFT aft, T arg);

  R visitArrayAFT(ArrayAFT aft, T arg);

  R visitBasicAFT(BasicAFT aft, T arg);

  R visitClassTokenAFT(ClassTokenAFT aft, T arg);

  R visitEnumAFT(EnumAFT aft, T arg);
}
