// 开机启动接收器 - 设备重启后重新注册所有提醒
package com.journiv.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

public class BootReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context ctx,Intent in){
		// 仅在开机完成后处理
		if(!Intent.ACTION_BOOT_COMPLETED.equals(in.getAction())) return;
		
		// 重新调度所有已启用的提醒
		RemindManager rm=new RemindManager(ctx);
		JSONArray list=rm.getAll();
		for(int i=0;i<list.length();i++){
			try{
				JSONObject r=list.getJSONObject(i);
				if(r.optBoolean("on",false)){
					rm.set(r.optString("id"),r.optInt("hour"),r.optInt("min"),null,r.optBoolean("on"),r.optString("type"));
				}
			}catch(Exception e){}
		}
	}
}