package com.j.plugin;

import android.content.Context;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class T{
	private SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());

	T(Context c){}

	JSONObject summary(J.D d)throws Exception{
		JSONObject o=new JSONObject();JSONArray a=d.allRaw();
		int imgs=0,files=0,len=0;Set<String> ds=new HashSet<>();Map<Integer,Integer> hh=new HashMap<>();
		for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);
			try{imgs+=new JSONArray(r.optString("imgs","[]")).length();}catch(Exception e){}
			try{files+=new JSONArray(r.optString("files","[]")).length();}catch(Exception e){}
			try{len+=r.optString("content","").length();}catch(Exception e){}
			try{long ts=Long.parseLong(r.optString("at","0"));ds.add(f.format(new Date(ts)));Calendar c=Calendar.getInstance();c.setTimeInMillis(ts);int h=c.get(Calendar.HOUR_OF_DAY);hh.put(h,hh.getOrDefault(h,0)+1);}catch(Exception e){}
		}
		Calendar c=Calendar.getInstance();int s=0;while(true){if(ds.contains(f.format(c.getTime()))){s++;c.add(Calendar.DAY_OF_YEAR,-1);}else break;}
		o.put("totalCount",a.length());o.put("totalDays",ds.size());o.put("streak",s);
		o.put("imgCount",imgs);o.put("fileCount",files);
		o.put("avgLen",a.length()>0?len/a.length():0);
		int peak=0;for(int v:hh.values())if(v>peak)peak=v;o.put("peakHour",peak);
		return o;
	}
}