# Acknowledgement
This project is forked from [noidsirius/SootTutorial](https://github.com/noidsirius/SootTutorial). I would like to thank the author for his great work.
# How to use
## Setup
- Configure environment variable `JAVA_HOME` to JDK 8

- `./gradlew.bat build`
## AvailableExp
Available Expression Analysis is used as a tool to help developers to eliminate redundant evaluations in their code.
### example: 

`./gradlew.bat run --args="AvailableExp -cp demo/AvailableExpression -c AvailableExp -m may2 -may_iter 2 -enhanced_may_iter -report_performance"`
### arguments:

- `-cp`: classpath

- `-c`: class name

- `-m`: method name

- `-may_iter`: the number of iterations for may analysis

    - this argument is optional, default value is 0

    - if the value is 0, the analysis will be a must analysis

    - if the value is greater than 0, then after the must analysis, specified number of may analysis iterations will be performed

    - For our given example, 

        `./gradlew.bat run --args="AvailableExp -cp demo/AvailableExpression -c AvailableExp -m may1 -may_iter 1"`
        
        will report:

        ```java
        Units you may optimize in <AvailableExp: void may1()>:
        demo/AvailableExpression\AvailableExp.java:6             s = a * b;
        ```

        while `./gradlew.bat run --args="AvailableExp -cp demo/AvailableExpression -c AvailableExp -m may1"` will not give any optimization suggestions because it's based on vanilla Available Expression Analysis.

        In general, more may iterations will give more optimization suggestions, but it will also take more time to run the analysis and the analysis result will be less precise.
    
- `-enhanced_may_iter`: If this argument is present, the may iterations the analysis will be enhanced in the following way:

    After each may iteration, an increment global must analysis will be performed. "Increment" means that no available expression will be removed from the result of the may iteration, because otherwise the new available expression found in the may iteration will all be removed. The purpose of the increment global must analysis is to allow successors to inherit the new available expression found in the may iterations.

    This can make may iterations more explainable. One enhanced may iteration can effectively keep an expression alive over a branch statement where it is not always killed.

    For example, 
    
    `./gradlew.bat run --args="AvailableExp -cp demo/AvailableExpression -c AvailableExp -m may1fail -may_iter 1"` 
    
    would still fail to provide any optimization suggestions. But,
    
    `./gradlew.bat run --args="AvailableExp -cp demo/AvailableExpression -c AvailableExp -m may1fail -may_iter 1 -enhanced_may_iter"` 
    
    will report the desired suggestion. You may also want to try to analyse `may2()` for more details.

    `./gradlew.bat run --args="AvailableExp -cp demo/AvailableExpression -c AvailableExp -m may2 -may_iter 2 -enhanced_may_iter"`

- `-report_performance`: If this argument is present, the analysis will report its performance, measured by how many times unit propagation is performed.
- `-use_iterative`: If this argument is present, the analysis will use iterative dataflow analysis instead of worklist algorithm. This can be used to compare the performance of these two algorithms.
# Develop Notes
## Setup and run
- setup requires gradle build

- recompile with complie.ps1 (has been written to main, this file recomplies all files in demo)

- must use java8, configure at JAVA_HOME
## Workflow
- `./gradlew.bat run` will enter `mainClassName` specified in `build.gradle`

- args can be specified by `./gradlew.bat run --args="AvailableExp"`