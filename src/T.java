package com.j.plugin;

import android.content.Context;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class T{
	private SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
	{f.setTimeZone(J.TZ);}

	T(Context c){}

	JSONObject sum(J.D d)throws Exception{
		JSONObject o=new JSONObject();JSONArray a=d.all();
		int im=0,fi=0,len=0;Set<String> ds=new HashSet<>();Map<Integer,Integer> hh=new HashMap<>();
		for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);
			try{im+=new JSONArray(r.optString("imgs","[]")).length();}catch(Exception e){}
			try{fi+=new JSONArray(r.optString("files","[]")).length();}catch(Exception e){}
			try{len+=r.optString("content","").length();}catch(Exception e){}
			try{long ts=Long.parseLong(r.optString("at","0"));ds.add(f.format(new Date(ts)));Calendar c=Calendar.getInstance(J.TZ);c.setTimeInMillis(ts);int h=c.get(Calendar.HOUR_OF_DAY);hh.put(h,hh.getOrDefault(h,0)+1);}catch(Exception e){}
		}
		Calendar c=Calendar.getInstance(J.TZ);int s=0;while(true){if(ds.contains(f.format(c.getTime()))){s++;c.add(Calendar.DAY_OF_YEAR,-1);}else break;}
		int pk=0;for(int v:hh.values())if(v>pk)pk=v;
		o.put("days",ds.size());o.put("count",a.length());o.put("icount",im);o.put("fcount",fi);o.put("streak",s);o.put("peak",pk);o.put("lavg",a.length()>0?len/a.length():0);
		return o;
	}
}