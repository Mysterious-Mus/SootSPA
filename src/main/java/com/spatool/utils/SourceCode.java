package com.spatool.utils;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class SourceCode {
    /**
     * get the source code at the given line number
     */
    public static String get(String classPath, String className, int lineNum) {
        String fileName = classPath + "/" + className + ".java";
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(fileName));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (lineNum > 0 && lineNum <= lines.size()) {
            return lines.get(lineNum - 1);
        } else {
            return null;
        }
    }
}
