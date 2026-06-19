package tiiehenry.code.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ListPopupWindow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import tiiehenry.code.language.Language;
import tiiehenry.code.language.text.TextLanguage;
import tiiehenry.code.praser.Variable;
import tiiehenry.code.view.autocomplete.AutoCompleteData;

public class AutoCompletePanel implements ColorScheme.OnColorChangedListener, OnItemClickListener {
    private final String TAG = "AutoCompletePanel";

    @Override
    public void onColorChanged() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(ColorScheme.Colorable.AUTOCOMPLETE_BACKGROUND.getColor());
        gd.setCornerRadius(5);
//        window.setBackgroundDrawable(gd);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        _filter.abort();
        AutoCompleteData data = _adapter.getItem(position);
        String text = data.text;
        boolean isFunc = text.contains("(");
        String commitText;
        if (isFunc) {
            commitText = text.substring(0, text.indexOf('(')) + "()";
        } else {
            commitText = text;
        }
        isFunc = isFunc&&!text.contains("()");
        if (editor.getSetting().isCompatibilityMode) {
            if (commitText.startsWith(_constraint.toString())) {
                String p = commitText.substring(_constraint.length());
                editor.fieldController.replaceText(
                        editor.getCaretPosition(),
                        0,
                        p);
            } else {
                editor.fieldController.replaceText(editor.getCaretPosition() - _constraint.length(), _constraint.length(), commitText);
            }
            editor._inputConnection.commitTextDisableOnce = true;
            editor.fieldController.stopTextComposing();
        } else {
            editor.replaceText(editor.getCaretPosition() - _constraint.length(), _constraint.length(), commitText);
        }
        if (isFunc) {
            editor.moveCaretLeft();
        }
        dismiss();
    }


    private final EditorField editor;

    public ListPopupWindow window;
    private AutoCompleteListAdapter _adapter;
    private final AutoCompleteFilter _filter = new AutoCompleteFilter();

    private int _verticalOffset;
    private int _horizontal;
    private CharSequence _constraint;

    AutoCompletePanel(EditorField textField) {
        editor = textField;
        initAutoCompletePanel(textField.getContext());
    }

    public void setBackground(Drawable color) {
        window.setBackgroundDrawable(color);
    }

    private void initAutoCompletePanel(Context c) {
        window = new ListPopupWindow(c);
        window.setAnchorView(editor);
        window.setOnItemClickListener(this);
        setHeight(300);
        ColorScheme.addOnColorChangedListener(this);
    }

    public void setAdapter(AutoCompleteListAdapter a) {
        _adapter = a;
        window.setAdapter(_adapter);
    }

    public void setWidth(int width) {
        window.setWidth(width);
    }

    private void setHeight(int height) {
        window.setHeight(height);
    }

    private void setHorizontalOffset(int horizontal) {
        horizontal = Math.min(horizontal, editor.getWidth() / 2);
        if (_horizontal != horizontal) {
            _horizontal = horizontal;
            window.setHorizontalOffset(horizontal);
        }
    }


    private void setVerticalOffset(int verticalOffset) {
        //verticalOffset=Math.min(verticalOffset,editor.getWidth()/2);
        int max = -window.getHeight();
        if (verticalOffset > max) {
            editor.scrollBy(0, verticalOffset - max);
            verticalOffset = max;
        }
        if (_verticalOffset != verticalOffset) {
            _verticalOffset = verticalOffset;
            window.setVerticalOffset(verticalOffset);
        }
    }

    public void update(CharSequence constraint) {
        if (_adapter != null) {
            _filter.restart();
            _filter.filter(constraint);
        }
    }

    public void show() {
        if (!window.isShowing())
            window.show();
        window.getListView().setFadingEdgeLength(0);
    }

    public void dismiss() {
        if (window.isShowing()) {
            window.dismiss();
        }
    }

    public ListPopupWindow getPanel() {
        return window;
    }


    public static abstract class AutoCompleteListAdapter<VH> extends BaseAdapter {

        public final LayoutInflater layoutInflater;
        private final ArrayList<AutoCompleteData> dataList = new ArrayList<>();

        public AutoCompleteListAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        public abstract int getItemHeight();

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public AutoCompleteData getItem(int position) {
            return dataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public List<AutoCompleteData> getDataList() {
            return dataList;
        }

        public void setDataList(List<AutoCompleteData> data) {
            if (data == null)
                return;
            dataList.clear();
            dataList.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VH holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(getLayoutId(), null, false);
                holder = newViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (VH) convertView.getTag();
            }

            AutoCompleteData item = getItem(position);
            if (item != null) {
                convert(holder, item, position);
            }
            return convertView;
        }

        /**
         * 创建ViewHolder
         *
         * @param convertView
         * @return
         */
        protected abstract VH newViewHolder(View convertView);

        /**
         * 获取适配的布局ID
         *
         * @return
         */
        protected abstract int getLayoutId();

        /**
         * 转换布局
         *
         * @param holder
         * @param item
         * @param position
         */
        protected abstract void convert(VH holder, AutoCompleteData item, int position);

    }


    public class AutoCompleteFilter extends Filter {
        private final Flag _abort = new Flag();


        private Language language = TextLanguage.getInstance();

        public void abort() {
            _abort.set();
        }

        public void restart() {
            _abort.clear();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            language = editor.getLanguage();
            ArrayList<AutoCompleteData> result = new ArrayList<>();

            String input = String.valueOf(constraint).toLowerCase();
            if (input.length() == 0)
                return new FilterResults();
            String[] ss = input.split("\\.");
            if (ss.length == 2) {//ab.c
                String pkg = ss[0];
                input = ss[1];
                addBasePackageToResult(input, pkg, result);
            } else if (ss.length == 1) {//ab.
                if (input.charAt(input.length() - 1) == '.') {
                    String pkg = input.substring(0, input.length() - 1);
                    addBasePackageToResult(pkg, result);
                    input = "";
                } else {//ab
                    //keywords first
                    addKeywordToResult(input, result);
                    addBlockVariableToResult(input, result);
                    //internal function
                    addInternalFuncToResult(input, result);
                }
            }
            _constraint = input;
            FilterResults filterResults = new FilterResults();
            filterResults.values = result;   // results是上面的过滤结果
            filterResults.count = result.size();  // 结果数量
            return filterResults;
        }

        /**
         * 本方法在UI线程执行，用于更新自动完成列表
         */
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results != null && results.count > 0 && !_abort.isSet()) {
                // 有过滤结果，显示自动完成列表
                _adapter.setDataList((List<AutoCompleteData>) results.values);
                int y = editor.getCaretY() + editor.drawer.getRowHeight() / 2 - editor.getScrollY();
                setHeight(_adapter.getItemHeight() * Math.min(editor.getSetting().autoCompleteItemCount, results.count));

                setHorizontalOffset(editor.getCaretX() - editor.getScrollX());
                setVerticalOffset(y - editor.getHeight());//editor.getCaretY()-editor.getScrollY()-editor.getHeight());
                show();
            } else {
                // 无过滤结果，关闭列表
                _adapter.notifyDataSetInvalidated();
            }
        }


        void addBasePackageToResult(String input, String pkg, ArrayList<AutoCompleteData> result) {
            if (language.isBasePackage(pkg)) {
                String[] keywords = language.getBasePackage(pkg);
                for (String k : keywords) {
                    if (k.toLowerCase().startsWith(input))
                        result.add(new AutoCompleteData(Variable.Type.Package, k));
                }
            }
        }

        void addBasePackageToResult(String pkg, ArrayList<AutoCompleteData> result) {
            if (language.isBasePackage(pkg)) {
                String[] keywords = language.getBasePackage(pkg);
                for (String k : keywords) {
                    result.add(new AutoCompleteData(Variable.Type.Package, k));
                }
            }
        }

        void addKeywordToResult(String input, ArrayList<AutoCompleteData> result) {
            String[] keywords = language.getKeywords();
            for (String k : keywords) {
                if (k.indexOf(input) == 0)
                    result.add(new AutoCompleteData(Variable.Type.Keyword, k));
            }
        }

        void addInternalFuncToResult(String input, ArrayList<AutoCompleteData> result) {
            String[] internalFuncs = language.getInternalFuncs();
            for (String k : internalFuncs) {
                if (k.toLowerCase().startsWith(input))
                    result.add(new AutoCompleteData(Variable.Type.Func_Internal, k));
            }
        }

        void addBlockVariableToResult(String input, ArrayList<AutoCompleteData> result) {
            int index = editor.getSelectionStart();
            List<Variable> variables = editor.getLexTask().getBlockVariableList();
            HashSet<String> paramList = new HashSet<>();
            HashSet<String> funcList = new HashSet<>();
            HashSet<String> varList = new HashSet<>();
            HashSet<String> packageList = new HashSet<>();
            for (Variable v : variables) {
                if (v.startIndex <= index && v.endIndex >= index) {//is in block
                    for (int i = 0; i < v.typeList.size(); i++) {
                        Variable.Type type = v.typeList.get(i);
                        String name = v.nameList.get(i);
                        if (name.toLowerCase().startsWith(input))
                            switch (type) {
                                case Param:
                                    paramList.add(name);
                                    break;
                                case Var:
                                    varList.add(name);
                                    break;
                                case Func_User:
                                    funcList.add(name);
                                    break;
                                case Package:
                                    packageList.add(name);
                                    break;
                            }
                    }
                }
            }
            for (String n : paramList) {
                result.add(new AutoCompleteData(Variable.Type.Param, n));
            }
            for (String n : varList) {
                result.add(new AutoCompleteData(Variable.Type.Var, n));
            }
            for (String n : funcList) {
                result.add(new AutoCompleteData(Variable.Type.Func_User, n));
            }
            for (String n : packageList) {
                result.add(new AutoCompleteData(Variable.Type.Package, n));
            }
        }

    }

}
