package com.example.mypackage;

import org.checkerframework.checker.lock.qual.GuardedBy;

import java.util.ArrayList;
import java.util.List;

public class FullyQualified {
    public static final @GuardedBy("<self>") List<Object> all_classes = new ArrayList<>();

    void test() {
        synchronized (com.example.mypackage.FullyQualified.all_classes) {
            com.example.mypackage.FullyQualified.all_classes.add(new Object());
        }
    }
}
