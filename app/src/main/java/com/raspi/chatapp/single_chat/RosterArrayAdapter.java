package com.raspi.chatapp.single_chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.raspi.chatapp.R;

import org.jivesoftware.smack.roster.RosterEntry;

import java.util.ArrayList;
import java.util.List;

public class RosterArrayAdapter extends ArrayAdapter<RosterEntry>{

    private List<RosterEntry> rosterList = new ArrayList<RosterEntry>();
    private TextView rosterText;

    public RosterArrayAdapter(Context context, int textViewResourceId){
        super(context, textViewResourceId);
    }

    @Override
    public void add(RosterEntry object){
        rosterList.add(object);
        notifyDataSetChanged();
    }

    @Override
    public RosterEntry getItem(int position){
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

        RosterEntry rosterObj = getItem(position);
        rosterText = (TextView) v.findViewById(R.id.roster_entry_name);
        rosterText.setText(rosterObj.getName());

        return v;
    }
}
