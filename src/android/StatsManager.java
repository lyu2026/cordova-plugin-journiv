// 统计管理器 - 分析日记数据生成统计报告
package com.journiv.plugin;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import com.journiv.plugin.models.Entry;
import java.text.SimpleDateFormat;
import java.util.*;

public class StatsManager{
	private DBHelper db;
	private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());

	public StatsManager(Context ctx){
		this.db=new DBHelper(ctx);
	}

	// 综合统计
	public JSONObject stats(String start,String end) throws Exception{
		JSONObject s=new JSONObject();

		// 查询时间范围内的日记
		String where="at >= ? AND at <= ?";
		List<Entry> list=db.query(where,new String[]{start,end},false);

		// 统计总数
		s.put("total",list.size());
		s.put("days",getDays(list)); // 有写日记的天数

		// 统计情绪分布
		s.put("mood",moodCount(list));

		// 统计标签
		s.put("tags",tagCount(list));

		// 平均字数
		s.put("avgLen",avgLength(list));

		return s;
	}

	// 情绪时间线
	public JSONArray moodTimeline(int days){
		JSONArray arr=new JSONArray();
		try{
			Calendar today=Calendar.getInstance();
			for(int i=days-1;i>=0;i--){
				Calendar cal=(Calendar)today.clone();
				cal.add(Calendar.DAY_OF_YEAR,-i);
				String date=sdf.format(cal.getTime());

				String where="at LIKE ?";
				List<Entry> list=db.query(where,new String[]{date+"%"},true);

				JSONObject d=new JSONObject();
				d.put("date",date);
				if(list.isEmpty()){
					d.put("mood","none");
					d.put("count",0);
				}else{
					d.put("mood",list.get(0).mood); // 当天第一条的情绪
					d.put("count",list.size());
				}
				arr.put(d);
			}
		}catch(Exception e){}
		return arr;
	}

	// 连续写作天数
	public JSONObject streak(){
		JSONObject s=new JSONObject();
		try{
			List<Entry> all=db.all();
			Set<String> days=new HashSet<>();
			for(Entry e:all){
				long ts=Long.parseLong(e.created);
				days.add(sdf.format(new Date(ts)));
			}

			// 从今天往回数连续天数
			Calendar cal=Calendar.getInstance();
			int streak=0;
			while(true){
				String date=sdf.format(cal.getTime());
				if(days.contains(date)){
					streak++;
					cal.add(Calendar.DAY_OF_YEAR,-1);
				}else{
					break;
				}
			}
			s.put("streak",streak);
			s.put("total",all.size());
		}catch(Exception e){
			s.put("streak",0);
		}
		return s;
	}

	// 统计情绪分布
	private JSONObject moodCount(List<Entry> list){
		JSONObject m=new JSONObject();
		try{
			Map<String,Integer> map=new HashMap<>();
			for(Entry e:list){
				String mood=e.mood!=null?e.mood:"其他";
				map.put(mood,map.getOrDefault(mood,0)+1);
			}
			for(Map.Entry<String,Integer> kv:map.entrySet()){
				m.put(kv.getKey(),kv.getValue());
			}
		}catch(Exception ex){}
		return m;
	}

	// 统计标签
	private JSONObject tagCount(List<Entry> list){
		JSONObject t=new JSONObject();
		try{
			Map<String,Integer> map=new HashMap<>();
			for(Entry e:list){
				if(e.tags==null) continue;
				for(String tag:e.tags.split(",")){
					tag=tag.trim();
					if(!tag.isEmpty()) map.put(tag,map.getOrDefault(tag,0)+1);
				}
			}
			for(Map.Entry<String,Integer> kv:map.entrySet()){
				t.put(kv.getKey(),kv.getValue());
			}
		}catch(Exception ex){}
		return t;
	}

	// 平均内容长度
	private int avgLength(List<Entry> list){
		if(list.isEmpty()) return 0;
		int total=0;
		for(Entry e:list){
			try{
				String dec=CryptoUtil.dec(e.content);
				total+=dec.length();
			}catch(Exception ex){}
		}
		return total/list.size();
	}

	// 有写日记的天数
	private int getDays(List<Entry> list){
		Set<String> days=new HashSet<>();
		for(Entry e:list){
			try{
				long ts=Long.parseLong(e.created);
				days.add(sdf.format(new Date(ts)));
			}catch(Exception ex){}
		}
		return days.size();
	}
}