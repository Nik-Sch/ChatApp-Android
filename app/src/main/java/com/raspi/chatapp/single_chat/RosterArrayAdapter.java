package com.raspi.chatapp.single_chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.RosterItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Niklas-Yoga on 11/24/2015.
 */
public class RosterArrayAdapter extends ArrayAdapter<RosterItem>{

    private List<RosterItem> rosterList = new ArrayList<RosterItem>();
    private TextView rosterText;

    public RosterArrayAdapter(Context context, int textViewResourceId){
        super(context, textViewResourceId);
    }

    @Override
    public void add(RosterItem object){
        rosterList.add(object);
        notifyDataSetChanged();
    }

    @Override
    public RosterItem getItem(int position){
        return rosterList.get(position);
    }

    @Override
    public int getCount(){
        return rosterList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View v = convertView;
        if (v == null)
            v = ((LayoutInflater) this.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.roster, parent, false);

        RosterItem rosterObj = getItem(position);
        rosterText = (TextView) v.findViewById(R.id.roster_entry_name);
        rosterText.setText(rosterObj.name);

        return v;
    }
}
