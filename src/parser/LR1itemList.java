package parser;

import java.util.List;
import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 20:02 2021/4/25
 */
public class LR1itemList {
    private List<LR1item> items;

    public LR1itemList(List<LR1item> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LR1itemList that = (LR1itemList) o;

        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}
