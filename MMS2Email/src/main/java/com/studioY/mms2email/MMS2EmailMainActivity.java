package com.studioY.mms2email;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;

public class MMS2EmailMainActivity extends Activity implements AdapterView.OnItemClickListener{
    private ListView settingList;
    private ItemAdapter adapter;
    private Dialog senderEmailSettingDialog;
    private Dialog receiverEmailSettingDialog;
    private RelativeLayout senderEmailSettingView;
    private RelativeLayout receiverEmailSettingView;
    private AppPreference appPreference;
    private Switch mmsMonitorSwitch;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Commons.COM_STUDIO_Y_MMS2EMAIL_SERVICE_RUNS_OK)){
                mmsMonitorSwitch.setChecked(true);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Commons.COM_STUDIO_Y_MMS2EMAIL_SERVICE_RUNS_OK);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appPreference = new AppPreference(this);

        senderEmailSettingView = makeView(R.layout.sender_email_setting);
        receiverEmailSettingView = makeView(R.layout.receiver_email_setting);
        senderEmailSettingDialog = makeSenderEmailSettingDialog();
        receiverEmailSettingDialog = makeReceiverEmailSettingDialog();

        adapter = new ItemAdapter(this);

        settingList = (ListView) findViewById(R.id.settingListView);
        settingList.setAdapter(adapter);
        settingList.setOnItemClickListener(this);

        mmsMonitorSwitch = (Switch) findViewById(R.id.serviceStatus);
        mmsMonitorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Intent intent = new Intent(Commons.COM_STUDIO_Y_MMS2EMAIL_START_MMSMONITOR);
                    startService(intent);
                }else{
                    Intent intent = new Intent(Commons.COM_STUDIO_Y_MMS2EMAIL_START_MMSMONITOR);
                    stopService(intent);
                }
            }
        });

        sendMMSMonitorCheckIntent();
    }

    private void sendMMSMonitorCheckIntent() {
        Intent intent = new Intent(Commons.COM_STUDIO_Y_MMS2EMAIL_CHECK_MMSMONITOR);
        sendBroadcast(intent);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(position == 0){
            senderEmailSettingDialog.show();
        }else if(position == 1){
            receiverEmailSettingDialog.show();
        }
    }

    private RelativeLayout makeView(int layoutID){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return (RelativeLayout) inflater.inflate(layoutID, null);
    }

    private Dialog makeSenderEmailSettingDialog(){
        Builder builder = new AlertDialog.Builder(this);

        final EditText senderAddress = (EditText)senderEmailSettingView.findViewById(R.id.senderEmail);
        final EditText senderPassword = (EditText)senderEmailSettingView.findViewById(R.id.senderEmailPassword);

        if(!appPreference.getValue(Commons.SENDER_EMAIL_ADDRESS).equals("")){
            senderAddress.setText(appPreference.getValue(Commons.SENDER_EMAIL_ADDRESS));
        }

        if(!appPreference.getValue(Commons.SENDER_EMAIL_PASSWORD).equals("")){
            senderPassword.setText(appPreference.getValue(Commons.SENDER_EMAIL_PASSWORD));
        }

        builder.setPositiveButton(R.string.positive,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("yskang", "senderAddress : " + senderAddress);
                        appPreference.saveValue(Commons.SENDER_EMAIL_ADDRESS, senderAddress.getText().toString());
                        appPreference.saveValue(Commons.SENDER_EMAIL_PASSWORD, senderPassword.getText().toString());
                        adapter.updateList();
                    }
                });

        builder.setNegativeButton(R.string.negative,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        builder.setView(senderEmailSettingView);
        builder.setTitle(R.string.senderDialogTitle);

        return builder.create();
    }

    private Dialog makeReceiverEmailSettingDialog(){
        Builder builder = new AlertDialog.Builder(this);

        final EditText receiverAddress = (EditText) receiverEmailSettingView.findViewById(R.id.receiverEmail);

        if(!appPreference.getValue(Commons.RECEIVER_EMAIL_ADDRESS).equals("")){
            receiverAddress.setText(appPreference.getValue(Commons.RECEIVER_EMAIL_ADDRESS));
        }

        builder.setPositiveButton(R.string.positive,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        appPreference.saveValue(Commons.RECEIVER_EMAIL_ADDRESS, receiverAddress.getText().toString());
                        adapter.notifyDataSetChanged();
                    }
                });

        builder.setNegativeButton(R.string.negative,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        builder.setView(receiverEmailSettingView);
        builder.setTitle(R.string.receiverDialogTitle);

        return builder.create();
    }
}