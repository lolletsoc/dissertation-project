
package com.fyp.resilience.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.fyp.resilience.R;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.swarm.model.SwarmClient;
import com.fyp.resilience.view.ClientView;

public class ClientListAdapter extends BaseAdapter {

    private List<SwarmClient> mClientList;

    private final Context mContext;
    private final ResilienceController mController;
    private final LayoutInflater mInflater;

    public ClientListAdapter(final Context context) {
        mContext = context;
        mController = ResilienceController.getInstance(mContext);
        mInflater = LayoutInflater.from(mContext);
        mClientList = mController.getSwarmList();
    }

    @Override
    public int getCount() {
        return null != mClientList ? mClientList.size() : 0;
    }

    @Override
    public SwarmClient getItem(int position) {
        return mClientList.get(position);
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
            convertView = mInflater.inflate(R.layout.client_fragment_item_view, parent, false);
        }

        final ClientView clientView = (ClientView) convertView;
        final SwarmClient client = getItem(position);
        clientView.setClient(client);

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        mClientList = mController.getSwarmList();
        super.notifyDataSetChanged();
    }
}
