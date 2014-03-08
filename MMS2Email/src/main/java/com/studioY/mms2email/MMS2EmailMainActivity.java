package com.studioY.mms2email;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;

public class MMS2EmailMainActivity extends Activity{
    private ListView settingList;
    private ItemAdapter adapter;
    private String[] strings = new String[] {"aaa", "bbb"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ItemAdapter(this);

        settingList = (ListView) findViewById(R.id.settingListView);
        settingList.setAdapter(adapter);

        MMSMonitor mmsMonitor = new MMSMonitor(this);
        mmsMonitor.startMMSMonitoring();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}