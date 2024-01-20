package com.spatool.utils;

import java.util.*;

public class ArgParse {
    
    /** 
     * generate a map from param name to param value 
     * return false on failure */
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
            else {
                System.err.println("Unknown format: " + args[i]);
                return null;
            }
        }
        return map;
    }

    /* check the availablity of required args, return true if all present, false otherwise and report usage */
    public static boolean check(Map<String, String> map, String[] requiredArgs, String[] optionalArgs) {
        boolean success = true;
        for (String arg : requiredArgs) {
            if (!map.containsKey(arg)) {
                System.err.println("Missing required argument: " + arg);
                success = false;
            }
        }
        for (String arg : map.keySet()) {
            if (!Arrays.asList(requiredArgs).contains(arg) && !Arrays.asList(optionalArgs).contains(arg)) {
                System.err.println("Unknown argument: " + arg);
                success = false;
            }
        }
        return success;
    }
}
