@echo off
@title 建立test.jar 为语法分析器
if not exist out (
    mkdir out
   )
javac -encoding utf-8 -d out src/oldlexer/*.java src/parser/*.java src/Main.java
jar cvfm test.jar MANIFEST.MF -C out/ .
java -jar test.jar
