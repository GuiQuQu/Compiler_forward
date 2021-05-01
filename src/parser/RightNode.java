package parser;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 20:35 2021/4/24
 * 表示一个文法符号,value是其对应的名字，如果是终结符，就是终结符对应的内容
 */
public class RightNode {
    //该语法符号保存的值,终结符就是对应的种别，非终结符就是对应文法符号的名称
    private String value; //终结符的种别或者是非终结符的名字
    private String tokenDescription; //对应token的具体的内容
    private boolean isTerminator;
    private int lineNum; //行号信息
    public static RightNode epsilon = new RightNode("epsilon", "epsilon", true);
    public static RightNode end = new RightNode("$", "$", true);

    public RightNode() {
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public int getLineNum() {
        return lineNum;
    }

    public String getTokenDescription() {
        return tokenDescription;
    }

    public void setTokenDescription(String tokenDescription) {
        this.tokenDescription = tokenDescription;
    }

    public RightNode(String value) {
        this.value = value;
    }

    public RightNode(String value, boolean isTerminator) {
        this.value = value;
        this.isTerminator = isTerminator;
        if (!isTerminator) {
            tokenDescription = "";
        }
    }

    public RightNode(String value, String tokenDescription, boolean isTerminator) {
        this.value = value;
        this.isTerminator = isTerminator;
        this.tokenDescription = tokenDescription;
    }

    public RightNode(String value, String tokenDescription, boolean isTerminator, int lineNum) {
        this.value = value;
        this.tokenDescription = tokenDescription;
        this.isTerminator = isTerminator;
        this.lineNum = lineNum;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isTerminator() {
        return isTerminator;
    }

    public void setTerminator(boolean terminator) {
        isTerminator = terminator;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RightNode rightNode = (RightNode) o;
        return Objects.equals(value, rightNode.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
