package tiiehenry.code.praser;

import java.util.ArrayList;


public interface Blockable extends Drawer{
    int startIndex();

    int endIndex();

    ArrayList<Blockable> children();

    Blockable parent();

    String getText();
//    Drawer getDrawer();

}
