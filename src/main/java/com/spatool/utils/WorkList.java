package com.spatool.utils;

import java.util.*;

public class WorkList<T> {
    private Queue<T> workList;
    private Map<String, String> args;
    private int pollCount = 0;

    public WorkList(Map<String, String> args) {
        workList = new LinkedList<T>();
        this.args = args;
    }

    public void add(T u) {
        workList.add(u);
    }

    public T remove() {
        pollCount ++;
        return workList.remove();
    }

    public boolean isEmpty() {
        return workList.isEmpty();
    }

    public int size() {
        return workList.size();
    }

    public int getPollCount() {
        return pollCount;
    }

    public void clear() {
        workList.clear();
    }
}