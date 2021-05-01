package parser;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 20:16 2021/4/25
 */
public class LR1TableValue {
    private String action = "";
    private int use;

    public LR1TableValue() {
    }

    public LR1TableValue(String action, int use) {
        this.action = action;
        this.use = use;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getUse() {
        return use;
    }

    public void setUse(int use) {
        this.use = use;
    }

    @Override
    public String toString() {
        return "" + action +" "+use;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LR1TableValue that = (LR1TableValue) o;
        return use == that.use &&
                Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, use);
    }
}
