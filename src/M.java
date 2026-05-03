package com.j.plugin;

import androidx.core.app.NotificationCompat;
import android.content.*;
import android.os.Build;
import android.app.*;
import org.json.*;
import java.util.*;

// 提醒管理器 - 每天定时判断并提醒添加日记记录
public class M{
	private Context C; // Android上下文
	private AlarmManager A; // 闹钟管理器
	M(Context _){
		this.C=_;
		this.A=(AlarmManager)C.getSystemService(Context.ALARM_SERVICE);
	}

	// [M.config] 配置提醒 - _={hour:小时(默认20)}
	void config(JSONObject _){
		SharedPreferences p=C.getSharedPreferences("config",Context.MODE_PRIVATE);
		if(_.has("hour"))p.edit().putInt("hour",_.optInt("hour",20)).apply();
		set(p.getInt("hour",20));
	}

	// [M.set] 设置闹钟 - _=小时
	private void set(int _){
		Calendar c=Calendar.getInstance(J.Z); // 使用全局东八区时区
		c.set(Calendar.HOUR_OF_DAY,_);
		c.set(Calendar.MINUTE,0);
		c.set(Calendar.SECOND,0);
		if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);
		Intent i=new Intent("com.j.REMIND");
		i.putExtra("title","日记时间");
		i.putExtra("text","今天还没写日记哦~");
		PendingIntent p=PendingIntent.getBroadcast(C,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
		A.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),p);
	}

	// 提醒广播接收器 - 收到闹钟后显示通知
	public static class R extends BroadcastReceiver{
		public void onReceive(Context c,Intent i){
			NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
				n.createNotificationChannel(new NotificationChannel("journiv","提醒",NotificationManager.IMPORTANCE_HIGH));
			}
			PendingIntent p=PendingIntent.getActivity(c,0,c.getPackageManager().getLaunchIntentForPackage(c.getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
			n.notify((int)System.currentTimeMillis(),new NotificationCompat.Builder(c,"journiv")
				.setSmallIcon(android.R.drawable.ic_dialog_info)
				.setContentTitle(i.getStringExtra("title"))
				.setContentText(i.getStringExtra("text"))
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setAutoCancel(true)
				.setContentIntent(p)
				.build());
		}
	}

	// 开机启动接收器 - 设备重启后重新设置闹钟
	public static class B extends BroadcastReceiver{
		public void onReceive(Context c,Intent i){
			if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){
				new M(c).set(c.getSharedPreferences("config",Context.MODE_PRIVATE).getInt("hour",20));
			}
		}
	}
}