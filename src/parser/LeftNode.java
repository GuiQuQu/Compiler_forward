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
    private List<RightNode> right = new ArrayList<>();
    private String semanticAction="";
    public LeftNode(RightNode value) {
        this.value = value;
    }

    public LeftNode(RightNode value, List<RightNode> right, String semanticAction) {
        this.value = value;
        this.right = right;
        this.semanticAction = semanticAction;
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

    public String getSemanticAction() {
        return semanticAction;
    }

    public void setSemanticAction(String semanticAction) {
        this.semanticAction = semanticAction;
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
