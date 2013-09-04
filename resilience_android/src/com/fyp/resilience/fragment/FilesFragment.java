
package com.fyp.resilience.fragment;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.fyp.resilience.Flags;
import com.fyp.resilience.R;
import com.fyp.resilience.ResilienceApplication;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.adapter.FileListAdapter;
import com.fyp.resilience.connection.Connectable;
import com.fyp.resilience.connection.ServerDownloadConnectable;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.WholeModified;
import com.fyp.resilience.util.Utils;
import com.fyp.resilience.view.FileView;

import de.greenrobot.event.EventBus;

public class FilesFragment extends ListFragment implements OnItemClickListener {

    private static final String TAG = FilesFragment.class.getSimpleName();

    private BaseAdapter mListAdapter;

    boolean mDualPane;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        EventBus.getDefault().register(this);

        getListView().setOnItemClickListener(this);

        mListAdapter = new FileListAdapter(getActivity());
        setListAdapter(mListAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(final WholeModified event) {
        if (Flags.DEBUG) {
            Log.i(TAG, event.getClass().getSimpleName() + " event has been called");
        }
        notifyChange();
    }

    private void notifyChange() {
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final FileView fileView = (FileView) view;

        if (null != fileView) {
            final DataWhole dataWhole = fileView.getDataWhole();
            if (null != dataWhole) {

                /* Check if the Whole already has a URI */
                if (null != dataWhole.getUriString()) {
                    final Uri fileUri = Uri.parse(dataWhole.getUriString());
                    final Intent fileIntent = new Intent(Intent.ACTION_VIEW);

                    final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
                    final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

                    fileIntent.setDataAndType(fileUri, mimeType);
                    if (Utils.isUriAvailable(getActivity(), fileIntent)) {
                        startActivity(fileIntent);
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.run_file_no_applications), Toast.LENGTH_SHORT)
                                .show();
                    }

                } else if (dataWhole.getState() == DataWhole.STATE_DOWNLOADING) {
                    Toast.makeText(getActivity(), getString(R.string.download_in_progress), Toast.LENGTH_SHORT);

                } else {

                    /* Ask the user if they wish to attempt a download */
                    final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setMessage(getString(R.string.download_alert_question))
                            .setCancelable(true);

                    alertBuilder.setPositiveButton(R.string.download_alert_yes_button,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Connectable serverDownload = new ServerDownloadConnectable(getActivity(), dataWhole);

                                    ResilienceController.getInstance(getActivity()).addConnection(serverDownload);

                                    ResilienceApplication.getApplication(getActivity())
                                            .getServerUploadThreadExecutorService().submit(serverDownload);
                                }
                            });

                    alertBuilder.setNegativeButton(R.string.download_alert_no_button, null);

                    alertBuilder.create().show();
                }
            }
        }
    }
}
