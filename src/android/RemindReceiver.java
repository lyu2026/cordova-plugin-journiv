// 提醒广播接收器 - 收到闹钟后显示通知
package com.journiv.plugin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class RemindReceiver extends BroadcastReceiver{
	private static final String CH="journiv_remind"; // 通知渠道ID

	@Override
	public void onReceive(Context ctx,Intent in){
		// 获取提醒信息
		String title=in.getStringExtra("title");
		String msg=in.getStringExtra("msg");
		if(title==null) title="📔 日记时间";
		if(msg==null) msg="该写日记了，记录今天的故事吧～";
		
		// 创建通知渠道（Android 8+）
		NotificationManager nm=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			NotificationChannel ch=new NotificationChannel(CH,"日记提醒",
					NotificationManager.IMPORTANCE_HIGH);
			ch.setDescription("提醒你写日记");
			nm.createNotificationChannel(ch);
		}
		
		// 点击通知打开App的Intent
		Intent launch=ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
		PendingIntent pi=PendingIntent.getActivity(ctx,0,launch,
				PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
		
		// 构建通知
		NotificationCompat.Builder nb=new NotificationCompat.Builder(ctx,CH)
				.setSmallIcon(android.R.drawable.ic_dialog_info)
				.setContentTitle(title)
				.setContentText(msg)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setAutoCancel(true)
				.setContentIntent(pi);
		
		// 显示通知
		nm.notify((int)System.currentTimeMillis(),nb.build());
	}
}