package com.spatool.utils;

import java.util.*;

public class ArgParse {
    
    /* generate a map from param name to param value */
    public static Map<String, String> parse(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                String key = args[i].substring(1);
                String value = "";
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    value = args[i + 1];
                    i++;
                }
                map.put(key, value);
            }
        }
        return map;
    }

    /* check the availablity of required args, return true if all present, false otherwise and report usage */
    public static boolean check(Map<String, String> map, String[] requiredArgs) {
        boolean allPresent = true;
        for (String arg : requiredArgs) {
            if (!map.containsKey(arg)) {
                System.err.println("Missing required argument: " + arg);
                allPresent = false;
            }
        }
        if (!allPresent) {
            System.err.println("Usage: java -jar <jarfile> " + String.join(" ", requiredArgs));
        }
        return allPresent;
    }
}
