package taokdao.codeeditor.layout.selectoperate;

import android.graphics.drawable.Drawable;
import android.view.View;

public class SelectOperate {
    public Drawable icon;
    public View.OnClickListener click;
    public View.OnLongClickListener longClick;

    public SelectOperate(Drawable icon, View.OnClickListener click) {
        this(icon, click, null);
    }

    public SelectOperate(Drawable icon, View.OnClickListener click, View.OnLongClickListener longClick) {
        this.icon = icon;
        this.click = click;
        this.longClick = longClick;
    }
}
