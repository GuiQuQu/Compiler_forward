package parser.semantic;

import org.w3c.dom.Attr;
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
            q.setResult(Integer.toString(instr));
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
        stack.offer(new Attribute("real.value", value));
    }

    /**
     * primary_expression -> integer
     */
    public void primaryInteger(LeftNode production) {
        Integer value = Integer.parseInt(production.getRight().get(0).getTokenDescription());
        stack.offer(new Attribute("integer.value", value));
    }

    /**
     * primary_expression -> character
     */
    public void primaryCharacter(LeftNode production) {
//        Character value =production.getRight().get(0).getTokenDescription();
    }

    public void primaryTrue(LeftNode production) {
        List<Integer> trueList = makeList(getNextInstr());
        List<Integer> falseList = new ArrayList<>();
        gen(new Quadruple("goto", "", "", "_"));
        stack.push(new Attribute("falseList", falseList));
        stack.push(new Attribute("trueList", trueList));

    }

    public void primaryFalse(LeftNode production) {
        List<Integer> falseList = makeList(getNextInstr());
        List<Integer> trueList = new ArrayList<>();
        gen(new Quadruple("goto", "", "", "_"));
        stack.push(new Attribute("trueList", trueList));
        stack.push(new Attribute("falseList", falseList));
    }

    /**
     * postfix_expression -> postfix_expression [ expression ]
     */
    public void postfix2(LeftNode production) {
        Attribute addr = stack.pop();
        assert addr.getName().equals("addr");
        if (stack.peek().getName().equals("id")) { //第一维
            SymbolTable st = symTabStack.peek();
            SymbolTableEntry ste = st.getById((String) stack.peek().getValue());
            Integer i = (Integer) addr.value * ste.getType().getWidth();
            stack.push(new Attribute("offset", i));
        } else { //多维
            Attribute offset = stack.pop();
            assert stack.peek().getName().equals("id");
            SymbolTable st = symTabStack.peek();
            SymbolTableEntry ste = st.getById((String) stack.peek().getValue());
            Integer i = (Integer) addr.getValue() * ste.getType().getWidth() + (Integer) offset.getValue();
            stack.push(new Attribute("offset", i));
        }
    }

    /**
     * postfix_expression -> postfix_expression_1 ( )
     */
    public void postfix3(LeftNode production) {
        String a = getNewTemp();
        gen(new Quadruple("call", (String) stack.peek().getValue(), "", a));
        stack.push(new Attribute("addr", a));
    }

    /**
     * argument_expression_list -> assignment_expression
     */
    public void argument_expression1(LeftNode production) {
        gen(new Quadruple("param", (String) stack.peek().getValue(), "", ""));
        stack.pop();
        stack.push(new Attribute("param_num", 1));
    }

    /**
     * argument_expression_list -> argument_expression_list_1 , assignment_expression
     */
    public void argument_expression2(LeftNode production) {
        Attribute addr = stack.pop();
        gen(new Quadruple("param", (String) addr.getValue(), "", ""));
        stack.peek().setValue((Integer) stack.peek().getValue() + 1);
    }

    /**
     * postfix_expression -> postfix_expression_1 ( argument_expression_list )
     */
    public void postfix4(LeftNode production) throws Exception {
        String a = getNewTemp();
        Integer param_num = (Integer) stack.pop().getValue();
        String addr = (String) stack.pop().getValue();
        SymbolTable st = symTabStack.peek();
        if (st.isUseEntry(addr)) {
            gen(new Quadruple("call", addr, Integer.toString(param_num), a));
            stack.push(new Attribute("addr", a));
        } else {
            throw new Exception("函数未定义");
        }
    }

    /**
     * postfix_expression -> postfix_expression_1 ++
     */
    public void postfix5(LeftNode production) {
        stack.peek().setValue((Integer) stack.peek().getValue() + 1);
    }

    /**
     * postfix_expression -> postfix_expression_1 --
     */
    public void postfix6(LeftNode production) {
        stack.peek().setValue((Integer) stack.peek().getValue() - 1);
    }

    /**
     * unary_expression -> ++ postfix_expression
     */
    public void unary2(LeftNode production) {
        stack.peek().setValue((Integer) stack.peek().getValue() + 1);
    }

    /**
     * unary_expression -> -- postfix_expression
     */
    public void unary3(LeftNode production) {
        stack.peek().setValue((Integer) stack.peek().getValue() - 1);
    }

    /**
     * unary_expression -> unary_operator unary_expression
     * 布尔表达式部分待确定
     */
    public void unary4(LeftNode production) {
        Attribute addr = stack.pop(); // 数字 或者是 id
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
                gen(new Quadruple("not", addr.getValue().toString(), "", a));
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
     * multiplicative_expression -> multiplicative_expression * unary_expression
     */
    public void multiplicative2(LeftNode production) {
        String a = getNewTemp();
        Attribute unary = stack.pop();
        Attribute multi = stack.pop();
        gen(new Quadruple("*", multi.getValue().toString(), unary.getValue().toString(), a));
        stack.push(new Attribute("addr", a));
    }

    /**
     * multiplicative_expression -> multiplicative_expression / unary_expression
     */
    public void multiplicative3(LeftNode production) {
        String a = getNewTemp();
        Attribute unary = stack.pop();
        Attribute multi = stack.pop();
        gen(new Quadruple("/", multi.getValue().toString(), unary.getValue().toString(), a));
        stack.push(new Attribute("addr", a));

    }

    /**
     * multiplicative_expression -> multiplicative_expression % unary_expression
     */
    public void multiplicative4(LeftNode production) {
        String a = getNewTemp();
        Attribute unary = stack.pop();
        Attribute multi = stack.pop();
        gen(new Quadruple("%", multi.getValue().toString(), unary.getValue().toString(), a));
        stack.push(new Attribute("addr", a));
    }

    /**
     * additive_expression -> additive_expression + multiplicative_expression
     */
    public void additive2(LeftNode production) {
        String a = getNewTemp();
        Attribute multi = stack.pop();
        Attribute addi = stack.pop();
        gen(new Quadruple("+", multi.getValue().toString(), addi.getValue().toString(), a));
        stack.push(new Attribute("addr", a));
    }

    /**
     * additive_expression -> additive_expression - multiplicative_expression
     */
    public void additive3(LeftNode production) {
        String a = getNewTemp();
        Attribute multi = stack.pop();
        Attribute addi = stack.pop();
        gen(new Quadruple("-", multi.getValue().toString(), addi.getValue().toString(), a));
        stack.push(new Attribute("addr", a));
    }

    /**
     * relational_expression -> relational_expression > additive_expression
     */
    public void relation2(LeftNode production) {

    }

    /**
     * relational_expression -> relational_expression < additive_expression
     */
    public void relation3(LeftNode production) {

    }

    /**
     * relational_expression -> relational_expression >= additive_expression
     */
    public void relation4(LeftNode production) {

    }

    /**
     * relational_expression -> relational_expression <= additive_expression
     */
    public void relation5(LeftNode production) {

    }

    /**
     * equality_expression -> equality_expression == relational_expression
     */
    public void equality2(LeftNode production) {

    }

    /**
     * equality_expression -> equality_expression != relational_expression
     */
    public void equality3(LeftNode production) {

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
    //结构体跳过

    /**
     * init_declarator_list -> init_declarator
     */
    public void init_declarator_list1(LeftNode p) {
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        SymbolTable st = symTabStack.peek();
        // 可能是其子类
        st.addEntry(new SymbolTableEntry((String) id.getValue(), (Type) type.getValue()));
    }

    /**
     * init_declarator_list -> init_declarator_list , init_declarator
     */
    public void init_declarator_list2(LeftNode p) {
        Attribute id = stack.pop();
        Attribute type = stack.peek();
        SymbolTable st = symTabStack.peek();
        // 可能是其子类
        st.addEntry(new SymbolTableEntry((String) id.getValue(), (Type) type.getValue()));
    }

    /**
     * direct_declarator -> id
     */
    public void direct_declarator1(LeftNode p) {
        String lexeme = p.getRight().get(0).getTokenDescription();
        stack.add(new Attribute("id", lexeme));
    }

    /**
     * direct_declarator -> direct_declarator [ constant_expression ]
     */
    public void direct_declarator2(LeftNode p) {
        Attribute addr = stack.pop();
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        Array a = new Array((Integer) addr.getValue(), (Type) type.getValue());
        stack.push(new Attribute("type", a));
        stack.push(new Attribute("id", id));
    }

    /**
     * direct_declarator -> direct_declarator M3 ( parameter_list )
     */
    public void direct_declarator3(LeftNode p) {

    }

    /**
     * direct_declarator -> direct_declarator M3 ( )
     */
    public void direct_declarator5(LeftNode p) {

    }

    /**
     * M3 -> epsilon
     * 为该函数建立词条，并且进入一个新的符号表
     */
    public void m3(LeftNode p) {
        Attribute id = stack.pop();
        assert id.getName().equals("id");
        Attribute returnType = stack.pop();
        Function func = new Function((Type) returnType.getValue());
        SymbolTable st = symTabStack.peek();
        st.addEntry(new SymbolTableEntry(id.getValue().toString(), func));
        SymbolTable funcSt = new SymbolTable();
        funcSt.setPrevious(st);
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
     */
    public void parameter_declaration1(LeftNode p) {
        SymbolTable st = symTabStack.peek();
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        SymbolTableEntry ste = new SymbolTableEntry(id.getValue().toString(), (Type) type.getValue());
        st.addEntry(ste);
    }

    /**
     * initializer -> assignment_expression
     */
    public void initializer1(LeftNode p) {
        Attribute addr = stack.pop();
        Attribute id = stack.peek();
        gen(new Quadruple("=", addr.getValue().toString(), "", id.getValue().toString()));
        //前面可能是一个数组
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
     * declaration_list -> declaration_list M4 declaration
     */
    public void declaration_list2(LeftNode p) {

    }

    /**
     * function_definition -> type_specifier declarator compound_statement
     */
    public void function_definition1(LeftNode p) {
        symTabStack.pop();
    }

    /**
     * M5 -> epsilon 新建符号表
     */
    public void m5(LeftNode p) {
        SymbolTable oldSt = symTabStack.peek();
        SymbolTable st = new SymbolTable();
        st.setPrevious(oldSt);
        symTabStack.push(st);
    }

    /**
     * compound_statement -> M5 { statement_list }
     */
    public void compound2(LeftNode p) {
        symTabStack.pop();
    }

    /**
     * compound_statement -> M5 { declaration_list }
     */
    public void compound3(LeftNode p) {
        symTabStack.pop();
    }

    /**
     * compound_statement -> M5 { declaration_list statement_list }
     */
    public void compound4(LeftNode p) {
        symTabStack.pop();
        List<Integer> s2List = (List<Integer>) stack.pop().getValue();
        Integer instr = (Integer) stack.pop().getValue();
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        backPatch(s1List, instr);
        stack.push(new Attribute("nextList", s2List));
    }

    /**
     * statement_list -> statement_list M6 statement
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
     * jump语句的nextList求法
     * ##############################################3
     */
    public void jump1(LeftNode p) {
        Attribute addr = stack.pop();
        gen(new Quadruple("return", "", "", addr.toString()));
    }

    /**
     * jump_statement -> return ;
     */
    public void jump2(LeftNode p) {
        gen(new Quadruple("return", "", "", ""));
    }

    /**
     * M6 -> epsilon
     */
    public void m6(LeftNode p) {
        stack.push(new Attribute("instr", getNextInstr()));
    }

    /**
     * selection_statement -> if ( expression ) M6 statement
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
     */
    public void m7(LeftNode p) {
        List<Integer> li = makeList(getNextInstr());
        stack.push(new Attribute("nextList", li));
        gen(new Quadruple("goto", "", "", "_"));
    }

    /**
     * selection_statement -> if ( expression ) M6 statement M7 else statement
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
     * iteration_statement -> while M6 ( expression ) M6 statement
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
     * iteration_statement -> do M6 statement while ( expression ) ;
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

}
