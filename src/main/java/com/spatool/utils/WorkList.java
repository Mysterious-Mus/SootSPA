package com.spatool.utils;

import java.util.*;

public class WorkList<T> {
    private Queue<T> workList;

    public WorkList() {
        workList = new LinkedList<T>();
    }

    public void add(T u) {
        workList.add(u);
    }

    public T remove() {
        return workList.remove();
    }

    public boolean isEmpty() {
        return workList.isEmpty();
    }

    public int size() {
        return workList.size();
    }
}