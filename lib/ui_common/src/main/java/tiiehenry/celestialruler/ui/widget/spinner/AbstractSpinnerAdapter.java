package tiiehenry.celestialruler.ui.widget.spinner;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AbstractSpinnerAdapter<DATATYPE, VB extends ViewBinding, VBD extends ViewBinding>
        extends BaseAdapter {
    private final List<DATATYPE> mData = new ArrayList<>();

    public AbstractSpinnerAdapter() {
        super();
    }

    public AbstractSpinnerAdapter(@NonNull Collection<DATATYPE> list) {
        this();
        mData.addAll(list);
    }

    public AbstractSpinnerAdapter(@NonNull DATATYPE[] data) {
        this();
        if (data.length > 0) {
            mData.addAll(Arrays.asList(data));
        }
    }

    @NonNull
    public abstract SpinnerViewHolder<VB> onCreateViewHolder(@NonNull ViewGroup parent, int position);

    @NonNull
    public abstract SpinnerViewHolder<VBD> onCreateDropDownViewHolder(@NonNull ViewGroup parent, int position);

    public abstract void bindData(@NonNull SpinnerViewHolder<VB> holder, @NonNull DATATYPE item, int position);

    public abstract void bindDropDownData(@NonNull SpinnerViewHolder<VBD> holder, @NonNull DATATYPE item, int position);

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SpinnerViewHolder<VB> holder = onCreateViewHolder(parent, position);
        DATATYPE data = getData(position);
        bindData(holder, data, position);
        holder.itemView.setTag(holder);
        return holder.itemView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        SpinnerViewHolder<VBD> holder = onCreateDropDownViewHolder(parent, position);
        DATATYPE data = getData(position);
        bindDropDownData(holder, data, position);
        holder.itemView.setTag(holder);
        return holder.itemView;
    }

    public List<DATATYPE> getDataList() {
        return mData;
    }

    @Override
    public Object getItem(int position) {
        return getData(position);
    }

    public DATATYPE getData(int position) {
        return mData.get(position);
    }

    int getPosition(@NonNull DATATYPE item) {
        return mData.indexOf(item);
    }

    public void clear() {
        mData.clear();
        notifyDataSetChanged();
    }

    public void refresh(@NonNull Collection<DATATYPE> collection) {
        mData.clear();
        mData.addAll(collection);
        notifyDataSetChanged();
    }

    public void refresh(@NonNull DATATYPE[] array) {
        refresh(Arrays.asList(array));
    }
}
