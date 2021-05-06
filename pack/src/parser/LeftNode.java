package parser;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Wang keLong
 * @DateTime: 20:23 2021/4/23
 * 文法左部的非终结符,用来表示一条文法
 */
public class LeftNode {
    private RightNode value;
    //    private List<List<RightNode>> rights = new ArrayList<>();
    private List<RightNode> right = new ArrayList<>();

    public LeftNode(RightNode value) {
        this.value = value;
    }

    public RightNode getValue() {
        return value;
    }

    public void setValue(RightNode value) {
        this.value = value;
    }

    public List<RightNode> getRight() {
        return right;
    }

    public void setRight(List<RightNode> right) {
        this.right = right;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(value);
        stringBuilder.append(" ->");
        for (RightNode rightNode : right) {
            stringBuilder.append(" ").append(rightNode.getValue());
        }
        return stringBuilder.toString();
    }


}
