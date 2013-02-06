package com.taylorskalyo.chronodex;

import java.util.Calendar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.support.v4.app.NavUtils;

public class Chronodex extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chronodex);
        
        Calendar cal = Calendar.getInstance();
        CalendarAdapter.month = cal.get(Calendar.MONTH);
		CalendarAdapter.day = cal.get(Calendar.DAY_OF_MONTH);
		CalendarAdapter.year = cal.get(Calendar.YEAR);
		
		EventListAdapter adapter = new EventListAdapter(this);
        ListView list = (ListView) findViewById(R.id.list);
		list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_chronodex, menu);
        return true;
    }
    
}
