
package com.fyp.resilience.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.fyp.resilience.R;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.connection.Connectable;
import com.fyp.resilience.view.ConnectionView;

public class ConnectionListAdapter extends BaseAdapter {

    private List<Connectable> mConnectionList;

    private final Context mContext;
    private final LayoutInflater mInflater;

    public ConnectionListAdapter(final Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mConnectionList = ResilienceController.getInstance(mContext).getConnectionList();
    }

    @Override
    public int getCount() {
        return null != mConnectionList ? mConnectionList.size() : 0;
    }

    @Override
    public Connectable getItem(int position) {
        return mConnectionList.get(position);
    }

    @Override
    public long getItemId(int position) {
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
            convertView = mInflater.inflate(R.layout.connection_fragment_item_view, parent, false);
        }

        final ConnectionView connectionView = (ConnectionView) convertView;
        final Connectable connectable = getItem(position);
        connectionView.setConnectable(connectable);

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        mConnectionList = ResilienceController.getInstance(mContext).getConnectionList();
        super.notifyDataSetChanged();
    }
}
