package com.j.plugin;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import android.database.Cursor;
import android.database.sqlite.*;
import org.apache.cordova.*;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import android.content.*;
import org.json.*;
import java.util.*;

public class J extends CordovaPlugin{
	private static final String K="abcdefghijklmnopqrstuvwxyz123456",V="abcdefghijklmnop";
	private D d;private S s;private M m;private T t;private P p;private SharedPreferences f;

	public void initialize(CordovaInterface c,CordovaWebView w){
		super.initialize(c,w);Context x=c.getContext();
		d=new D(x);s=new S(x);m=new M(x);t=new T(x);p=new P(x);
		f=x.getSharedPreferences("j",Context.MODE_PRIVATE);
	}

	public boolean execute(String a,JSONArray r,CallbackContext b)throws JSONException{
		try{
			switch(a){
				case "lmap":b.success(s.lmap());break;
				case "lsync":cordova.getThreadPool().execute(()->{try{b.success(s.lsync(r.getLong(0),d));}catch(Exception e){b.error(e.getMessage());}});return true;
				case "save":{JSONObject o=r.getJSONObject(0);long id=d.save(o,r.optBoolean(1,false)?s:null);b.success(d.one((int)id));break;}
				case "remove":{Object v=r.get(0);int[] ids=v instanceof JSONArray?ja((JSONArray)v):new int[]{r.getInt(0)};d.remove(ids,r.optBoolean(1,false)?s:null);b.success();break;}
				case "page":b.success(d.page(r.optJSONObject(0),r.optInt(1,1),r.optInt(2,20)));break;
				case "one":b.success(d.one(r.getInt(0)));break;
				case "multi":b.success(d.multi(ja(r.getJSONArray(0))));break;
				case "memory":b.success(d.memory());break;
				case "sync":cordova.getThreadPool().execute(()->{try{b.success(s.sync(d,r.optBoolean(0,true)));}catch(Exception e){b.error(e.getMessage());}});return true;
				case "clear":{int v=r.optInt(0,0);if(v==0){d.clear(s);s.clear();}else if(v==1)d.clear(null);else s.clear();b.success();break;}
				case "export":b.success(p.export(ja2(r.getJSONArray(0)),r.getString(1),d));break;
				case "summary":b.success(t.summary(d));break;
				case "config":m.config(r.getJSONObject(0));b.success();break;
				default:b.error("?"+a);return false;
			}
		}catch(Exception e){b.error(e.getMessage());}
		return true;
	}

	private int[] ja(JSONArray a)throws JSONException{int[] r=new int[a.length()];for(int i=0;i<r.length;i++)r[i]=a.getInt(i);return r;}
	private long[] ja2(JSONArray a)throws JSONException{return new long[]{a.getLong(0),a.getLong(1)};}

	static String enc(String x)throws Exception{Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(K.getBytes("UTF-8"),"AES"),new IvParameterSpec(V.getBytes("UTF-8")));return Base64.encodeToString(c.doFinal(x.getBytes("UTF-8")),Base64.NO_WRAP);}
	static String dec(String x)throws Exception{Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(K.getBytes("UTF-8"),"AES"),new IvParameterSpec(V.getBytes("UTF-8")));return new String(c.doFinal(Base64.decode(x,Base64.NO_WRAP)),"UTF-8");}

	class D extends SQLiteOpenHelper{
		D(Context x){super(x,"Journiv",null,2);}
		public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE o(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,content TEXT,mood TEXT,tags TEXT,imgs TEXT DEFAULT '[]',files TEXT DEFAULT '[]',lat REAL,lng REAL,addr TEXT,at TEXT)");}
		public void onUpgrade(SQLiteDatabase d,int o,int n){d.execSQL("DROP TABLE IF EXISTS o");onCreate(d);}

		long save(JSONObject o,S s)throws Exception{
			SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();
			long id=o.optLong("id",0);
			JSONArray oldImgs=null,oldFiles=null;
			if(id>0&&s!=null){
				Cursor c=db.query("o",new String[]{"imgs","files"},"id=?",new String[]{String.valueOf(id)},null,null,null);
				if(c.moveToFirst()){try{oldImgs=new JSONArray(c.getString(0));}catch(Exception e){}try{oldFiles=new JSONArray(c.getString(1));}catch(Exception e){}}
				c.close();
			}
			v.put("title",o.optString("title"));v.put("content",enc(o.optString("content")));
			v.put("mood",o.optString("mood"));v.put("tags",o.optString("tags"));
			Object im=o.opt("imgs");v.put("imgs",im instanceof JSONArray?im.toString():(im instanceof String?(String)im:"[]"));
			Object fi=o.opt("files");v.put("files",fi instanceof JSONArray?fi.toString():(fi instanceof String?(String)fi:"[]"));
			if(o.has("lat"))v.put("lat",o.getDouble("lat"));if(o.has("lng"))v.put("lng",o.getDouble("lng"));
			v.put("addr",o.optString("addr",""));v.put("at",String.valueOf(System.currentTimeMillis()));
			if(id>0){db.update("o",v,"id=?",new String[]{String.valueOf(id)});}else{id=db.insert("o",null,v);}
			db.close();if(s!=null){o.put("id",id);s.upRecord(id,o,oldImgs,oldFiles);}return id;
		}

		void remove(int[] ids,S s)throws Exception{
			SQLiteDatabase db=getWritableDatabase();
			for(int id:ids){Cursor c=db.query("o",new String[]{"imgs","files"},"id=?",new String[]{String.valueOf(id)},null,null,null);if(c.moveToFirst()&&s!=null){delRemoteFiles(s,c.getString(0));delRemoteFiles(s,c.getString(1));}c.close();db.delete("o","id=?",new String[]{String.valueOf(id)});if(s!=null)s.delRecord(id);}
			db.close();
		}

		void clear(S s)throws Exception{
			if(s!=null){Cursor c=getReadableDatabase().rawQuery("SELECT imgs,files FROM o",null);while(c.moveToNext()){delRemoteFiles(s,c.getString(0));delRemoteFiles(s,c.getString(1));}c.close();}
			getWritableDatabase().delete("o",null,null);
		}

		private void delRemoteFiles(S s,String json){try{JSONArray a=new JSONArray(json);for(int i=0;i<a.length();i++){String u=a.getString(i);if(u.startsWith("https://"))s.delFile(u);}}catch(Exception e){}}

		void updateLinks(long id,String imgs,String files){ContentValues v=new ContentValues();v.put("imgs",imgs);v.put("files",files);getWritableDatabase().update("o",v,"id=?",new String[]{String.valueOf(id)});}

		JSONArray allRaw(){JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery("SELECT * FROM o ORDER BY at DESC",null);while(c.moveToNext())a.put(row(c));c.close();return a;}

		JSONObject row(Cursor c){JSONObject o=new JSONObject();try{o.put("id",c.getLong(0));o.put("title",c.getString(1));try{o.put("content",dec(c.getString(2)));}catch(Exception e){o.put("content",c.getString(2));}o.put("mood",c.getString(3));o.put("tags",c.getString(4));o.put("imgs",new JSONArray(c.getString(5)));o.put("files",new JSONArray(c.getString(6)));o.put("lat",c.getDouble(7));o.put("lng",c.getDouble(8));o.put("addr",c.getString(9));o.put("at",c.getString(10));}catch(Exception e){}return o;}

		JSONObject page(JSONObject q,int pg,int sz)throws Exception{
			StringBuilder w=new StringBuilder(" WHERE 1=1");List<String> p=new ArrayList<>();
			if(q!=null){
				if(q.has("kw")&&!q.optString("kw").isEmpty()){w.append(" AND (title LIKE ? OR content LIKE ? OR tags LIKE ?)");String k="%"+q.optString("kw")+"%";p.add(k);p.add(k);p.add(k);}
				if(q.has("mood")&&!q.optString("mood").isEmpty()){w.append(" AND mood=?");p.add(q.optString("mood"));}
				if(q.has("tags")&&!q.optString("tags").isEmpty()){String[] ts=q.optString("tags").split(",");w.append(" AND (");for(int i=0;i<ts.length;i++){if(i>0)w.append(" OR ");w.append("tags LIKE ?");p.add("%"+ts[i].trim()+"%");}w.append(")");}
				if(q.has("start")){w.append(" AND at >= ?");p.add(q.optString("start"));}
				if(q.has("end")){w.append(" AND at <= ?");p.add(q.optString("end"));}
			}
			int total=count(w.toString(),p.toArray(new String[0]));
			w.append(" ORDER BY at DESC LIMIT ? OFFSET ?");p.add(String.valueOf(sz));p.add(String.valueOf((pg-1)*sz));
			JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery("SELECT * FROM o"+w.toString(),p.toArray(new String[0]));while(c.moveToNext())a.put(row(c));c.close();
			JSONObject r=new JSONObject();r.put("data",a);r.put("total",total);r.put("page",pg);r.put("size",sz);return r;
		}

		int count(String w,String[] a){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM o"+w,a);c.moveToFirst();int n=c.getInt(0);c.close();return n;}

		JSONObject one(int id){Cursor c=getReadableDatabase().query("o",null,"id=?",new String[]{String.valueOf(id)},null,null,null);JSONObject o=c.moveToFirst()?row(c):new JSONObject();c.close();return o;}

		JSONArray multi(int[] ids){JSONArray a=new JSONArray();for(int id:ids){JSONObject o=one(id);if(o.has("id"))a.put(o);}return a;}

		JSONArray memory(){JSONArray a=new JSONArray();Calendar c=Calendar.getInstance();int y=c.get(Calendar.YEAR);for(int i=1;i<=10;i++){c.set(Calendar.YEAR,y-i);String d=new SimpleDateFormat("MM-dd",Locale.getDefault()).format(c.getTime());Cursor cu=getReadableDatabase().rawQuery("SELECT * FROM o WHERE at LIKE ? ORDER BY at DESC",new String[]{"%"+d+"%"});JSONArray r=new JSONArray();while(cu.moveToNext())r.put(row(cu));cu.close();if(r.length()>0)a.put(r);}return a;}
	}
}