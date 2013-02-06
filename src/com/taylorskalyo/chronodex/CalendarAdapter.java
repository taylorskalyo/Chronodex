package com.taylorskalyo.chronodex;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.text.format.Time;
import android.util.Log;

public class CalendarAdapter {
	public static int day, month, year;
	private static final String[] EVENT_DETAILS = new String[] {
		Events._ID,			// 0
		Events.TITLE,		// 1
		Events.CALENDAR_ID,	// 2
		Events.DTSTART,		// 3
		Events.DTEND,		// 4
		Events.ALL_DAY		// 5
	};
	
	private static final int ID_INDEX = 0;
	private static final int TITLE_INDEX = 1;
	private static final int CAL_ID_INDEX = 2;
	private static final int DTSTART_INDEX = 3;
	private static final int DTEND_INDEX = 4;
	private static final int ALL_DAY_INDEX = 5;
	
	private Cursor cur;
	private ContentResolver cr;
	private String uriBase;
	private String selection;
	private String[] selectionArgs;
	
	public CalendarAdapter(ContentResolver cr) {
		this.cr = cr;
	}
	
	public List<ChronoEvent> getEventsOnDate(int month, int day, int year) {
		
		// Form date start and end in milliseconds
		String dateStart, dateEnd;
		Time t = new Time();
		t.set(0, 0, 0, day, month, year);
		dateStart = Long.toString(t.toMillis(false));
		t.set(59, 59, 23, day, month, year);
		dateEnd = Long.toString(t.toMillis(false));
		
		// Prepare query
		cur = null;
		uriBase = "content://com.android.calendar/";
		selection = "(((" + Events.DTSTART + " >= ?) AND ("
				+ Events.DTEND + " <= ?)) OR (("
				+ Events.DTEND + " > ?) AND ("
				+ Events.DTEND + " <= ?) AND ("
				+ Events.ALL_DAY + " > ?)) OR (("
				+ Events.DTSTART + " <= ?) AND ("
				+ Events.DTEND + " >= ?)))";
		selectionArgs = new String[] {dateStart, dateEnd, dateStart, dateEnd, "0", dateStart, dateEnd};
		
		// Submit the query and get a Cursor object back
		cur = cr.query(Uri.parse(uriBase+"events"), EVENT_DETAILS, selection, selectionArgs, null);
		
		// Use the cursor to store events in a list
		List<ChronoEvent> events = new ArrayList<ChronoEvent>();
		while (cur != null && cur.moveToNext()) {
		    long id = 0;
		    String title = null;
		    String calId = null;
		    int color = 0;
		    int start = 0;
		    int end = 0;
		    int allDay = 0;
		      
		    // Get the field values
		    id = cur.getLong(ID_INDEX);
		    title = cur.getString(TITLE_INDEX);
		    calId = cur.getString(CAL_ID_INDEX);
		    Cursor colorCur = cr.query(Uri.parse(uriBase+"/calendars"), new String[] {Calendars.CALENDAR_COLOR}, 
		    		"(" + Calendars._ID + " = ?)", new String[] {calId}, null);
		    colorCur.moveToNext();
		    color = colorCur.getInt(0);
		    allDay = cur.getInt(ALL_DAY_INDEX);
		    if(allDay > 0) {
		    	start = 0;
		    	end = 24*60;
		    }
		    else {
		    	long duration = (cur.getLong(DTEND_INDEX) - cur.getLong(DTSTART_INDEX))/(1000*60);
		    	start = extractTime(cur.getLong(DTSTART_INDEX));
		    	end = extractTime(cur.getLong(DTEND_INDEX));
		    	if(duration > 24*60) {
		    		start = 0;
		    		end = 24*60;
		    	}
		    }
		              
		    // Create new DayGraphEvent and add it to a list of events
		    events.add(new ChronoEvent(id, title, color, start, end));
		}
		Log.d("debug", events.size()+" events");
		return events;
	}
	
	private int extractTime(Long millis) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(millis);
		date.setTimeZone(TimeZone.getDefault());
		int value = date.get(Calendar.HOUR_OF_DAY)*60 + date.get(Calendar.MINUTE);
		return value;
	}

	public List<ChronoEvent> getEvents() {
		return getEventsOnDate(month, day, year);
	}

}
