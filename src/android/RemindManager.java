// 提醒管理器 - 设置定时通知提醒用户写日记
package com.journiv.plugin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class RemindManager{
	private Context ctx;
	private AlarmManager am;

	// 写作提示语库
	private static final String[] TIPS={
		"今天最让你感恩的三件事是什么？",
		"如果可以和十年前的自己对话，你会说什么？",
		"今天有什么值得记录的小确幸？",
		"写下今天最深刻的一个想法",
		"今天完成了什么目标？感觉如何？",
		"今天克服了什么困难？",
		"什么人让你今天微笑了？",
		"睡前记录三个今天的闪光时刻"
	};

	public RemindManager(Context ctx){
		this.ctx=ctx;
		this.am=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
	}

	// 设置提醒
	public JSONObject set(String id,int hour,int min,boolean[] days,boolean on,String type){
		try{
			JSONObject r=new JSONObject();
			r.put("id",id==null||id.isEmpty()?UUID.randomUUID().toString():id);
			r.put("hour",hour);
			r.put("min",min);
			r.put("days",new JSONArray(days));
			r.put("on",on);
			r.put("type",type); // daily/weekly/prompt
			r.put("at",System.currentTimeMillis());
			save(r); // 保存配置
			if(on) schedule(r); // 启用时设置闹钟
			return r;
		}catch(Exception e){
			return null;
		}
	}

	// 调度闹钟
	private void schedule(JSONObject r){
		String id=r.optString("id");
		int hour=r.optInt("hour",20);
		int min=r.optInt("min",0);
		boolean[] days=getDays(r.optJSONArray("days"));
		
		// 计算下次触发时间
		Calendar cal=Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY,hour);
		cal.set(Calendar.MINUTE,min);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		if(cal.getTimeInMillis()<=System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR,1);
		// 找到匹配的星期
		int day=cal.get(Calendar.DAY_OF_WEEK)-1; // 0=周日
		if(day<0) day=0;
		if(day>=days.length) day=0;
		while(!days[day]){
			cal.add(Calendar.DAY_OF_YEAR,1);
			day=cal.get(Calendar.DAY_OF_WEEK)-1;
			if(day<0||day>=days.length) day=0;
		}
		
		// 创建Intent
		Intent in=new Intent(ctx,RemindReceiver.class);
		in.setAction("com.journiv.ACTION_REMIND");
		in.putExtra("id",id);
		in.putExtra("title","📔 日记时间");
		in.putExtra("msg",TIPS[new Random().nextInt(TIPS.length)]);
		
		int req=id.hashCode();
		PendingIntent pi=PendingIntent.getBroadcast(ctx,req,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
		
		// 设置精确闹钟
		am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
	}

	// 取消提醒
	public void cancel(String id){
		Intent in=new Intent(ctx,RemindReceiver.class);
		in.setAction("com.journiv.ACTION_REMIND");
		PendingIntent pi=PendingIntent.getBroadcast(ctx,id.hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
		am.cancel(pi);
		// 从配置中移除
		try{
			JSONArray all=getAll();
			JSONArray keep=new JSONArray();
			for(int i=0;i<all.length();i++){
				JSONObject r=all.getJSONObject(i);
				if(!r.optString("id").equals(id)) keep.put(r);
			}
			saveList(keep);
		}catch(Exception e){}
	}

	// 获取所有提醒
	public JSONArray getAll(){
		SharedPreferences p=ctx.getSharedPreferences("remind",Context.MODE_PRIVATE);
		String s=p.getString("list","[]");
		try{return new JSONArray(s);}catch(Exception e){return new JSONArray();}
	}

	// 保存单个提醒
	private void save(JSONObject r){
		try{
			JSONArray all=getAll();
			// 更新或新增
			String id=r.optString("id");
			JSONArray keep=new JSONArray();
			boolean found=false;
			for(int i=0;i<all.length();i++){
				JSONObject old=all.getJSONObject(i);
				if(old.optString("id").equals(id)){
					keep.put(r);
					found=true;
				}else{
					keep.put(old);
				}
			}
			if(!found) keep.put(r);
			saveList(keep);
		}catch(Exception e){}
	}

	// 保存提醒列表
	private void saveList(JSONArray list){
		ctx.getSharedPreferences("remind",Context.MODE_PRIVATE).edit().putString("list",list.toString()).apply();
	}

	// 从JSON解析星期数组
	private boolean[] getDays(JSONArray arr){
		boolean[] days=new boolean[7]; // 周日到周六
		if(arr==null||arr.length()==0){
			// 默认每天
			Arrays.fill(days,true);
			return days;
		}
		for(int i=0;i<Math.min(arr.length(),7);i++){
			days[i]=arr.optBoolean(i,true);
		}
		return days;
	}
}