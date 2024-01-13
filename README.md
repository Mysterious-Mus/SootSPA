# Acknowledgement
This project is forked from [noidsirius/SootTutorial](https://github.com/noidsirius/SootTutorial). I would like to thank the author for his great work.
# How to use
- Configure environment variable `JAVA_HOME` to JDK 8

- `./gradlew.bat build`

- `./gradlew.bat run --args="AvailableExp"`
# Develop Notes
## Setup and run
- setup requires gradle build

- recompile with complie.ps1 (has been written to main, this file recomplies all files in demo)

- must use java8, configure at JAVA_HOME
## Workflow
- `./gradlew.bat run` will enter `mainClassName` specified in `build.gradle`

- args can be specified by `./gradlew.bat run --args="AvailableExp"`