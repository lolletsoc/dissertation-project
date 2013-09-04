
package com.fyp.resilience.activity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.fyp.resilience.Flags;
import com.fyp.resilience.PreferenceConstants;
import com.fyp.resilience.R;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.ServerRegistrationChanged;
import com.fyp.resilience.fragment.ClientsFragment;
import com.fyp.resilience.fragment.ConnectionsFragment;
import com.fyp.resilience.fragment.FilesFragment;
import com.fyp.resilience.service.PieceUploadService;
import com.fyp.resilience.util.Utils;

import de.greenrobot.event.EventBus;

public class ResilienceActivity extends Activity implements TabListener {

    static final String TAG = ResilienceActivity.class.getSimpleName();

    static final int TAB_FILES = 0;
    static final int TAB_CONNECTIONS = 1;
    static final int TAB_CLIENTS = 2;

    static final int REQUEST_CODE = 200;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.resilience_activity_layout);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(actionBar
                .newTab()
                .setText(R.string.tab_item_files)
                .setTag(TAB_FILES)
                .setTabListener(this), true);

        actionBar.addTab(actionBar
                .newTab()
                .setText(R.string.tab_item_connections)
                .setTag(TAB_CONNECTIONS)
                .setTabListener(this));

        actionBar.addTab(actionBar
                .newTab()
                .setText(R.string.tab_item_clients)
                .setTag(TAB_CLIENTS)
                .setTabListener(this));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this, ServerRegistrationChanged.class);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        
        final MenuItem recordItem = menu.findItem(R.id.start_camera_menu_item);
        if (null != recordItem) {
            recordItem.setEnabled(true);

        }
        
        final MenuItem addItem = menu.findItem(R.id.add_file_menu_item);
        if (null != addItem) {
            addItem.setEnabled(true);
        }

        if (Utils.getDeviceInfo(this).getServerRegistrationId().equals("")) {
            /*
             * If the device doesn't have a Server registration ID then disable
             * options
             */

            if (null != recordItem) {
                recordItem.setEnabled(false);

            }

            if (null != addItem) {
                addItem.setEnabled(false);
            }
        } else if (!Utils.hasCamera(this)) {

            /*
             * If this device doesn't have a camera then no point displaying the
             * record item
             */

            if (null != recordItem) {
                recordItem.setEnabled(false);
                recordItem.setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.resil_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.start_camera_menu_item:
                final Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(videoIntent, REQUEST_CODE);
                return true;

            case R.id.add_file_menu_item:
                final Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("*/*")
                        .addCategory(Intent.CATEGORY_OPENABLE);

                final Intent chooser = Intent.createChooser(fileIntent, getString(R.string.file_chooser_text));
                startActivityForResult(chooser, REQUEST_CODE);
                return true;

            case R.id.start_licence_menu_item:
                final Intent licenceIntent = new Intent(this, LicenceActivity.class);
                startActivity(licenceIntent);
                return true;

            case R.id.start_settings_menu_item:
                final Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(final Tab tab, final FragmentTransaction ft) {
        final int tag = (Integer) tab.getTag();
        replaceWithFragment(tag, ft);
    }

    /**
     * Replaces the main {@link Fragment} with the one specified by the selected
     * tab.
     * 
     * @param ft - integer tag assigned against the tab.
     * @param tag - {@link FragmentTransaction}.
     */
    private void replaceWithFragment(final int tag, final FragmentTransaction ft) {
        final Fragment fragment;
        switch (tag) {
            case TAB_FILES:
                fragment = new FilesFragment();
                break;

            case TAB_CONNECTIONS:
                fragment = new ConnectionsFragment();
                break;

            case TAB_CLIENTS:
            default:
                // TODO
                fragment = new ClientsFragment();
                break;
        }

        ft.replace(R.id.frag_main, fragment);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (null != data) {
            switch (requestCode) {
                case REQUEST_CODE:
                    if (null != data.getData()) {
                        /*
                         * Creates a file based on the path return from the
                         * responding app
                         */
                        final long pieceSize = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this)
                                .getString(PreferenceConstants.PIECE_SIZE_KEY, "4194304"));

                        final DataWhole dataWhole = DataWhole.getOwnedDataWhole(this, data.getData(), pieceSize);
                        ResilienceController.getInstance(this).addDataWhole(dataWhole);

                        if (Flags.DEBUG) {
                            Log.d(TAG, "Starting service!");
                        }
                        startService(new Intent(this, PieceUploadService.class));
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
    
    public void onEventMainThread(ServerRegistrationChanged event) {
        invalidateOptionsMenu();
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

}
