package com.j.plugin;

import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import org.json.*;
import java.util.*;

public class M{
	private Context C;private AlarmManager A;

	M(Context C){this.C=C;this.A=(AlarmManager)C.getSystemService(Context.ALARM_SERVICE);}

	void cfg(JSONObject o){
		SharedPreferences p=C.getSharedPreferences("cfg",Context.MODE_PRIVATE);
		SharedPreferences.Editor e=p.edit();
		if(o.has("rh"))e.putInt("rh",o.optInt("rh",20));
		e.apply();sch(p.getInt("rh",20));
	}

	private void sch(int h){
		Calendar c=Calendar.getInstance(J.TZ);c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);
		if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);
		Intent in=new Intent("com.j.REMIND");in.putExtra("t","日记时间");in.putExtra("m","今天还没写日记哦~");
		PendingIntent pi=PendingIntent.getBroadcast(C,0,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
		A.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
	}

	public static class R extends BroadcastReceiver{
		public void onReceive(Context c,Intent i){
			NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)n.createNotificationChannel(new NotificationChannel("j","提醒",NotificationManager.IMPORTANCE_HIGH));
			PendingIntent p=PendingIntent.getActivity(c,0,c.getPackageManager().getLaunchIntentForPackage(c.getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
			n.notify((int)System.currentTimeMillis(),new NotificationCompat.Builder(c,"j").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(i.getStringExtra("t")).setContentText(i.getStringExtra("m")).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(p).build());
		}
	}
	public static class B extends BroadcastReceiver{
		public void onReceive(Context c,Intent i){if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))new M(c).sch(c.getSharedPreferences("cfg",Context.MODE_PRIVATE).getInt("rh",20));}
	}
}