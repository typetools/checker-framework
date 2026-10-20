package org.checkerframework.afu.annotator.tests;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class BoundClassMultiple<
    T extends Date, U extends List & Serializable, V extends Comparable<V>> {}
