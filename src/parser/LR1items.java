package parser;

import java.util.Objects;
import java.util.Set;

/**
 * @Author: Wang keLong
 * @DateTime: 0:07 2021/5/13
 */
public class LR1items {
    private Set<LR1item> content;
    private Set<LR1item> core;

    public LR1items() {
    }

    public Set<LR1item> getContent() {
        return content;
    }

    public void setContent(Set<LR1item> content) {
        this.content = content;
    }

    public Set<LR1item> getCore() {
        return core;
    }

    public void setCore(Set<LR1item> core) {
        this.core = core;
    }
    public boolean contains(LR1item item){
        return content.contains(item);
    }
    public boolean isCore(LR1item item){
        return core.contains(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LR1items lr1items = (LR1items) o;
        return content.equals(lr1items.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
