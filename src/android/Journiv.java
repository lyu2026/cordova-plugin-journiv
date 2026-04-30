// Cordova插件主入口 - 处理所有JS调用
package com.journiv.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import com.journiv.plugin.models.Entry;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class Journiv extends CordovaPlugin{
	private static final String TAG="Journiv";
	private DBHelper db;
	private ImgManager img;
	private SyncManager sync;
	private RemindManager rm;
	private StatsManager stat;
	private PdfExport pdf;
	private SharedPreferences prefs;
	private static final String PREF="journiv_pref";
	private static final String KEY_HASH="pwd_hash";

	private static final String[] PROMPTS={
		"今天最让你感恩的三件事是什么？",
		"如果可以和十年前的自己对话，你会说什么？",
		"描述一个让你感到平静的地方。",
		"今天学到了什么新东西？",
		"写一封信给未来的自己。"
	};

	@Override
	public void initialize(CordovaInterface cordova,CordovaWebView webView){
		super.initialize(cordova,webView);
		Context ctx=cordova.getContext();
		db=new DBHelper(ctx);
		img=new ImgManager(ctx);
		sync=new SyncManager(ctx);
		rm=new RemindManager(ctx);
		stat=new StatsManager(ctx);
		pdf=new PdfExport(ctx);
		prefs=ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE);
	}

	@Override
	public boolean execute(String action,JSONArray args,CallbackContext cb) throws JSONException{
		try{
			switch(action){
				case "save":       return save(args,cb);
				case "get":        return get(args,cb);
				case "all":        return all(cb);
				case "update":     return update(args,cb);
				case "remove":     return remove(args,cb);
				case "memory":     return memory(cb);
				case "byMood":     return byMood(args,cb);
				case "prompt":     return prompt(cb);
				case "setPass":    return setPass(args,cb);
				case "checkPass":  return checkPass(args,cb);
				case "addImg":     return addImg(args,cb);
				case "getImgs":    return getImgs(args,cb);
				case "search":     return search(args,cb);
				case "advSearch":  return advSearch(args,cb);
				case "syncUp":     return syncUp(cb);
				case "syncDown":   return syncDown(cb);
				case "syncStatus": return syncStatus(cb);
				case "setRemind":    return setRemind(args,cb);
				case "cancelRemind": return cancelRemind(args,cb);
				case "allReminds":   return allReminds(cb);
				case "stats":     return stats(args,cb);
				case "moodChart": return moodChart(args,cb);
				case "streak":    return streak(cb);
				case "exportPdf": return exportPdf(args,cb);
				default:
					cb.error("未知操作: "+action);
					return false;
			}
		}catch(Exception e){
			cb.error("错误: "+e.getMessage());
			return false;
		}
	}

	private boolean save(JSONArray args,CallbackContext cb){
		try{
			String title=args.getString(0);
			String content=args.getString(1);
			String mood=args.getString(2);
			String tags=args.getString(3);
			String enc=CryptoUtil.enc(content);
			long id=db.insert(title,enc,mood,tags);
			JSONObject r=new JSONObject();
			r.put("id",id);
			r.put("ok",true);
			cb.success(r);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean get(JSONArray args,CallbackContext cb){
		try{
			long id=args.getLong(0);
			Entry e=db.get(id);
			if(e==null){cb.error("日记不存在");return true;}
			cb.success(toJson(e));
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean all(CallbackContext cb){
		try{
			List<Entry> list=db.all();
			JSONArray arr=new JSONArray();
			for(Entry e:list) arr.put(toJson(e));
			cb.success(arr);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean update(JSONArray args,CallbackContext cb){
		try{
			long id=args.getLong(0);
			String title=args.getString(1);
			String content=args.getString(2);
			String mood=args.getString(3);
			String tags=args.getString(4);
			String enc=CryptoUtil.enc(content);
			db.update(id,title,enc,mood,tags);
			cb.success("更新成功");
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean remove(JSONArray args,CallbackContext cb){
		try{
			long id=args.getLong(0);
			img.removeByDiary(id);
			db.remove(id);
			cb.success("删除成功");
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean memory(CallbackContext cb){
		try{
			Calendar cal=Calendar.getInstance();
			cal.add(Calendar.YEAR,-1);
			String date=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(cal.getTime());
			String where="at LIKE ?";
			List<Entry> list=db.query(where,new String[]{date+"%"},true);
			JSONArray arr=new JSONArray();
			for(Entry e:list) arr.put(toJson(e));
			JSONObject r=new JSONObject();
			r.put("date",date);
			r.put("memories",arr);
			cb.success(r);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean byMood(JSONArray args,CallbackContext cb){
		try{
			String mood=args.getString(0);
			String where="mood=?";
			List<Entry> list=db.query(where,new String[]{mood},true);
			JSONArray arr=new JSONArray();
			for(Entry e:list) arr.put(toJson(e));
			cb.success(arr);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean prompt(CallbackContext cb){
		String p=PROMPTS[new Random().nextInt(PROMPTS.length)];
		cb.success(p);
		return true;
	}

	private boolean setPass(JSONArray args,CallbackContext cb){
		try{
			String pwd=args.getString(0);
			String hash=hashSha256(pwd);
			prefs.edit().putString(KEY_HASH,hash).apply();
			cb.success("密码设置成功");
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean checkPass(JSONArray args,CallbackContext cb){
		try{
			String pwd=args.getString(0);
			String hash=hashSha256(pwd);
			String stored=prefs.getString(KEY_HASH,"");
			JSONObject r=new JSONObject();
			r.put("ok",hash.equals(stored));
			cb.success(r);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean addImg(JSONArray args,CallbackContext cb){
		try{
			long diaryId=args.getLong(0);
			String b64=args.getString(1);
			if(b64.contains(",")) b64=b64.substring(b64.indexOf(",")+1);
			String path=img.save(diaryId,b64);
			db.addImg(diaryId,path);
			JSONObject r=new JSONObject();
			r.put("ok",true);
			r.put("path",path);
			cb.success(r);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean getImgs(JSONArray args,CallbackContext cb){
		try{
			long diaryId=args.getLong(0);
			String[] paths=img.getByDiary(diaryId);
			JSONArray arr=new JSONArray();
			for(String p:paths){
				try{arr.put(img.toBase64(p));}catch(Exception e){}
			}
			JSONObject r=new JSONObject();
			r.put("id",diaryId);
			r.put("imgs",arr);
			cb.success(r);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean search(JSONArray args,CallbackContext cb){
		try{
			String q=args.getString(0);
			List<Entry> list=db.search(q);
			JSONArray arr=new JSONArray();
			for(Entry e:list) arr.put(toJson(e));
			cb.success(arr);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean advSearch(JSONArray args,CallbackContext cb){
		try{
			String kw=args.optString(0,null);
			String start=args.optString(1,null);
			String end=args.optString(2,null);
			String mood=args.optString(3,null);
			String tags=args.optString(4,null);
			List<Entry> list=db.advSearch(kw,start,end,mood,tags);
			JSONArray arr=new JSONArray();
			for(Entry e:list) arr.put(toJson(e));
			cb.success(arr);
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean syncUp(CallbackContext cb){
		cordova.getThreadPool().execute(()->{
			try{
				List<Entry> list=db.all();
				JSONArray arr=new JSONArray();
				for(Entry e:list) arr.put(toJson(e));
				JSONObject r=sync.up(arr);
				cb.success(r);
			}catch(Exception e){cb.error(e.getMessage());}
		});
		return true;
	}

	private boolean syncDown(CallbackContext cb){
		cordova.getThreadPool().execute(()->{
			try{
				JSONObject r=sync.down();
				cb.success(r);
			}catch(Exception e){cb.error(e.getMessage());}
		});
		return true;
	}

	private boolean syncStatus(CallbackContext cb){
		cb.success(sync.status());
		return true;
	}

	private boolean setRemind(JSONArray args,CallbackContext cb){
		try{
			String id=args.optString(0,null);
			int hour=args.getInt(1);
			int min=args.getInt(2);
			JSONArray daysArr=args.optJSONArray(3);
			boolean[] days=new boolean[7];
			if(daysArr!=null){
				for(int i=0;i<Math.min(daysArr.length(),7);i++) days[i]=daysArr.optBoolean(i,true);
			}else{
				Arrays.fill(days,true);
			}
			boolean on=args.optBoolean(4,true);
			String type=args.optString(5,"daily");
			JSONObject r=rm.set(id,hour,min,days,on,type);
			cb.success(r!=null?r:new JSONObject().put("ok",false));
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean cancelRemind(JSONArray args,CallbackContext cb){
		try{
			String id=args.getString(0);
			rm.cancel(id);
			cb.success("已取消");
		}catch(Exception e){cb.error(e.getMessage());}
		return true;
	}

	private boolean allReminds(CallbackContext cb){
		cb.success(rm.getAll());
		return true;
	}

	private boolean stats(JSONArray args,CallbackContext cb){
		cordova.getThreadPool().execute(()->{
			try{
				String start=args.getString(0);
				String end=args.getString(1);
				JSONObject r=stat.stats(start,end);
				cb.success(r);
			}catch(Exception e){cb.error(e.getMessage());}
		});
		return true;
	}

	private boolean moodChart(JSONArray args,CallbackContext cb){
		cordova.getThreadPool().execute(()->{
			try{
				int days=args.getInt(0);
				JSONArray r=stat.moodTimeline(days);
				cb.success(r);
			}catch(Exception e){cb.error(e.getMessage());}
		});
		return true;
	}

	private boolean streak(CallbackContext cb){
		cb.success(stat.streak());
		return true;
	}

	private boolean exportPdf(JSONArray args,CallbackContext cb){
		cordova.getThreadPool().execute(()->{
			try{
				String path=args.optString(0,null);
				String start=args.getString(1);
				String end=args.getString(2);
				String result=pdf.export(path,start,end);
				JSONObject r=new JSONObject();
				r.put("ok",true);
				r.put("path",result);
				cb.success(r);
			}catch(Exception e){cb.error(e.getMessage());}
		});
		return true;
	}

	private JSONObject toJson(Entry e) throws Exception{
		JSONObject o=new JSONObject();
		o.put("id",e.id);
		o.put("title",e.title);
		o.put("content",e.content);
		o.put("mood",e.mood);
		o.put("tags",e.tags);
		o.put("imgs",e.imgs);
		o.put("created",e.created);
		o.put("updated",e.updated);
		return o;
	}

	private String hashSha256(String input) throws Exception{
		MessageDigest md=MessageDigest.getInstance("SHA-256");
		byte[] hash=md.digest(input.getBytes("UTF-8"));
		return Base64.encodeToString(hash,Base64.NO_WRAP);
	}
}