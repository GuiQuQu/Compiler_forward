package symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 16:00 2021/5/8
 */
public class Function extends Type {
    //content 函数名
    private Type returnType; // 返回值类型
    private String FuncName; //函数名
    private SymbolTable args = new SymbolTable();// 函数体中的参数


    public Function(String funcName, Type returnType) {
        super("function", 0);
        this.returnType = returnType;
        this.FuncName = funcName;
    }

    public String getFuncName() {
        return FuncName;
    }

    public void setFuncName(String funcName) {
        FuncName = funcName;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public SymbolTable getArgs() {
        return args;
    }

    public void setArgs(SymbolTable args) {
        this.args = args;
    }

    public boolean paramCompare(List<Type> param_list) {
        if (args.size() != param_list.size()) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            Type type1 = args.get(i).getType();
            Type type2 = param_list.get(i);
            if (!type1.equals(type2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Function function = (Function) o;
        return returnType.equals(function.returnType) &&
                FuncName.equals(function.FuncName) &&
                args.equals(function.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), returnType, FuncName, args);
    }
}
