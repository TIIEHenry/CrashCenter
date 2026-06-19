package tiiehenry.code.praser;

import java.util.ArrayList;

public class Variable {
    public ArrayList<Variable.Type> typeList = new ArrayList<>();
    public ArrayList<String> nameList = new ArrayList<>();
    public int startIndex;
    public int endIndex;

    public Variable parent = null;
    public ArrayList<Variable> childs = new ArrayList<>();

    public enum Type {
        Keyword, Var, Func_Internal, Func_User, Package, Param
    }

    public Variable(int startIndex) {
        this.startIndex = startIndex;
    }

    @Override
    public String toString() {
        return "type:" +/*type.toString()+",name:"+name+*/",startIndex:" + startIndex + ",endIndex:" + endIndex;
    }


}
