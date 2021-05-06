package parser;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 20:51 2021/4/24
 * LR0项
 */
public class LR0Item {
    private int grammarNum;
    private int dotPoint;

    public LR0Item() {
    }

    public LR0Item(int grammarNum, int dotPoint) {
        this.grammarNum = grammarNum;
        this.dotPoint = dotPoint;
    }

    public int getGrammarNum() {
        return grammarNum;
    }

    public void setGrammarNum(int grammarNum) {
        this.grammarNum = grammarNum;
    }

    public int getDotPoint() {
        return dotPoint;
    }

    public void setDotPoint(int dotPoint) {
        this.dotPoint = dotPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LR0Item lr0Item = (LR0Item) o;
        return grammarNum == lr0Item.grammarNum &&
                dotPoint == lr0Item.dotPoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(grammarNum, dotPoint);
    }
}
