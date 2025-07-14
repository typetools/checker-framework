package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class GenericCell {
  private List<IntCell> internalList;

  public GenericCell(List<IntCell> list) {
    internalList = list;
  }

  public List<IntCell> getList() {
    return internalList;
  }

  public static class IntCell {
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
}
