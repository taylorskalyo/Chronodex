package com.taylorskalyo.chronodex;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
public class EventListAdapter extends BaseAdapter{
	
	private List<ChronoEvent> events;
	private final Context context;
	
	private final int colorSize = 10;
	
	public EventListAdapter(Context context) {
		this.context = context;
		CalendarAdapter calAdapter = new CalendarAdapter(context.getContentResolver());
		events = calAdapter.getEvents();
	}

	@Override
	public int getCount() {
		return events.size();
	}

	@Override
	public Object getItem(int index) {
		return events.get(index);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    View rowView = inflater.inflate(R.layout.eventview, parent, false);
		    TextView labelText = (TextView) rowView.findViewById(R.id.eventtext);
		    ImageView labelColor = (ImageView) rowView.findViewById(R.id.eventcolor);
		    
		    // Set event label text
		    labelText.setText(events.get(position).toString());
		    
		    // Set event label color
		    int[] colors = new int[colorSize*colorSize];
		    for(int i=0; i<colorSize*colorSize; i++) {
		    	colors[i] = events.get(position).getColor();
		    }
		    Bitmap bm = Bitmap.createBitmap(colors, colorSize, colorSize, Bitmap.Config.ARGB_8888);
		    labelColor.setImageBitmap(bm);

		    return rowView;
	}
	
}
