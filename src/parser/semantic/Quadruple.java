package parser.semantic;

/**
 * @Author: Wang keLong
 * @DateTime: 16:12 2021/5/6
 */
public class Quadruple {
    private String op = "";
    private String arg1 = "";
    private String arg2 = "";
    private String result = "";

    public Quadruple() {
    }

    public Quadruple(String op, String arg1, String arg2, String result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getArg1() {
        return arg1;
    }

    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String transToThreeAddressCode() {
        StringBuilder sb = new StringBuilder();
        switch (op) {
            // x =y op z
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
                sb.append(result).append(" = ").append(arg1).append(" ").append(op).append(" ").append(arg2);
                break;
            //单目运算符
            case "minus":
                sb.append(result).append(" = ").append(op).append(" ").append(arg1);
            case "=":
               sb.append(result).append(" ").append(op).append(" ").append(arg1);
                break;
            case "goto":
                sb.append(op).append(" ").append(result);
                break;
            case ">":
            case "<":
            case "<=":
            case ">=":
            case "==":
            case "!=":
                sb.append("if ").append(arg1).append(op).append(arg2).append(" goto ").append(result);
                break;
            case "=[]":
            case "[]=":
            case ".=":
            case "=.":
                sb.append(result).append(" = ").append(arg1);
                break;
            case "param":
            case "return":
                sb.append(op).append(" ").append(arg1);
                break;
            case "call":
                if (result.length() > 0) {
                    sb.append(result).append("=").append(op).append(" ").append(arg1).append(" ").append(arg2);
                } else {
                    sb.append(op).append(" ").append(arg1).append(" ").append(arg2);
                }
                break;
            case "comment":
                sb.append(result).append(".");
                break;
            default:
                sb.append(toString());
                break;
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "[\'" + op + "\' " + arg1 + " " + arg2 + " " + result+"]";
    }
}
