package parser.semantic;

import parser.LeftNode;
import symbols.*;

import java.util.*;

/**
 * @Author: Wang keLong
 * @DateTime: 20:11 2021/5/8
 * 变量名是由字符串表示的
 * 语义栈中的内容各种各样，表示各个非终结符的属性
 */
public class SemanticAction {
    //语义栈
    private Deque<Attribute> stack = new LinkedList<>();
    private int tempNum = 0;
    private Stack<SymbolTable> symTabStack = new Stack<>();
    private List<Quadruple> threeAddressCode = new ArrayList<>();

    public SemanticAction() {
        stack = new LinkedList<>();
        tempNum = 0;
        symTabStack = new Stack<>();
        threeAddressCode = new ArrayList<>();
        //建立第一个符号表
        SymbolTable st = new SymbolTable();
        st.setPrevious(null);
        symTabStack.push(st);
    }

    /**
     * 得到一个临时变量
     */
    public String getNewTemp() {
        String s = "t" + tempNum;
        tempNum++;
        return s;
    }

    public void push(String name, Object value) {
        stack.push(new Attribute(name, value));
    }

    public void gen(Quadruple q) {
        threeAddressCode.add(q);
    }

    public List<Quadruple> getThreeAddressCode() {
        return threeAddressCode;
    }

    /**
     * 获取下一条指令的标号
     */
    private int getNextInstr() {
        return threeAddressCode.size();
    }

    /**
     * 为某一条指令拉链
     */
    private List<Integer> makeList(int nextInstr) {
        List<Integer> list = new ArrayList<>();
        list.add(nextInstr);
        return list;
    }

    /**
     * 用一个标号回填
     */
    private void backPatch(List<Integer> list, int instr) {
        for (Integer i : list) {
            Quadruple q = threeAddressCode.get(i);
            if (q.getOp().equals("goto") && q.getResult().equals("_")) {
                q.setResult(Integer.toString(instr));
            }
        }
    }

    /**
     * 合并两个拉链的列表
     */
    private List<Integer> merge(List<Integer> list1, List<Integer> list2) {
        List<Integer> result = new ArrayList<>(list1);
        result.addAll(list2);
        return result;
    }

    /**
     * primary_expression -> id
     */
    public void primaryId(LeftNode production) throws Exception {
        SymbolTable st = symTabStack.peek();
        String lexeme = production.getRight().get(0).getTokenDescription();
        if (st.contains(lexeme)) {
            stack.push(new Attribute("type", st.getById(lexeme).getType()));
            stack.add(new Attribute("id", lexeme));
        } else {
            throw new Exception("变量" + lexeme + "未声明");
        }
    }

    /**
     * primary_expression -> real
     */
    public void primaryReal(LeftNode production) {
        Float value = Float.parseFloat(production.getRight().get(0).getTokenDescription());
        stack.push(new Attribute("type", Type.Float));
        stack.offer(new Attribute("real.value", value));
    }

    /**
     * primary_expression -> integer
     */
    public void primaryInteger(LeftNode production) {
        Integer value = Integer.parseInt(production.getRight().get(0).getTokenDescription());
        stack.push(new Attribute("type", Type.Int));
        stack.offer(new Attribute("integer.value", value));
    }

    /**
     * primary_expression -> character
     */
    public void primaryCharacter(LeftNode production) {
        stack.push(new Attribute("type", Type.Char));
        String value = production.getRight().get(0).getTokenDescription();
        stack.push(new Attribute("char.value", value));
    }

    /**
     * 当计算布尔值时,通过'control'指明是值还是控制流
     * primary_expression -> true
     * primary_expression.value
     * primary_expression.type
     */
    public void primaryTrue(LeftNode production) {
        Attribute a;
        if (!stack.isEmpty()) {
            a = stack.peek();
            if (a.getValue().toString().equals("control")) {
                stack.pop();
                List<Integer> trueList = makeList(getNextInstr());
                List<Integer> falseList = new ArrayList<>();
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", falseList));
                stack.push(new Attribute("trueList", trueList));
                return;
            }
        }
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("boolean.true", "true"));
    }

    /**
     * primary_expression -> false
     * primary_expression.value
     * primary_expression.type
     */
    public void primaryFalse(LeftNode production) {
        Attribute a;
        if (!stack.isEmpty()) {
            a = stack.peek();
            if (a.getValue().toString().equals("control")) {
                stack.pop();
                List<Integer> falseList = makeList(getNextInstr());
                List<Integer> trueList = new ArrayList<>();
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("trueList", trueList));
                stack.push(new Attribute("falseList", falseList));
                return;
            }
        }
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("boolean.false", "false"));
    }

    /**
     * postfix_expression -> postfix_expression [ expression ] 综合属性往上算
     * condition：
     * expression.addr
     * expression.type
     * postfix_expression.addr
     * postfix_expression.type
     * result:
     * postfix_expression.addr
     * postfix_expression.type
     * <p>
     * a[1]=10
     * t1 =C(a)
     * t2 = 1 * 4
     * t1[t2]=10
     * y=a[2]
     * #################################################################
     */
    public void postfix2(LeftNode production) throws Exception {
        Attribute addr = stack.pop(); //值或者id
        Type type = (Type) stack.pop().getValue();
        Attribute posAddr = stack.pop(); // id 或者是已经计算的数组值
        if (type.equals(Type.Int)) {
            throw new Exception("数组下标不是整数");
        }
        if (posAddr.getName().equals("id")) {
            //取出数组的基地址
            Array arrayType = (Array) stack.pop().getValue();
            String a = getNewTemp();
            String c = getNewTemp();
            String b = getNewTemp();
            //数组的基地址设定为a
//            t1=C(Y)
//            t2 = width * e.addr
//            t3 =t1[t2]
            gen(new Quadruple("=", posAddr.getValue().toString(), "", a));
            gen(new Quadruple("*", addr.getValue().toString(), "" + arrayType.getType().getWidth(), c));
            gen(new Quadruple("=[]", a, addr.getValue().toString(), b));
            stack.push(new Attribute("type", arrayType.getType()));
            stack.push(new Attribute("addr", b));
        } else {
            //t1=C(Y)
            //t2 = width * e1.addr
            //t3 = width * e2.addr
            //t4 = t2+t3
            //t5 =t1[t4]  t5 使用的变量还是原来那个变量
            Attribute typeA = stack.pop();
            Type arrayType = (Type) typeA.getValue();
            Quadruple q = threeAddressCode.get(threeAddressCode.size() - 1);
            threeAddressCode.remove(threeAddressCode.size() - 1);
            String t2 = q.getArg2();
            String t3 = getNewTemp();
            String t4 = getNewTemp();
            gen(new Quadruple("*", addr.getValue().toString(), "" + arrayType.getWidth(), t3));
            gen(new Quadruple("+", t2, t3, t4));
            q.setArg2(t4);
            gen(q);
            stack.push(typeA);
            stack.push(posAddr);
        }
    }

    /**
     * postfix_expression -> postfix_expression_1 ( )
     * postfix_expression_1.addr(id)
     * postfix_expression_1.type(Function)
     */
    public void postfix3(LeftNode production) throws Exception {
        String funcName = stack.pop().getValue().toString();
        Function func = (Function) stack.pop().getValue();
        if (func.getReturnType().equals(Type.Void)) {
            gen(new Quadruple("call", funcName, "", ""));
        } else {
            String a = getNewTemp();
            gen(new Quadruple("call", funcName, "", a));
            stack.push(new Attribute("addr", a));
        }
    }

    /**
     * argument_expression_list -> assignment_expression
     * assignment_expression.addr
     * assignment_expression.type
     */
    public void argument_expression1(LeftNode production) {
        Attribute addr = stack.pop();
        Attribute type = stack.pop();
        //type可以检查 参数类型是否匹配
        gen(new Quadruple("param", addr.toString(), "", ""));
        stack.pop();
        stack.push(new Attribute("param_num", 1));
    }

    /**
     * argument_expression_list -> argument_expression_list_1 , assignment_expression
     * assignment_expression.addr
     * assignment_expression.type
     * argument_expression_list_1.param_num
     */
    public void argument_expression2(LeftNode production) {
        Attribute addr = stack.pop();
        Attribute type = stack.pop();
        //type可以检查 参数类型是否匹配
        Integer param_num = (Integer) stack.pop().getValue();
        gen(new Quadruple("param", (String) addr.getValue(), "", ""));
        stack.push(new Attribute("param_num", param_num + 1));
    }

    /**
     * postfix_expression -> postfix_expression_1 ( argument_expression_list )
     * argument_expression_list.param_num
     * postfix_expression_1.addr(id)
     * postfix_expression_1.type(Function)
     */
    public void postfix4(LeftNode production) {
        Integer param_num = (Integer) stack.pop().getValue();
        String funcName = stack.pop().getValue().toString();
        Function func = (Function) stack.pop().getValue();
        if (func.getReturnType().equals(Type.Void)) {
            gen(new Quadruple("call", funcName, param_num.toString(), ""));
        } else {
            String a = getNewTemp();
            gen(new Quadruple("call", funcName, param_num.toString(), a));
            stack.push(new Attribute("addr", a));
        }

    }

    /**
     * postfix_expression -> postfix_expression_1 ++
     * postfix_expression_1.addr
     * postfix_expression_1.type
     */
    public void postfix5(LeftNode production) throws Exception {
        Attribute addr = stack.pop();
        Attribute typeA = stack.pop();
        Type type = (Type) typeA.getValue();
        if (type.equals(Type.Int) || type.equals(Type.Float)) {
            stack.push(typeA);
            stack.push(new Attribute("addr", (Integer) addr.getValue() + 1));
        } else {
            throw new Exception("类型不匹配,无法应用++");
        }
    }

    /**
     * postfix_expression -> postfix_expression_1 --
     * postfix_expression_1.addr
     * postfix_expression_1.type
     */
    public void postfix6(LeftNode production) throws Exception {
        Attribute addr = stack.pop();
        Attribute typeA = stack.pop();
        Type type = (Type) typeA.getValue();
        if (type.equals(Type.Int) || type.equals(Type.Float)) {
            stack.push(typeA);
            stack.push(new Attribute("addr", (Integer) addr.getValue() - 1));
        } else {
            throw new Exception("类型不匹配,无法应用--");
        }
    }

    /**
     * unary_expression -> ++ postfix_expression
     * postfix_expression_1.addr
     * postfix_expression_1.type
     */
    public void unary2(LeftNode production) throws Exception {
        postfix5(production);
    }

    /**
     * unary_expression -> -- postfix_expression
     * postfix_expression_1.addr
     * postfix_expression_1.type
     */
    public void unary3(LeftNode production) throws Exception {
        postfix6(production);
    }

    /**
     * unary_expression -> unary_operator unary_expression
     * (普通表达式)
     * unary_expression.addr
     * unary_expression.type
     * unary_operator.op
     * 控制流语句
     * unary_expression.trueList
     * unary_expression.falseList
     * unary_operator.op=!
     */
    public void unary4(LeftNode production) {
        Attribute addr = stack.pop(); // 数字 或者是 id
        Attribute typeA = stack.pop();
        Attribute op = stack.pop();
        String opS = (String) op.getValue();
        String a = getNewTemp();
        switch (opS) {
            case "+":
                break;
            case "-":
                gen(new Quadruple("minus", addr.getValue().toString(), "", a));
                break;
            case "!":
                if (Type.Boolean.equals(typeA.getValue())) { //数值运算
                    gen(new Quadruple("not", addr.getValue().toString(), "", a));
                } else {
                    //交换trueList和falseList
                    stack.push(addr);
                    stack.push(typeA);
                }
                break;
        }
    }

    /**
     * unary_operator -> +
     */
    public void unaryOp1(LeftNode production) {
        stack.push(new Attribute("op", "+"));
    }

    /**
     * unary_operator -> -
     */
    public void unaryOp2(LeftNode production) {
        stack.push(new Attribute("op", "-"));
    }

    /**
     * unary_operator -> !
     */
    public void unaryOp3(LeftNode production) {
        stack.push(new Attribute("op", "!"));
    }

    /**
     * 类型转换(只有INT和FLOAT)
     */
    public Type mergeType(Type typeA, Type typeB) {
        Type type = Type.Int;
        if (typeA.equals(Type.Float) || typeB.equals(Type.Float)) {
            type = Type.Float;
        }
        return type;
    }

    /**
     * multiplicative_expression -> multiplicative_expression * unary_expression
     * unary_expression.addr
     * unary_expression.type
     * multiplicative_expression.addr
     * multiplicative_expression.type
     */
    public void multiplicative2(LeftNode production) throws Exception {
        Attribute unary = stack.pop();
        Type typeU = (Type) stack.pop().getValue();
        Attribute multi = stack.pop();
        Type typeM = (Type) stack.pop().getValue();
        if (!(typeU.equals(Type.Int) || typeU.equals(Type.Float))) {
            throw new Exception(typeU.getContent() + "不可应用*");
        }
        if (!(typeM.equals(Type.Int) || typeM.equals(Type.Float))) {
            throw new Exception(typeM.getContent() + "不可应用*");
        }
        String a = getNewTemp();
        gen(new Quadruple("*", multi.getValue().toString(), unary.getValue().toString(), a));
        stack.push(new Attribute("type", mergeType(typeU, typeM)));
        stack.push(new Attribute("addr", a));
    }

    /**
     * multiplicative_expression -> multiplicative_expression / unary_expression
     * unary_expression.addr
     * unary_expression.type
     * multiplicative_expression.addr
     * multiplicative_expression.type
     */
    public void multiplicative3(LeftNode production) throws Exception {
        Attribute unary = stack.pop();
        Type typeU = (Type) stack.pop().getValue();
        Attribute multi = stack.pop();
        Type typeM = (Type) stack.pop().getValue();
        String a = getNewTemp();
        if (!(typeU.equals(Type.Int) || typeU.equals(Type.Float))) {
            throw new Exception(typeU.getContent() + "不可应用/");
        }
        if (!(typeM.equals(Type.Int) || typeM.equals(Type.Float))) {
            throw new Exception(typeM.getContent() + "不可应用/");
        }
        gen(new Quadruple("/", multi.getValue().toString(), unary.getValue().toString(), a));
        stack.push(new Attribute("type", mergeType(typeU, typeM)));
        stack.push(new Attribute("addr", a));

    }

    /**
     * multiplicative_expression -> multiplicative_expression % unary_expression
     * unary_expression.addr
     * unary_expression.type
     * multiplicative_expression.addr
     * multiplicative_expression.type
     */
    public void multiplicative4(LeftNode production) throws Exception {
        Attribute unary = stack.pop();
        Type typeU = (Type) stack.pop().getValue();
        Attribute multi = stack.pop();
        Type typeM = (Type) stack.pop().getValue();
        String a = getNewTemp();
        if (!(typeU.equals(Type.Int))) {
            throw new Exception(typeU.getContent() + "不可应用/");
        }
        if (!(typeM.equals(Type.Int))) {
            throw new Exception(typeM.getContent() + "不可应用/");
        }
        gen(new Quadruple("/", multi.getValue().toString(), unary.getValue().toString(), a));
        stack.push(new Attribute("type", Type.Int));
        stack.push(new Attribute("addr", a));
    }

    /**
     * additive_expression -> additive_expression + multiplicative_expression
     * multiplicative_expression.addr
     * multiplicative_expression.type
     * additive_expression.addr
     * additive_expression.type
     */
    public void additive2(LeftNode production) {
        String a = getNewTemp();
        Attribute multi = stack.pop();
        Type typeM = (Type) stack.pop().getValue();
        Attribute addi = stack.pop();
        Type typeA = (Type) stack.pop().getValue();
        //异常处理
        gen(new Quadruple("+", multi.getValue().toString(), addi.getValue().toString(), a));
        stack.push(new Attribute("type", mergeType(typeA, typeM)));
        stack.push(new Attribute("addr", a));
    }

    /**
     * additive_expression -> additive_expression - multiplicative_expression
     * multiplicative_expression.addr
     * multiplicative_expression.type
     * additive_expression.addr
     * additive_expression.type
     */
    public void additive3(LeftNode production) {
        String a = getNewTemp();
        Attribute multi = stack.pop();
        Type typeM = (Type) stack.pop().getValue();
        Attribute addi = stack.pop();
        Type typeA = (Type) stack.pop().getValue();
        //异常处理
        gen(new Quadruple("-", multi.getValue().toString(), addi.getValue().toString(), a));
        stack.push(new Attribute("type", mergeType(typeA, typeM)));
        stack.push(new Attribute("addr", a));
    }

    /**
     * relational_expression -> relational_expression > additive_expression
     * additive_expression.addr
     * additive_expression.type
     * relational_expression.addr
     * relational_expression.type
     * control?
     */
    public void relation2(LeftNode production) {
        Attribute addi = stack.pop();
        Attribute TypeA = stack.pop();
        Attribute rela = stack.pop();
        Attribute TypeR = stack.pop();
        //确定是算术运算还是控制流
        if (!stack.isEmpty()) {
            if (stack.peek().getName().equals("control")) { //控制流
                stack.pop();
                List<Integer> BTrueList = makeList(getNextInstr());
                List<Integer> BFalseList = makeList(getNextInstr() + 1);
                gen(new Quadruple(">", rela.getValue().toString(), addi.getValue().toString(), "_"));
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", BFalseList));
                stack.push(new Attribute("trueList", BTrueList));
                return;
            }
        }
        //算术运算,使用数值表示
        String a = getNewTemp();
        int instr = getNextInstr() + 3;
        gen(new Quadruple(">", rela.getValue().toString(), addi.getValue().toString(), "" + instr));
        gen(new Quadruple("=", "0", "", a)); //false
        int instr1 = getNextInstr() + 2;
        gen(new Quadruple("goto", "", "", "" + instr1));
        gen(new Quadruple("=", "1", "", "a")); //true
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("addr", a));
    }

    /**
     * relational_expression -> relational_expression < additive_expression
     * additive_expression.addr
     * additive_expression.type
     * relational_expression.addr
     * relational_expression.type
     * control?
     * if t1<t2 goto true
     * t3=0
     * goto next
     * t3=1 (true)
     * (next)
     */
    public void relation3(LeftNode production) {
        Attribute addi = stack.pop();
        Attribute TypeA = stack.pop();
        Attribute rela = stack.pop();
        Attribute TypeR = stack.pop();
        //确定是算术运算还是控制流
        if (!stack.isEmpty()) {
            if (stack.peek().getName().equals("control")) { //控制流
                stack.pop();
                List<Integer> BTrueList = makeList(getNextInstr());
                List<Integer> BFalseList = makeList(getNextInstr() + 1);
                gen(new Quadruple("<", rela.getValue().toString(), addi.getValue().toString(), "_"));
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", BFalseList));
                stack.push(new Attribute("trueList", BTrueList));
                return;
            }
        }
        //算术运算,使用数值表示
        String a = getNewTemp();
        int instr = getNextInstr() + 3;
        gen(new Quadruple("<", rela.getValue().toString(), addi.getValue().toString(), "" + instr));
        gen(new Quadruple("=", "0", "", a)); //false
        int instr1 = getNextInstr() + 2;
        gen(new Quadruple("goto", "", "", "" + instr1));
        gen(new Quadruple("=", "1", "", "a")); //true
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("addr", a));
    }

    /**
     * relational_expression -> relational_expression >= additive_expression
     * additive_expression.addr
     * additive_expression.type
     * relational_expression.addr
     * relational_expression.type
     * control?
     */
    public void relation4(LeftNode production) {
        Attribute addi = stack.pop();
        Attribute TypeA = stack.pop();
        Attribute rela = stack.pop();
        Attribute TypeR = stack.pop();
        //确定是算术运算还是控制流
        if (!stack.isEmpty()) {
            if (stack.peek().getName().equals("control")) { //控制流
                stack.pop();
                List<Integer> BTrueList = makeList(getNextInstr());
                List<Integer> BFalseList = makeList(getNextInstr() + 1);
                gen(new Quadruple(">=", rela.getValue().toString(), addi.getValue().toString(), "_"));
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", BFalseList));
                stack.push(new Attribute("trueList", BTrueList));
                return;
            }
        }
        //算术运算,使用数值表示
        String a = getNewTemp();
        int instr = getNextInstr() + 3;
        gen(new Quadruple(">=", rela.getValue().toString(), addi.getValue().toString(), "" + instr));
        gen(new Quadruple("=", "0", "", a)); //false
        int instr1 = getNextInstr() + 2;
        gen(new Quadruple("goto", "", "", "" + instr1));
        gen(new Quadruple("=", "1", "", "a")); //true
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("addr", a));
    }

    /**
     * relational_expression -> relational_expression <= additive_expression
     * additive_expression.addr
     * additive_expression.type
     * relational_expression.addr
     * relational_expression.type
     * control?
     */
    public void relation5(LeftNode production) {
        Attribute addi = stack.pop();
        Attribute TypeA = stack.pop();
        Attribute rela = stack.pop();
        Attribute TypeR = stack.pop();
        //确定是算术运算还是控制流
        if (!stack.isEmpty()) {
            if (stack.peek().getName().equals("control")) { //控制流
                stack.pop();
                List<Integer> BTrueList = makeList(getNextInstr());
                List<Integer> BFalseList = makeList(getNextInstr() + 1);
                gen(new Quadruple("<=", rela.getValue().toString(), addi.getValue().toString(), "_"));
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", BFalseList));
                stack.push(new Attribute("trueList", BTrueList));
                return;
            }
        }
        //算术运算,使用数值表示
        String a = getNewTemp();
        int instr = getNextInstr() + 3;
        gen(new Quadruple("<=", rela.getValue().toString(), addi.getValue().toString(), "" + instr));
        gen(new Quadruple("=", "0", "", a)); //false
        int instr1 = getNextInstr() + 2;
        gen(new Quadruple("goto", "", "", "" + instr1));
        gen(new Quadruple("=", "1", "", "a")); //true
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("addr", a));
    }

    /**
     * equality_expression -> equality_expression == relational_expression
     * relational_expression.addr
     * relational_expression.type
     * equality_expression.addr
     * equality_expression.type
     * control?getName()
     */
    public void equality2(LeftNode production) {
        Attribute rela = stack.pop();
        Attribute TypeR = stack.pop();
        Attribute equa = stack.pop();
        Attribute TypeE = stack.pop();
        if (!stack.isEmpty()) {
            if (stack.peek().getName().equals("control")) {
                stack.pop();
                List<Integer> BTrueList = makeList(getNextInstr());
                List<Integer> BFalseList = makeList(getNextInstr() + 1);
                gen(new Quadruple("==", equa.getValue().toString(), rela.getValue().toString(), "_"));
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", BFalseList));
                stack.push(new Attribute("trueList", BTrueList));
                return;
            }
        }
        //算术运算,使用数值表示
        String a = getNewTemp();
        int instr = getNextInstr() + 3;
        gen(new Quadruple("==", equa.getValue().toString(), rela.getValue().toString(), "" + instr));
        gen(new Quadruple("=", "0", "", a)); //false
        int instr1 = getNextInstr() + 2;
        gen(new Quadruple("goto", "", "", "" + instr1));
        gen(new Quadruple("=", "1", "", "a")); //true
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("addr", a));
    }

    /**
     * equality_expression -> equality_expression != relational_expression
     * relational_expression.addr
     * relational_expression.type
     * equality_expression.addr
     * equality_expression.type
     * control?getName()
     */
    public void equality3(LeftNode production) {
        Attribute rela = stack.pop();
        Attribute TypeR = stack.pop();
        Attribute equa = stack.pop();
        Attribute TypeE = stack.pop();
        if (!stack.isEmpty()) {
            if (stack.peek().getName().equals("control")) {
                stack.pop();
                List<Integer> BTrueList = makeList(getNextInstr());
                List<Integer> BFalseList = makeList(getNextInstr() + 1);
                gen(new Quadruple("!=", equa.getValue().toString(), rela.getValue().toString(), "_"));
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", BFalseList));
                stack.push(new Attribute("trueList", BTrueList));
                return;
            }
        }
        //算术运算,使用数值表示
        String a = getNewTemp();
        int instr = getNextInstr() + 3;
        gen(new Quadruple("!=", equa.getValue().toString(), rela.getValue().toString(), "" + instr));
        gen(new Quadruple("=", "0", "", a)); //false
        int instr1 = getNextInstr() + 2;
        gen(new Quadruple("goto", "", "", "" + instr1));
        gen(new Quadruple("=", "1", "", "a")); //true
        stack.push(new Attribute("type", Type.Boolean));
        stack.push(new Attribute("addr", a));
    }

    public void m100(LeftNode p) {
        Attribute a = stack.pop();
        Attribute b = stack.pop();
    }

    /**
     * M8 -> epsilon
     * 在逻辑运算中向后传递该运算是控制流还是普通布尔表达式计算
     */
    public void m8(LeftNode p) {
        Attribute addr = stack.pop();
        Attribute type = stack.peek();
        stack.push(addr);
        stack.push(new Attribute("instr", getNextInstr()));
        if (!Type.Boolean.equals(type.getValue())) {//算术型
            stack.push(new Attribute("control", null));
        }
    }

    /**
     * logical_and_expression -> logical_and_expression_1 && M8 equality_expression
     * M8
     * equality_expression.trueList
     * equality_expression.falseList
     * M.nextInstr
     * logical_and_expression_1.trueList
     * logical_and_expression_1.falseList
     * or
     * equality_expression.addr(t1)
     * equality_expression.type =boolean
     * M.nextInstr
     * logical_and_expression_1.addr(t2)
     * logical_and_expression_1.boolean
     * if t1==0 goto false
     * if t2==0 goto false
     * t3=1
     * goto next
     * t3=0 (false)
     */
    public void logical_and2(LeftNode p) {
        Attribute euqaAddr = stack.pop();
        Attribute typeE = stack.pop();
        Integer instr = (Integer) stack.pop().getValue();
        Attribute logiAddr = stack.pop();
        Attribute typeL = stack.pop();
        if (!Type.Boolean.equals(typeE.getValue())) { //控制流
            List<Integer> B2trueList = (List<Integer>) euqaAddr.getValue();
            List<Integer> B2falseList = (List<Integer>) typeE.getValue();
            List<Integer> B1trueList = (List<Integer>) logiAddr.getValue();
            List<Integer> B1falseList = (List<Integer>) typeL.getValue();
            List<Integer> BFalseList = merge(B1falseList, B2falseList);
            backPatch(B1trueList, instr);
            stack.push(new Attribute("falseList", BFalseList));
            stack.push(new Attribute("trueList", B2trueList));
        } else { //算术
            String t3 = getNewTemp();
            String t1 = logiAddr.getValue().toString();
            String t2 = euqaAddr.getValue().toString();
            int False = getNextInstr() + 5;
            gen(new Quadruple("==", t1, "0", "" + False));
            gen(new Quadruple("==", t2, "0", "" + False));
            gen(new Quadruple("=", "1", "", t3));
            int next = getNextInstr() + 2;
            gen(new Quadruple("goto", "", "", "" + next));
            gen(new Quadruple("=", "0", "", t3));
            stack.push(new Attribute("type", Type.Boolean));
            stack.push(new Attribute("addr", t3));
        }
    }

    /**
     * logical_or_expression -> logical_or_expression || M8 logical_and_expression
     * M8
     * equality_expression.trueList
     * equality_expression.falseList
     * M.nextInstr
     * logical_and_expression_1.trueList
     * logical_and_expression_1.falseList
     * or
     * equality_expression.addr (t1)
     * equality_expression.type =boolean
     * M.nextInstr
     * logical_and_expression_1.addr (t2)
     * logical_and_expression_1.boolean
     * if t1==1 goto true
     * if t2==1 goto true
     * t3=0
     * goto next
     * t3=1 (true)
     */
    public void logical_or2(LeftNode p) {
        Attribute euqaAddr = stack.pop();
        Attribute typeE = stack.pop();
        Integer instr = (Integer) stack.pop().getValue();
        Attribute logiAddr = stack.pop();
        Attribute typeL = stack.pop();
        if (!Type.Boolean.equals(typeE.getValue())) { //控制流
            List<Integer> B2trueList = (List<Integer>) euqaAddr.getValue();
            List<Integer> B2falseList = (List<Integer>) typeE.getValue();
            List<Integer> B1trueList = (List<Integer>) logiAddr.getValue();
            List<Integer> B1falseList = (List<Integer>) typeL.getValue();
            List<Integer> BTrueList = merge(B1trueList, B2trueList);
            backPatch(B1falseList, instr);
            stack.push(new Attribute("falseList", B2falseList));
            stack.push(new Attribute("trueList", BTrueList));
        } else { //算术
            String t3 = getNewTemp();
            String t1 = logiAddr.getValue().toString();
            String t2 = euqaAddr.getValue().toString();
            int True = getNextInstr() + 5;
            gen(new Quadruple("==", t1, "1", "" + True));
            gen(new Quadruple("==", t2, "1", "" + True));
            gen(new Quadruple("=", "0", "", t3));
            int next = getNextInstr() + 2;
            gen(new Quadruple("goto", "", "", "" + next));
            gen(new Quadruple("=", "1", "", t3));
            stack.push(new Attribute("type", Type.Boolean));
            stack.push(new Attribute("addr", t3));
        }
    }

    /**
     * assignment_expression -> unary_expression assignment_operator assignment_expression
     * assignment_expression.addr
     * assignment_expression.type
     * assignment_operator.op
     * unary_expression.addr
     * unary_expression.type
     */
    public void assignment2(LeftNode p) throws Exception {
        Attribute assignAddr = stack.pop();
        Attribute typeA = stack.pop();
        String op = stack.pop().getValue().toString();
        Attribute unaryAddr = stack.pop();
        Attribute unaryType = stack.pop();
        if (typeA.getValue().equals(unaryType.getValue())) {
            //如果是=，则不需要用到第二个参数
            if (op.equals("=")) {
                gen(new Quadruple(op, assignAddr.getValue().toString(), "", unaryAddr.getValue().toString()));
            } else {
                gen(new Quadruple(op, assignAddr.getValue().toString(), unaryAddr.getValue().toString(), unaryAddr.getValue().toString()));
            }
        } else {
            throw new Exception("赋值号左右类型不匹配");
        }
    }

    /**
     * assignment_operator -> =
     */
    public void assignmentOp1(LeftNode p) {
        stack.push(new Attribute("assOp", "="));
    }

    /**
     * assignment_operator -> +=
     */
    public void assignmentOp2(LeftNode p) {
        stack.push(new Attribute("assOp", "+"));
    }

    /**
     * assignment_operator -> -=
     */
    public void assignmentOp3(LeftNode p) {
        stack.push(new Attribute("assOp", "-"));
    }

    /**
     * assignment_operator -> *=
     */
    public void assignmentOp4(LeftNode p) {
        stack.push(new Attribute("assOp", "*"));
    }

    /**
     * assignment_operator -> /=
     */
    public void assignmentOp5(LeftNode p) {
        stack.push(new Attribute("assOp", "/"));
    }


    /**
     * expression_statement -> ;
     */
    public void expressionS1(LeftNode p) {
        List<Integer> list = makeList(getNextInstr());
        stack.push(new Attribute("nextList", list));
    }

    /**
     * expression_statement -> expression ;
     */
    public void expressionS2(LeftNode p) {
        List<Integer> list = makeList(getNextInstr());
        stack.push(new Attribute("nextList", list));
    }

    //从头开始

    /**
     * M2 -> epsilon
     */
    public void m2(LeftNode production) {
        SymbolTable st = new SymbolTable();
        st.setPrevious(null);
        symTabStack.push(st);
    }

    /**
     * declaration -> type_specifier ;
     */
    public void declaration1(LeftNode p) {
        List<Integer> list = makeList(getNextInstr());
        stack.push(new Attribute("nextList", list));
    }

    /**
     * declaration -> type_specifier init_declarator_list ;
     */
    public void declaration2(LeftNode p) {
        stack.pop(); //弹出type
        List<Integer> list = makeList(getNextInstr());
        stack.push(new Attribute("nextList", list));
    }

    /**
     * type_specifier -> int
     */
    public void type_specifier1(LeftNode p) {
        stack.push(new Attribute("type", Type.Int));
    }

    /**
     * type_specifier -> float
     */
    public void type_specifier2(LeftNode p) {
        stack.push(new Attribute("type", Type.Float));
    }

    /**
     * type_specifier -> boolean
     */
    public void type_specifier3(LeftNode p) {
        stack.push(new Attribute("type", Type.Boolean));
    }

    /**
     * type_specifier -> char
     */
    public void type_specifier4(LeftNode p) {
        stack.push(new Attribute("type", Type.Char));
    }

    /**
     * type_specifier -> void
     */
    public void type_specifier5(LeftNode p) {
        stack.push(new Attribute("type", Type.Void));
    }
    //结构体跳过

    /**
     * init_declarator_list -> init_declarator
     */
    public void init_declarator_list1(LeftNode p) {

    }

    /**
     * init_declarator_list -> init_declarator_list , init_declarator
     */
    public void init_declarator_list2(LeftNode p) {

    }

    /**
     * init_declarator -> declarator
     * declarator.id
     * type_specifier.type
     * 将一个id加入符号表条目中
     */
    public void init_declarator1(LeftNode p) {
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        SymbolTable st = symTabStack.peek();
        st.addEntry(new SymbolTableEntry(id.getValue().toString(), (Type) type.getValue()));
        stack.push(type);
    }

    /**
     * init_declarator -> declarator = initializer
     * initializer.addr
     * initializer.type
     * declarator.id
     * type_specifier.type
     */
    public void init_declarator2(LeftNode p) throws Exception {
        Attribute addrI = stack.pop();
        Attribute typeI = stack.pop();
        Attribute id = stack.pop();
        Attribute typeD = stack.pop();
        if (typeD.getValue().equals(typeI.getValue())) {
            gen(new Quadruple("=", addrI.getValue().toString(), "", id.getValue().toString()));
            SymbolTable st = symTabStack.peek();
            st.addEntry(new SymbolTableEntry(id.getValue().toString(), (Type) typeD.getValue()));
            stack.push(typeD);
        } else {
            throw new Exception("初始化类型不匹配");
        }
    }

    /**
     * declarator -> direct_declarator
     */
    public void declarator1(LeftNode p) {
    }

    /**
     * direct_declarator -> id
     * direct_declarator.id
     * type_specifier.type
     */
    public void direct_declarator1(LeftNode p) {
        String lexeme = p.getRight().get(0).getTokenDescription();
        stack.add(new Attribute("id", lexeme));
    }

    /**
     * direct_declarator -> direct_declarator [ constant_expression ]
     * constant_expression.addr
     * constant_expression.type
     * direct_declarator.id
     * type_specifier.type(数组or函数)
     */
    public void direct_declarator2(LeftNode p) throws Exception {
        Attribute addr = stack.pop();
        Type typeC = (Type) stack.pop().getValue();
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        if (typeC.equals(Type.Int)) {
            Array a = new Array((Integer) addr.getValue(), typeC);
            stack.push(new Attribute("type", a));
            stack.push(new Attribute("id", id));
        } else {
            throw new Exception("声明数组使用不是整数");
        }

    }

    /**
     * direct_declarator -> direct_declarator M3 ( parameter_list )
     * direct_declarator -> direct_declarator M3 ( )
     */
    public void direct_declarator3(LeftNode p) {

    }

    /**
     * M3 -> epsilon
     * 为该函数建立词条，并且进入该函数的的符号表
     */
    public void m3(LeftNode p) {
        Attribute id = stack.pop();
        assert id.getName().equals("id");
        Attribute returnType = stack.pop();
        SymbolTable st = symTabStack.peek();
        SymbolTable funcSt = new SymbolTable();
        Function func = new Function((Type) returnType.getValue());
        func.setFuncBody(funcSt);
        funcSt.setPrevious(st);
        st.addEntry(new SymbolTableEntry(id.getValue().toString(), func));
        symTabStack.push(funcSt);
    }

    /**
     * parameter_list -> parameter_declaration
     */
    public void parameter_list1(LeftNode p) {

    }

    /**
     * parameter_list -> parameter_list , parameter_declaration
     */
    public void parameter_list2(LeftNode p) {

    }

    /**
     * parameter_declaration -> type_specifier declarator
     * declarator.id
     * declarator.type
     * 向当前符号表中加入函数参数
     */
    public void parameter_declaration1(LeftNode p) {
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        SymbolTable st = symTabStack.peek();
        st.addEntry(new SymbolTableEntry(id.getValue().toString(), (Type) type.getValue()));
        stack.push(type);
    }

    /**
     * function_definition -> type_specifier declarator compound_statement
     * 结束一个函数定义
     * compound_statement.nextList有return在最后的话是没有语句需要回填的
     * 如果有return,则函数不需要回填
     * 如果没有return
     */
    public void function_definition1(LeftNode p) {
//        stack.pop(); //nextList
        symTabStack.pop();
    }


    /**
     * initializer -> assignment_expression
     */
    public void initializer1(LeftNode p) {

    }

    /**
     * initializer -> { initializer_list }
     */
    public void initializer2(LeftNode p) {

    }

    /**
     * declaration_list -> declaration
     */
    public void declaration_list1(LeftNode p) {

    }

    /**
     * declaration_list -> declaration_list declaration
     * 弹出declaration_list1的nextInstr
     */
    public void declaration_list2(LeftNode p) {
        Attribute instr2 = stack.pop();
        Attribute instr1 = stack.pop();
        stack.push(instr2);
    }


    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////

    /**
     * M5 -> epsilon 新建符号表
     * 遇见程序块,新建一个符号表
     */
    public void m5(LeftNode p) {
        SymbolTable oldSt = symTabStack.peek();
        SymbolTable st = new SymbolTable();
        st.setPrevious(oldSt);
        symTabStack.push(st);
    }

    /**
     * compound_statement -> M5 { statement_list }
     * compound_statement -> M5 { declaration_list }
     * 结束程序块,返回上一个符号表
     * 继承nextList
     */
    public void compound2(LeftNode p) {
        symTabStack.pop();
    }

    /**
     * compound_statement -> M5 { declaration_list statement_list }
     * statement_list.nextList
     * declaration_list.nextList
     */
    public void compound4(LeftNode p) {
        symTabStack.pop();
        Attribute nextList1 = stack.pop();
        stack.pop();
        stack.push(nextList1);
    }

    /**
     * statement_list -> statement_list M6 statement
     * statement.nextList
     * M6.instr
     * statement.nextList
     */
    public void statement_list2(LeftNode p) {
        List<Integer> s2List = (List<Integer>) stack.pop().getValue();
        Integer instr = (Integer) stack.pop().getValue();
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        backPatch(s1List, instr);
        stack.push(new Attribute("nextList", s2List));
    }

    /**
     * jump_statement -> return expression ;
     * expression.addr
     * expression.type
     * jump待解决检查返回类型和实际类型不同的问题
     */
    public void jump1(LeftNode p) {
        Attribute addr = stack.pop();
        Attribute type = stack.pop();
        gen(new Quadruple("return", "", "", addr.getValue().toString()));
    }

    /**
     * jump_statement -> return ;
     */
    public void jump2(LeftNode p) {
        gen(new Quadruple("return", "", "", ""));
    }

    /**
     * M6 -> epsilon
     * 获取下一行语句的标号
     */
    public void m6(LeftNode p) {
        stack.push(new Attribute("instr", getNextInstr()));
    }

    /**
     * M9 -> epsilon
     * 为表达式添加控制流信息
     */
    public void m9(LeftNode p) {
        stack.push(new Attribute("control", null));
    }

    /**
     * selection_statement -> if M9 ( expression ) M6 statement
     */
    public void selection1(LeftNode p) {
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer instr = (Integer) stack.pop().getValue();
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        List<Integer> sList = merge(s1List, falseList);
        backPatch(trueList, instr);
        stack.push(new Attribute("nextList", sList));
    }

    /**
     * M7 -> epsilon
     * 增加一条未填goto语句，并获取其标号拉链
     */
    public void m7(LeftNode p) {
        List<Integer> li = makeList(getNextInstr());
        stack.push(new Attribute("nextList", li));
        gen(new Quadruple("goto", "", "", "_"));
        m6(p);
    }

    /**
     * selection_statement -> if M9 ( expression ) M6 statement else M6 statement
     * statement.nextList
     * M6.instr2
     * M7.nextList
     * statement.nextList
     * expression.trueList
     * expression.falseList
     */
    public void selection2(LeftNode p) {
        List<Integer> s2List = (List<Integer>) stack.pop().getValue();
        Integer instr2 = (Integer) stack.pop().getValue();
        List<Integer> m7List = (List<Integer>) stack.pop().getValue();
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer instr1 = (Integer) stack.pop().getValue();
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        backPatch(trueList, instr1);
        backPatch(falseList, instr2);
        List<Integer> temp = merge(s1List, m7List);
        List<Integer> sList = merge(temp, s2List);
        stack.push(new Attribute("nextList", sList));
    }

    /**
     *
     */
    public void m10(LeftNode p) {
        m6(p);
        m9(p);
    }

    /**
     * iteration_statement -> while M10 ( expression ) M6 statement
     * statement.nextList
     * M6.instr2
     * expression.trueList
     * expression.falseList
     * M6.instr1
     */
    public void iteration1(LeftNode p) {
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer instr2 = (Integer) stack.pop().getValue();
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        Integer instr1 = (Integer) stack.pop().getValue();
        backPatch(trueList, instr2);
        backPatch(s1List, instr1);
        List<Integer> sList = falseList;
        gen(new Quadruple("goto", "", "", "" + instr1));
        stack.push(new Attribute("nextList", sList));
    }

    /**
     * iteration_statement -> do M6 statement while M9 ( expression ) ;
     * expression.trueList
     * expression.falseList
     * statement.nextList
     * M.instr
     */
    public void iteration2(LeftNode p) {
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer instr = (Integer) stack.pop().getValue();
        backPatch(trueList, instr);
        List<Integer> sList = falseList;
        gen(new Quadruple("goto", "", "", "" + instr));
        stack.push(new Attribute("nextList", sList));
    }

}
