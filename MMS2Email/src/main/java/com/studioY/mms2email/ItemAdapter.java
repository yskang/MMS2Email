package com.studioY.mms2email;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ItemAdapter extends BaseAdapter{
    private Context context;
    private ArrayList<SettingItem> settingItemList = new ArrayList<SettingItem>();
    private AppPreference appPreference;

    public ItemAdapter(Context context) {
        this.context = context;
        appPreference = new AppPreference(context);
        initSettingItemList();
    }

    private void initSettingItemList() {
        settingItemList.clear();

        settingItemList.add(new SettingItem());
        settingItemList.add(new SettingItem());

        settingItemList.get(0).setTitle(context.getString(R.string.item_title_send_mail));
        settingItemList.get(1).setTitle(context.getString(R.string.item_title_receive_mail));

        if(appPreference.getValue(Commons.SENDER_EMAIL_ADDRESS).equals("")){
            settingItemList.get(0).setAddress(context.getString(R.string.set_sender_email_info));
        }else{
            settingItemList.get(0).setAddress(appPreference.getValue(Commons.SENDER_EMAIL_ADDRESS));
        }

        if(appPreference.getValue(Commons.RECEIVER_EMAIL_ADDRESS).equals("")){
            settingItemList.get(1).setAddress(context.getString(R.string.set_receiver_email_info));
        }else {
            settingItemList.get(1).setAddress(appPreference.getValue(Commons.RECEIVER_EMAIL_ADDRESS));
        }
    }

    public void updateList() {
        initSettingItemList();
        notifyDataSetChanged();
    }

    public class ItemListViewHolder{
        public TextView titleText;
        public TextView addressText;
    }

    @Override
    public int getCount() {
        return settingItemList.size();
    }

    @Override
    public SettingItem getItem(int position) {
        return settingItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater vi = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = vi.inflate(R.layout.item, null);

            final ItemListViewHolder itemListViewHolder = new ItemListViewHolder();
            itemListViewHolder.titleText = (TextView) view.findViewById(R.id.itemTitle);
            itemListViewHolder.addressText = (TextView) view.findViewById(R.id.itemContents);

            itemListViewHolder.titleText.setText(getItem(position).getTitle());
            itemListViewHolder.addressText.setText(getItem(position).getAddress());

            view.setTag(itemListViewHolder);
        }

        ItemListViewHolder itemListViewHolder = (ItemListViewHolder) view.getTag();

        itemListViewHolder.addressText = (TextView) view.findViewById(R.id.itemContents);
        itemListViewHolder.addressText.setText(getItem(position).getAddress());

        return view;
    }


}
