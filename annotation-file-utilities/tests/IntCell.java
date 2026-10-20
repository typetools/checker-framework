package org.checkerframework.afu.annotator.tests;

public class IntCell {
  private int i;

  public IntCell(int in) {
    this.i = in;
  }

  public void set(int in) {
    this.i = in;
  }

  public int get() {
    return i;
  }
}
