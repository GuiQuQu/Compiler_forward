package parser;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 20:14 2021/4/25
 */
public class LR1TableKey {
    private int stateNum;
    private RightNode rn;

    public LR1TableKey(int stateNum, RightNode rn) {
        this.stateNum = stateNum;
        this.rn = rn;
    }

    public LR1TableKey() {
    }

    public int getStateNum() {
        return stateNum;
    }

    public void setStateNum(int stateNum) {
        this.stateNum = stateNum;
    }

    public RightNode getRn() {
        return rn;
    }

    public void setRn(RightNode rn) {
        this.rn = rn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LR1TableKey that = (LR1TableKey) o;
        return stateNum == that.stateNum &&
                Objects.equals(rn, that.rn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stateNum, rn);
    }

    @Override
    public String toString() {
        return "(" + stateNum + "," + rn + ")";
    }
}
