mkdir out
javac -encoding UTF-8 -d out src/oldlexer/TokenTable.java src/oldlexer/Token.java src/oldlexer/TokenAnalysis.java src/Main.java src/parser/Grammar.java src/parser/LeftNode.java src/parser/LR0Item.java src/parser/LR1item.java src/parser/LR1TableKey.java src/parser/LR1TableValue.java src/parser/Node.java src/parser/Parser.java src/parser/RightNode.java
jar cvfm test.jar MANIFEST.MF -C out/ .
java -jar test.jar
