# Acknowledgement
This project is forked from [noidsirius/SootTutorial](https://github.com/noidsirius/SootTutorial). I would like to thank the author for his great work.
# How to use
## Setup
- Configure environment variable `JAVA_HOME` to JDK 8

- `./gradlew.bat build`
## AvailableExp
example: `./gradlew.bat run --args="AvailableExp -cp demo/HelloSoot -c FizzBuzz -m available -may_iter 2"`
explanaition:

- `-cp`: classpath

- `-c`: class name

- `-m`: method name

- `-may_iter`: the number of iterations for may analysis

    - this argument is optional, default value is 0

    - if the value is 0, the analysis will be a must analysis

    - if the value is greater than 0, then after the must analysis, specified number of may analysis iterations will be performed

    - For our given example, 

        `./gradlew.bat run --args="AvailableExp -cp demo/HelloSoot -c FizzBuzz -m available -may_iter 1"` 
        
        will report:

        ```java
        Units you may optimize in <FizzBuzz: void available()>:
        line 29: i2 = b0 * b1
        ```

        while `./gradlew.bat run --args="AvailableExp -cp demo/HelloSoot -c FizzBuzz -m available -may_iter 0"` will not give any optimization suggestions because it's based on vanilla Available Expression Analysis.

        In general, more may iterations will give more optimization suggestions, but it will also take more time to run the analysis and the analysis result will be less precise.
# Develop Notes
## Setup and run
- setup requires gradle build

- recompile with complie.ps1 (has been written to main, this file recomplies all files in demo)

- must use java8, configure at JAVA_HOME
## Workflow
- `./gradlew.bat run` will enter `mainClassName` specified in `build.gradle`

- args can be specified by `./gradlew.bat run --args="AvailableExp"`