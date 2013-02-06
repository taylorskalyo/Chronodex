package com.taylorskalyo.chronodex;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.ListView;

/** DayGraph **
 * Represents a day's worth of calendar events in a circular grid
 */
public class DayGraphView extends View {
	private int size;
	private float centerX;
	private float centerY;
	private float outerRadius;
	private float innerRadius;
	private final int num_of_circles = 6;
	private float radiusStep;
	
	private CalendarAdapter calAdapter;
	private Calendar cal = Calendar.getInstance();
	private List<ChronoEvent> events;
	private int maxEvents;
	private String weekDay;
	private String[] daysOfWeek = new String[] {"SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
	private DatePickerDialog.OnDateSetListener datePickerListener;

	public DayGraphView(Context context) {
		this(context, null);
	}
	public DayGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		calAdapter = new CalendarAdapter(context.getContentResolver());
		datePickerListener = new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int selectedYear, 
					int selectedMonth, int selectedDay) { // called when date picker is closed
				CalendarAdapter.year = selectedYear;
				CalendarAdapter.month = selectedMonth;
				CalendarAdapter.day = selectedDay;
	 
				setEvents();
				invalidate();
				//ListView list = (ListView)findViewById(R.id.list);
				//BaseAdapter BA = (BaseAdapter)list.getAdapter();
				//BA.notifyDataSetChanged();
				
				//Chronodex.adapter.notifyDataSetChanged();
			}
		};
	}
	
	private void setEvents() {
		events = calAdapter.getEvents();
		Collections.sort(events);
		maxEvents = maxEventsPerHour();
		cal.set(CalendarAdapter.year, CalendarAdapter.month, CalendarAdapter.day);
		weekDay = daysOfWeek[cal.get(Calendar.DAY_OF_WEEK)];
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);
		
		int chosenDimension = Math.min(chosenWidth, chosenHeight);
		
		setMeasuredDimension(chosenDimension, chosenDimension);
	}
	
	private int chooseDimension(int mode, int size) {
		final int preferredSize = 300;
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else {
			return preferredSize;
		} 
	}
	
	private void init() {
		setEvents();
		size = getWidth();
		centerX = size/2;
		centerY = size/2;
		outerRadius = size*9/20;
		innerRadius = outerRadius/5;
		radiusStep = (outerRadius-innerRadius)/(float)(num_of_circles-1);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		init();
		drawChronodex(canvas);
		try {
			drawEvents(canvas);
		} catch (Exception e) {
			if(e.getLocalizedMessage() == null) {
				e.printStackTrace();
			}
			else
				Log.d("DayGraph", e.getLocalizedMessage());
		}
		finally {}
		drawGrid(canvas);
		drawTimeGuides(canvas);
	}
	
	private void drawGrid(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setAlpha(64);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);
		
		RectF oval = new RectF();
		
		// Draw 4 concentric circles
		for(int i=1; i<num_of_circles-1; i++) {
			float curRadius = innerRadius + radiusStep*i;
			oval.set(centerX - curRadius, centerY - curRadius,
					centerX + curRadius, centerY + curRadius);
			canvas.drawOval(oval, paint);
		}
		
		
		// Draw 12 dividing lines
		for(int angle=0; angle<360; angle+=30) {
			double radians = Math.toRadians(angle);
			
			float in_bound = innerRadius;
			float out_bound = outerRadius;
			if(angle>180 && angle<270) {in_bound+=radiusStep;}
			if(angle<180 || angle>270) {out_bound-=radiusStep;}
			
			canvas.drawLine(centerX+(int)(in_bound*Math.cos(radians)), 
				centerY+(int)(in_bound*Math.sin(radians)), 
				centerX+(int)(out_bound*Math.cos(radians)), 
				centerY+(int)(out_bound*Math.sin(radians)), paint);
		}
		
		// Draw inner arc
		oval.set(centerX - innerRadius, centerY - innerRadius,
				centerX + innerRadius, centerY + innerRadius);
		canvas.drawArc(oval, 270, 270, false, paint);
	}
	
	private void drawEvents(Canvas canvas) throws Exception {
		boolean[][] circularGrid = new boolean[24][maxEvents];
		
		if(events==null) {throw new Exception("No events");}
		
		for(ChronoEvent event:events) {
			Log.d("debug", event.getName()+" "+event.getDuration()+" "+event.getColor());
			// Find suitable slot
			int slot = 0;
			int startHour = event.getStartTime()/60;
			while(circularGrid[startHour][slot]) {slot++;} // while slot is filled, move to next slot
			
			// Fill slots that the event will need
			int endHour = (event.getEndTime()-1)/60;
			for(int i=startHour; i<=endHour; i++) {
				circularGrid[i][slot] = true;
			}
			
			// Prepare to draw the event
			int morningAngle = 270;
			int dayAngle = 180;
			int nightAngle = 180;
			int morningSweep = 0;
			int daySweep = 0;
			int nightSweep = 0;
			
			int startTime = event.getStartTime();
			int endTime = event.getEndTime();
			if(startTime < 9*60) { // if event starts before 9am
				morningAngle = timeToDegrees(startTime, true);
				if(endTime - (9*60) > 0) { // if event extends past 9am
					morningSweep = timeToDegrees(9*60 - startTime, false);
					if(endTime - (21*60) > 0) { // if event extends past 9pm
						daySweep = 360;
						nightSweep = timeToDegrees(endTime, true) - nightAngle;
					}
					else {daySweep = timeToDegrees(endTime, true) - dayAngle;} // event ends before 9pm
				}
				else {morningSweep = timeToDegrees(endTime, true) - morningAngle;} // event ends before 9am
			}
			else if(startTime < 21*60) { // if event starts between 9am and 9pm
				dayAngle = timeToDegrees(startTime, true);
				if(endTime - (21*60) > 0) { // if event extends past 9pm
					daySweep = timeToDegrees(21*60 - startTime, false);
					nightSweep = timeToDegrees(endTime, true) - nightAngle;
				}
				else {daySweep = (360 + timeToDegrees(endTime, true) - dayAngle)%360;} // event ends before 9pm
			}
			else { // if event starts after 9pm
				nightAngle = timeToDegrees(startTime, true);
				nightSweep = timeToDegrees(endTime, true) - nightAngle;
			}
			
			// Draw the event within the appropriate slots
			drawMorningArc(canvas, morningAngle, morningSweep, slot, event);
			drawDayArc(canvas, dayAngle, daySweep, slot, event);
			drawNightArc(canvas, nightAngle, nightSweep, slot, event);
		}
	}
	
	private int timeToDegrees(int time, boolean adjust) {
		int degrees = 360*time/720;
		if(adjust) {degrees -= 90;}
		return degrees%360;
	}
	
	private void drawMorningArc(Canvas canvas, int startAngle, int sweepAngle, int slot, ChronoEvent event) {
		float strokeWidth = radiusStep/maxEvents;
		
		Paint paint = new Paint();
		paint.setColor(event.getColor());
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(strokeWidth + 1);
		paint.setAntiAlias(true);
		
		RectF oval = new RectF();
		float radius = innerRadius + slot*strokeWidth + strokeWidth/2;
		oval.set(centerX - radius, centerY - radius,
				centerX + radius, centerY + radius);
		
		canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
	}
	private void drawDayArc(Canvas canvas, int startAngle, int sweepAngle, int slot, ChronoEvent event) {
		float strokeWidth = 3*radiusStep/maxEvents;
		
		Paint paint = new Paint();
		paint.setColor(event.getColor());
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(strokeWidth + 1);
		paint.setAntiAlias(true);
		
		RectF oval = new RectF();
		float radius = innerRadius + radiusStep + slot*strokeWidth + strokeWidth/2;
		oval.set(centerX - radius, centerY - radius,
				centerX + radius, centerY + radius);
		
		canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
	}
	private void drawNightArc(Canvas canvas, int startAngle, int sweepAngle, int slot, ChronoEvent event) {
		float strokeWidth = (radiusStep/maxEvents);
		
		Paint paint = new Paint();
		paint.setColor(event.getColor());
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(strokeWidth + 1);
		paint.setAntiAlias(true);
		
		RectF oval = new RectF();
		float radius = outerRadius - radiusStep + slot*strokeWidth + strokeWidth/2;
		oval.set(centerX - radius, centerY - radius,
				centerX + radius, centerY + radius);
		
		canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
	}
	
	private int maxEventsPerHour() {
		int[] hours = new int[24];
		
		// Determine events per hour
		if(events!=null) {
			for(ChronoEvent event:events) {
				int startHour = event.getStartTime()/60;
				int endHour = (event.getEndTime()-1)/60;
				for(int i=startHour; i<=endHour; i++) {
					hours[i] += 1;
				}
			}
		}
		
		// Determine max
		int maxEvents = 3;
		for(int events:hours) {
			if(events > maxEvents) {
				maxEvents = 3*(int) (Math.ceil(events/3.0)); // round up to nearest block of 3
			}
		}
		return maxEvents;
	}
	
	/**
	 * Draws a chronodex graph
	 * Chronodex is a circular graph split into twelve hour segments
	 */
	private void drawChronodex(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);
		
		RectF oval = new RectF();
		float curRadius;
		
		// For each segment, draw the outer arc
		for(int angle=0; angle<360; angle+=30) {
			int r = 2 + angle/30%3; // which circle is being drawn (i.e. the rth circle from the center)
			final int sweepAngle = 30;
			curRadius = innerRadius + r * radiusStep;
			oval.set(centerX - curRadius, centerY - curRadius,
					centerX + curRadius, centerY + curRadius);
			canvas.drawArc(oval, angle, sweepAngle, false, paint);
			
			// Draw lines on both side of each arc
			float inBound, outBound;
			double radians;
			if(r==4) { // if 4th circle from the center has just been drawn
				inBound = innerRadius+radiusStep;
				outBound = curRadius;
				radians = Math.toRadians(angle);
				canvas.drawLine(centerX+(int)(inBound*Math.cos(radians)), 
					centerY+(int)(inBound*Math.sin(radians)), 
					centerX+(int)(outBound*Math.cos(radians)), 
					centerY+(int)(outBound*Math.sin(radians)), paint);
				radians = Math.toRadians(angle + sweepAngle);
				canvas.drawLine(centerX+(int)(inBound*Math.cos(radians)), 
					centerY+(int)(inBound*Math.sin(radians)), 
					centerX+(int)(outBound*Math.cos(radians)), 
					centerY+(int)(outBound*Math.sin(radians)), paint);
			}
			else if(r==3) { // if 3rd circle from the center has just been drawn
				inBound = innerRadius+radiusStep;
				outBound = curRadius;
				radians = Math.toRadians(angle);
				canvas.drawLine(centerX+(int)(inBound*Math.cos(radians)), 
					centerY+(int)(inBound*Math.sin(radians)), 
					centerX+(int)(outBound*Math.cos(radians)), 
					centerY+(int)(outBound*Math.sin(radians)), paint);
			}
		}
		
		// Draw inner circle
		curRadius = innerRadius+radiusStep;
		oval.set(centerX - curRadius, centerY - curRadius,
				centerX + curRadius, centerY + curRadius);
		canvas.drawOval(oval, paint);
		
		// Draw outer dashed arc
		paint.setPathEffect(new DashPathEffect(new float[] {10,10}, 0));
		oval.set(centerX - outerRadius, centerY - outerRadius,
				centerX + outerRadius, centerY + outerRadius);
		canvas.drawArc(oval, 180, 90, false, paint);
	}
	
	private void drawTimeGuides(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);
		
		RectF oval = new RectF();
		float curRadius;
		
		// Draw four circles at the 12, 3, 6, and 9 o'clock positions
		curRadius = innerRadius/5;
		float x, y;
		int[][] set = {{1,0}, {-1,0}, {0,1}, {0,-1}};
		for(int[] pair : set) {
			x = centerX + pair[0]*(outerRadius-radiusStep);
			y = centerY + pair[1]*(outerRadius-radiusStep);
			oval.set(x - curRadius, y - curRadius,
					x + curRadius, y + curRadius);
			canvas.drawOval(oval, paint);
		}
		
		// Draw date
		float textSize = outerRadius/5;
		paint.setStyle(Paint.Style.FILL);
		paint.setTextAlign(Align.CENTER);
		paint.setTextSize(textSize);
		canvas.drawText(CalendarAdapter.day+"", centerX, centerY+textSize/8, paint);
		
		// Draw day of the week
		textSize = outerRadius/10;
		paint.setTextSize(textSize);
		canvas.drawText(weekDay+"", centerX, centerY+outerRadius/8, paint);
		
		
		// Draw day times
		float padding = outerRadius/9;
		textSize = outerRadius/15;
		paint.setTextSize(textSize);
		curRadius = outerRadius - radiusStep + padding;
		for(int h: new int[] {1, 2, 3, 4, 5, 6, 7, 8}) {
			x = (float) (centerX + curRadius*Math.cos(Math.toRadians(timeToDegrees(h*60, true))));
			y = (float) (centerY + curRadius*Math.sin(Math.toRadians(timeToDegrees(h*60, true))));
			canvas.drawText(h+"pm", x, y, paint);
		}
		
		paint.setAlpha(128);
		
		// Draw 12am
		textSize = outerRadius/25;
		paint.setTextSize(textSize);
		curRadius = innerRadius;
		x = (float) (centerX + curRadius*Math.cos(Math.toRadians(timeToDegrees(12*60, true)))) 
				- outerRadius/15;
		y = (float) (centerY + curRadius*Math.sin(Math.toRadians(timeToDegrees(12*60, true))));
		canvas.drawText("12am", x, y, paint);
		
		// Draw 9am
		paint.setTextSize(textSize);
		curRadius = innerRadius;
		x = (float) (centerX + curRadius*Math.cos(Math.toRadians(timeToDegrees(9*60, true)))) 
				- outerRadius/25;
		y = (float) (centerY + curRadius*Math.sin(Math.toRadians(timeToDegrees(9*60, true))))
				- outerRadius/60;
		canvas.drawText("9am", x, y, paint);
		
		// Draw night times
		padding = outerRadius/9;
		textSize = outerRadius/15;
		paint.setTextSize(textSize);
		curRadius = outerRadius + padding;
		for(int h: new int[] {10, 11}) {
			x = (float) (centerX + curRadius*Math.cos(Math.toRadians(timeToDegrees(h*60, true))));
			y = (float) (centerY + curRadius*Math.sin(Math.toRadians(timeToDegrees(h*60, true))));
			canvas.drawText(h+"pm", x, y, paint);
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		float x = event.getX();
		float y = event.getY();
		
		if(action == MotionEvent.ACTION_DOWN) {
			// Check for date clicked
			float dx = centerX - x;
			float dy = centerY - y;
			if(dx*dx + dy*dy < innerRadius*innerRadius) {
				DatePickerDialog datePick = new DatePickerDialog(getContext(), 
						datePickerListener, CalendarAdapter.year, 
						CalendarAdapter.month, CalendarAdapter.day);
				datePick.show();
			}
		}
		
		return false;
	}
	
	@Override
	public Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("instanceState", super.onSaveInstanceState());
		bundle.putInt("month", CalendarAdapter.month);
		bundle.putInt("day", CalendarAdapter.day);
		bundle.putInt("year", CalendarAdapter.year);
		
		return bundle;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			CalendarAdapter.month = bundle.getInt("month");
			CalendarAdapter.day = bundle.getInt("day");
			CalendarAdapter.year = bundle.getInt("year");
			super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
			return;
		}
		
		super.onRestoreInstanceState(state);
	}
	
}

