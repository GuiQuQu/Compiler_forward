package parser;


import oldlexer.Token;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 20:53 2021/4/24
 * LR1项
 */
public class LR1item extends LR0Item {
    private RightNode forward; //向前看符号

    public LR1item() {

    }

    public LR1item(int grammarNum, int dotPoint, RightNode forward) {
        super(grammarNum, dotPoint);
        this.forward = forward;
    }

    public RightNode getForward() {
        return forward;
    }

    public void setForward(RightNode forward) {
        this.forward = forward;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LR1item lr1item = (LR1item) o;
        return Objects.equals(forward, lr1item.forward);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), forward);
    }
}
