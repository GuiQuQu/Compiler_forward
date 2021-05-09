package symbols;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 22:01 2021/5/6
 * 一条符号表项
 */
public class SymbolTableEntry {
    /**
     * enter(id,type,offset) 普通变量声明  type是类型表达式
     * 类型表达式需要指明的类型
     * 普通变量 (id=a,type=int,width=4,offset=4)
     * 数组变量 (id=b,type=array(4,array(4,int)),width=int.width*4,offset=20)
     * (id=c,type=struct,width=*,offset)
     * ([]= b 1 10)
     * (.= )
     * 结构体
     * 指针声明 int* p; enter(id,type[pointer(int)],offset)
     * 数组声明 enter()
     * 没有结构体声明，也没有过程声明，C--并不嵌套，这些额外的符号表都交给符号表栈来管理
     */

    private String id;
    private Type type;

    public SymbolTableEntry() {
    }

    public SymbolTableEntry(String id, Type type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return id + " " + type.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolTableEntry that = (SymbolTableEntry) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
