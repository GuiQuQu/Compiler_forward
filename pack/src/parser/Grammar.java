package parser;

import java.io.*;
import java.util.*;

/**
 * @Author: Wang keLong
 * @DateTime: 19:55 2021/4/23
 */
public class Grammar {
    /**
     * 文法4要素
     * 1.开始符号
     * 2.终结符
     * 3.非终结符
     * 4.产生式
     */
    private RightNode startSym; // 开始符号
    private Set<RightNode> terminator = new HashSet<>(); //终结符
    private Set<RightNode> no_terminator = new HashSet<>(); //非终结符
    private Set<RightNode> symbol = new HashSet<>(); //终结符集和非终结符集
    private List<LeftNode> productions = new ArrayList<>(); //产生式
    private File grammar; //文法文件
    private Map<RightNode, Set<RightNode>> firsts = new HashMap<>(); //first集
    private int startNum = 0; // 语法开始符号的文法标号
    private RightNode augmentedStart;

    public Grammar(String filePath) throws IOException {
        this.grammar = new File(filePath);
        init();
        symbol.addAll(terminator);
        symbol.addAll(no_terminator);
    }

    public void init() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(grammar));

        String line = reader.readLine();
        //终结符
        if (!line.trim().equals("*terminator")) {
            return;
        }
        line = reader.readLine();
        while (!(line.trim().equals("*production"))) {
            if (line.length() > 0 && line.charAt(0) == '#' || line.trim().length() == 0) {
                line = reader.readLine();
                continue;
            }
            RightNode rn = new RightNode(line.trim(), true);
            terminator.add(rn);
            line = reader.readLine();
        }
        //产生式和非终结符
        if (!line.trim().equals("*production")) {
            return;
        }
        line = reader.readLine();
        while (!line.trim().equals("*startSym")) {
            if (line.length() > 0 && line.charAt(0) == '#' || line.trim().length() == 0) {
                line = reader.readLine();
                continue;
            }
            String[] strings = line.trim().split("\\s+");
            //左节点
            // program -> external_declaration %% m2
            // program -> %% m1
            LeftNode left = addLeftNode(strings[0]);
            no_terminator.add(left.getValue());
            //右节点
            List<RightNode> right = new ArrayList<>();
            //非空产生式
            if (strings.length > 2 && !strings[2].equals("%%")) {
                for (int i = 2; i < strings.length; i++) {
                    //语义动作
                    if ("%%".equals(strings[i])) {
                        left.setSemanticAction(strings[i + 1]);
                        i++;
                    } else {
                        RightNode rn = new RightNode(strings[i]);
                        if (terminator.contains(rn)) {
                            rn.setTerminator(true);
                        } else {
                            rn.setTerminator(false);
                            no_terminator.add(rn);
                        }
                        right.add(rn);
                    }
                }
            }
            // program -> %% m1
            if (strings.length > 2 && strings[2].equals("%%")) {
                left.setSemanticAction(strings[3]);
            }
            // program ->
            left.setRight(right);
            line = reader.readLine();
        }
        if (!line.trim().equals("*startSym")) {
            return;
        }
        line = reader.readLine();
        startSym = new RightNode(line.trim(), false);
    }


    /**
     * 添加一个产生式的左节点
     */
    public LeftNode addLeftNode(String value) {
        LeftNode left = new LeftNode(new RightNode(value));
        productions.add(left);
        return left;
    }

    public Set<RightNode> getSymbol() {
        return symbol;
    }

    public Set<RightNode> getTerminator() {
        return terminator;
    }

    public RightNode getStartSym() {
        return startSym;
    }

    public List<LeftNode> getProductions() {
        return productions;
    }

    public Set<RightNode> getNo_terminator() {
        return no_terminator;
    }

    public Map<RightNode, Set<RightNode>> getFirsts() {
        return firsts;
    }

    public int getStartNum() {
        return startNum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("terminator:\n").append(terminator).append("\n");
        sb.append("No_terminator:\n").append(no_terminator).append("\n");
        sb.append("startSym:\n").append(startSym.getValue()).append("\n");
        sb.append("productions:").append("\n");
        for (LeftNode production : productions) {
            sb.append(production.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 将文法改为增广文法
     */
    public void augmentGrammar() {
        LeftNode lf = new LeftNode(new RightNode("augmentStart"));
        List<RightNode> rightNodes = new ArrayList<>();
        rightNodes.add(startSym); //augmentStart -> program
        lf.setRight(rightNodes);
        productions.add(lf); //添加增广文法产生式
//        startSym = lf.getValue();
//        symbol.add(startSym);
        startNum = productions.size() - 1;  //开始产生式
        no_terminator.add(lf.getValue()); //将增广开始符号加入非终结符
        augmentedStart = lf.getValue();
    }

    public void calFIRST() {
        System.out.println("FIRST....");
        //init,建立所有终结符和非终结符的FIRST集合
        createValueForFirst();
        //终结符的FIRST集是他本身
        for (RightNode teNode : terminator) {
            addItemToFirst(teNode, teNode);
        }
        boolean change = true;
        //计算非终极符的FIRST集合
        while (change) {
            change = false;
            for (RightNode noTeNode : no_terminator) {
                for (LeftNode production : productions) {
                    //寻找该非终结符的所有产生式
                    if (production.getValue().equals(noTeNode)) {
                        if (production.getRight().size() == 0) { //X ->
                            if (addItemToFirst(noTeNode, RightNode.epsilon))
                                change = true;
                        } else { //X -> Y1Y2...
                            int i = 0;
                            while (i < production.getRight().size()) {
                                if (!production.getRight().get(i).equals(noTeNode)) {
                                    if (addItemToFirstWithoutEpsilon(noTeNode, firsts.get(production.getRight().get(i))))
                                        change = true;
                                }
                                if (firsts.get(production.getRight().get(i)).contains(RightNode.epsilon))
                                    i++;
                                else  //不能产生空串就退出
                                    break;
                            }
                            if (i == production.getRight().size()) { // Y1,Y2...所有产生式都为空
                                if (addItemToFirst(noTeNode, RightNode.epsilon))
                                    change = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public void calFIRST2() {
        System.out.println("FIRST...");
        //init
        createValueForFirst();
        //终结符的FIRST集
        for (RightNode rightNode : terminator) {
            addItemToFirst(rightNode, rightNode);
        }
        int no_te_i = 0;
        while (true) {
            //当前非终结终结符是no_te_i
            //寻找no_te_i的所有产生式
            for (LeftNode production : productions) {
                //空串产生式X-> epsilon
                if (production.getRight().size() == 1 && production.getRight().get(0).equals(RightNode.epsilon)) {
                    continue;
                }
                int i = 0;
                while (i < production.getRight().size()) {
                    i++;
                    return;

                }
            }
        }
    }

    public void calFIRST3() {
        System.out.println("FIRST...");
        //init
        createValueForFirst();
        //终结符的FIRST集
        for (RightNode rightNode : terminator) {
            addItemToFirst(rightNode, rightNode);
        }
        boolean haveChange = true;
        while (haveChange) {
            haveChange = false;
            for (RightNode node : no_terminator) { //对于每一个非终结符，寻该他的所有产生式
                for (LeftNode production : productions) {
                    if (production.getValue().equals(node)) { //该产生式左部是该非终结符
                        //X ->
                        if (production.getRight().size() == 0) {
                            if (addItemToFirst(production.getValue(), RightNode.epsilon)) {
                                haveChange = true;
                            }
                            continue;
                        }
                        // X->Y1Y2...
                        int i = 0;
                        while (i < production.getRight().size()) {
                            if (production.getRight().get(i).isTerminator()) { //当前字母是终结符
                                if (addItemToFirst(production.getValue(), production.getRight().get(0))) {
                                    haveChange = true;
                                }
                                break;
                            }
                            if (!production.getRight().get(i).isTerminator()) { //当前字母不是终结符
                                if (firsts.get(production.getRight().get(i)).contains(RightNode.epsilon)) { //该非终结符可以产生空串
                                    i++;
                                } else {
                                    //不是自己(X-> Y1X...)
                                    if (!production.getValue().equals(production.getRight().get(i))) {
                                        if (addItemToFirst(production.getValue(), firsts.get(production.getRight().get(i))))
                                            haveChange = true;
                                    }
                                    break;
                                }
                            }
                        }
                        //Y1Y2...均可生成空串
                        if (i == production.getRight().size()) {
                            if (addItemToFirst(production.getValue(), RightNode.epsilon)) {
                                haveChange = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public String printFIRST() {
        StringBuilder sb = new StringBuilder();
        for (RightNode rightNode : firsts.keySet()) {
            sb.append(rightNode.getValue()).append("->").append("{");
            for (RightNode node : firsts.get(rightNode)) {
                sb.append(node.getValue()).append(",");
            }
            sb.append("}").append("\n");
        }
        return sb.toString();
    }

    private void createValueForFirst() {
        for (RightNode rightNode : terminator) {
            Set<RightNode> set = new HashSet<>();
            firsts.put(rightNode, set);
        }
        for (RightNode rightNode : no_terminator) {
            Set<RightNode> set = new HashSet<>();
            firsts.put(rightNode, set);
        }
    }

    private boolean addItemToFirstWithoutEpsilon(RightNode rn, Set<RightNode> Yiset) {
        Set<RightNode> Xset = firsts.get(rn);
        int old_size = Xset.size();
        Set<RightNode> temp = new HashSet<>(Yiset);
        temp.remove(RightNode.epsilon);
        Xset.addAll(temp);
        return Xset.size() > old_size; // true 发生改变
    }

    private boolean addItemToFirst(RightNode rn, Set<RightNode> Yiset) {
        Set<RightNode> Xset = firsts.get(rn);
        int old_size = Xset.size();
        Xset.addAll(Yiset);
        return Xset.size() > old_size; // true 发生改变
    }

    private boolean addItemToFirst(RightNode rn, RightNode first) {
        Set<RightNode> set = firsts.get(rn);
        int old_size = set.size();
        set.add(first);
        int new_size = set.size();
        return new_size > old_size;  //发生改变
    }

    public RightNode getAugmentedStart() {
        return augmentedStart;
    }
}
