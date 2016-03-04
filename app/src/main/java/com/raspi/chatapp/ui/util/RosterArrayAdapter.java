/*
 * Copyright 2016 Niklas Schelten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.raspi.chatapp.ui.util;

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
    ((TextView) v.findViewById(R.id.roster_entry_name)).setText(rosterObj.getName());

    return v;
  }
}
