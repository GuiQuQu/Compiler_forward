package parser;

import java.util.List;

/**
 * @Author: Wang keLong
 * @DateTime: 22:21 2021/4/27
 */
public class Node {
    private RightNode rn;
    private List<Node> children=null;

    public Node() {
    }

    public Node(RightNode rn, List<Node> children) {
        this.rn = rn;
        this.children = children;
    }

    public RightNode getRn() {
        return rn;
    }

    public void setRn(RightNode rn) {
        this.rn = rn;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public void addChild(Node node) {
        children.add(node);
    }
    //修改一个孩子节点
    public void update(Node node,int index){

    }

    public boolean childrenEqual(List<RightNode> rns) {
        if (children.size() != rns.size()) {
            return false;
        }
        for (int i = 0; i < children.size(); i++) {
            if (!children.get(i).getRn().equals(rns.get(i))) {
                return false;
            }
        }
        return true;
    }

}
