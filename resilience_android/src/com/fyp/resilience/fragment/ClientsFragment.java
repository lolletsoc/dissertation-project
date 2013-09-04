
package com.fyp.resilience.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.widget.BaseAdapter;

import com.fyp.resilience.adapter.ClientListAdapter;
import com.fyp.resilience.event.ClientListChanged;

import de.greenrobot.event.EventBus;

public class ClientsFragment extends ListFragment {

    private BaseAdapter mListAdapter;

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        EventBus.getDefault().register(this);

        mListAdapter = new ClientListAdapter(getActivity());
        setListAdapter(mListAdapter);
    }

    public void onEventMainThread(final ClientListChanged event) {
        notifyChange();
    }

    private void notifyChange() {
        mListAdapter.notifyDataSetChanged();
    }

}
