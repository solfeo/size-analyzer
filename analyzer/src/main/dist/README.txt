Size Analyzer
=============
The Size Analyzer is a tool for developers to understand the size of their
Android application.

Prerequisites
=============
This Size Analyzer requires the Java Runtime Environment version 1.8
or greater ("Java 8"). Compatible JREs can be found at the following websites:

Oracle:     https://www.java.com/
OpenJDK:    http://openjdk.java.net/

Java also needs to be listed in the system PATH. Check the documentation for
your version of the JRE on how to do this.

Installation
============
After extracting this archive, be sure to keep all files together in the same
directory structure.

MacOS and Linux
---------------
Add the "size-analyzer" file to your PATH. In Bash:
    echo 'export PATH="$PATH:<full path of size-analyzer>"' >> ~/.bashrc && . ~/.bashrc

Windows
-------
1. Add "size-analyzer.bat" to your PATH:
    setx PATH "%PATH%;<full path of size-analyzer.bat>"

Usage
=====
There is a built-in help describing the available commands and options.
You can view it by running "size-analyzer help" from the command line.
