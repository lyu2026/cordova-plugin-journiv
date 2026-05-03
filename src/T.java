package com.j.plugin;

import java.text.SimpleDateFormat;
import android.content.Context;
import org.json.*;
import java.util.*;

// 统计管理器 - 分析日记数据生成统计报告
public class T{

	// 日期格式化 - 使用全局东八区时区
	private SimpleDateFormat f;
	T(Context ii){
		f=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
		f.setTimeZone(J.Z);
	}

	// 综合统计 - 返回日记总数、天数、连续天数、图片数、附件数、平均字数、最活跃时段
	JSONObject summary(J.D ii)throws Exception{
		JSONArray a=ii.list(); // 所有记录(不解密)
		int im=0,fi=0,len=0; // 图片数、附件数、总字数
		Set<String> ds=new HashSet<>(); // 有记录的天数集合
		Map<Integer,Integer> hh=new HashMap<>(); // 每小时记录数
		for(int i=0;i<a.length();i++){
			JSONObject r=a.getJSONObject(i);
			try{im+=new JSONArray(r.optString("imgs","[]")).length();}catch(Exception e){}
			try{fi+=new JSONArray(r.optString("files","[]")).length();}catch(Exception e){}
			try{len+=r.optString("content","").length();}catch(Exception e){}
			try{
				long ts=Long.parseLong(r.optString("at","0"));
				ds.add(f.format(new Date(ts)));
				Calendar c=Calendar.getInstance(J.Z);
				c.setTimeInMillis(ts);
				int h=c.get(Calendar.HOUR_OF_DAY);
				hh.put(h,hh.getOrDefault(h,0)+1);
			}catch(Exception e){}
		}
		// 计算连续天数
		Calendar c=Calendar.getInstance(J.Z);
		int s=0;
		while(true){
			if(ds.contains(f.format(c.getTime()))){
				s++;
				c.add(Calendar.DAY_OF_YEAR,-1);
			}else break;
		}
		// 最活跃时段
		int pk=0;
		for(int v:hh.values())if(v>pk)pk=v;
		// 返回结果
		JSONObject o=new JSONObject();
		o.put("count",a.length()); // 记录总条数
		o.put("days",ds.size()); // 有记录的总天数
		o.put("streak",s); // 当前连续天数
		o.put("icount",im); // 图片总数
		o.put("fcount",fi); // 附件总数
		o.put("lavg",a.length()>0?len/a.length():0); // 平均字数
		o.put("peak",pk); // 最活跃时段
		return o;
	}
}