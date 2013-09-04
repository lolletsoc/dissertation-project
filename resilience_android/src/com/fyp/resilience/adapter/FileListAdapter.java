
package com.fyp.resilience.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.fyp.resilience.R;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.view.FileView;

public class FileListAdapter extends BaseAdapter {

    private List<DataWhole> mDataWholeList;

    private final Context mContext;
    private final LayoutInflater mInflater;

    public FileListAdapter(final Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mDataWholeList = ResilienceController.getInstance(mContext).getDataWholeCache();
    }

    @Override
    public int getCount() {
        return null != mDataWholeList ? mDataWholeList.size() : 0;
    }

    @Override
    public DataWhole getItem(final int position) {
        return mDataWholeList.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        /*
         * Check if View is null. An inflation may not be required due to
         * recycling. The adapter has an interal recycler which re-uses views so
         * they don't need to be constantly inflated
         */
        if (null == convertView) {
            /* Inflate the view and DON'T attach to root */
            convertView = mInflater.inflate(R.layout.file_fragment_item_view, parent, false);
        }

        final FileView fileView = (FileView) convertView;
        final DataWhole dataWhole = getItem(position);
        fileView.setDataWhole(dataWhole);

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        mDataWholeList = ResilienceController.getInstance(mContext).getDataWholeCache();
        super.notifyDataSetChanged();
    }
}
