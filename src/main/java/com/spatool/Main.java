package com.spatool;

import com.spatool.availableexp.AvailableExp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Main {

    private static void compileDemo() {
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "demo\\compile.ps1");
            builder.redirectErrorStream(true);
            Process p = builder.start();
            p.getOutputStream().close();
            String line;
            BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = stdout.readLine()) != null) {
                System.out.println(line);
            }
            stdout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        compileDemo();
        
        if (args.length == 0){
            System.err.println("You must provide the name of the Java class file that you want to run.");
            return;
        }
        String[] restOfTheArgs = Arrays.copyOfRange(args, 1, args.length);
        if(args[0].equals("AvailableExp"))
            AvailableExp.main(restOfTheArgs);
        else
            System.err.println("The class '" + args[0] + "' does not exists or does not have a main method.");
    }
}
