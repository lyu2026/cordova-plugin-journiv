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
	static final TimeZone TZ=TimeZone.getTimeZone("Asia/Shanghai");
	private D d;private K s;private M m;private T t;private P p;

	public void initialize(CordovaInterface c,CordovaWebView w){
		super.initialize(c,w);Context x=c.getContext();
		d=new D(x);s=new K(x);m=new M(x);t=new T(x);p=new P(x);
	}

	public boolean execute(String a,JSONArray r,CallbackContext b)throws JSONException{
		try{
			switch(a){
				case "sync":cordova.getThreadPool().execute(()->{try{b.success(s.sync(d,r.optBoolean(0,true)));}catch(Exception e){b.error(e.getMessage());}});return true;
				case "save":{JSONObject o=r.getJSONObject(0);b.success(d.save(o,r.optBoolean(1,true),r.optBoolean(2,false)?null:s));break;}
				case "remove":{Object v=r.get(0);int[] ids=v instanceof JSONArray?ja((JSONArray)v):new int[]{r.getInt(0)};d.remove(ids,r.optBoolean(1,false)?s:null);b.success();break;}
				case "page":b.success(d.page(r.optJSONObject(0),r.optInt(1,1),r.optInt(2,20)));break;
				case "one":b.success(d.one(r.getInt(0)));break;
				case "multi":b.success(d.multi(ja(r.getJSONArray(0))));break;
				case "memory":b.success(d.mem());break;
				case "clear":{int v=r.optInt(0,0);if(v==0){d.clear(s);s.clear();}else if(v==1)d.clear(null);else s.clear();b.success();break;}
				case "export":b.success(p.exp(ja2(r.getJSONArray(0)),r.getString(1),d));break;
				case "summary":b.success(t.sum(d));break;
				case "config":m.cfg(r.getJSONObject(0));b.success();break;
				default:b.error("?"+a);return false;
			}
		}catch(Exception e){b.error(e.getMessage());}
		return true;
	}

	private int[] ja(JSONArray a)throws JSONException{int[] r=new int[a.length()];for(int i=0;i<r.length;i++)r[i]=a.getInt(i);return r;}
	private long[] ja2(JSONArray a)throws JSONException{return new long[]{a.getLong(0),a.getLong(1)};}

	static String enc(String x)throws Exception{Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(K.getBytes("UTF-8"),"AES"),new IvParameterSpec(V.getBytes("UTF-8")));return Base64.encodeToString(c.doFinal(x.getBytes("UTF-8")),Base64.NO_WRAP);}
	static String dec(String x)throws Exception{Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(K.getBytes("UTF-8"),"AES"),new IvParameterSpec(V.getBytes("UTF-8")));return new String(c.doFinal(Base64.decode(x,Base64.NO_WRAP)),"UTF-8");}

	static JSONObject ft(String ts){
		JSONObject o=new JSONObject();
		try{
			long t=Long.parseLong(ts);Calendar c=Calendar.getInstance(TZ);c.setTimeInMillis(t);
			int y=c.get(Calendar.YEAR),M=c.get(Calendar.MONTH),d=c.get(Calendar.DATE);
			int h=c.get(Calendar.HOUR_OF_DAY),m=c.get(Calendar.MINUTE),s=c.get(Calendar.SECOND);
			int w=c.get(Calendar.DAY_OF_WEEK)-1;
			String[] W={"日","一","二","三","四","五","六"};
			String[] MN={"元","二","三","四","五","六","七","八","九","十","十一","腊"};
			o.put("d",(d<10?"0":"")+d);
			o.put("m",MN[M]+"月");
			o.put("y",y);
			o.put("w","周"+W[w]);
			o.put("t",(h<10?"0":"")+h+":"+(m<10?"0":"")+m+":"+(s<10?"0":"")+s);
		}catch(Exception e){}
		return o;
	}

	class D extends SQLiteOpenHelper{
		private final Object L=new Object();

		D(Context x){super(x,"Journiv",null,2);}
		public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE o(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,content TEXT,mood TEXT,tags TEXT,imgs TEXT DEFAULT '[]',files TEXT DEFAULT '[]',lat REAL,lng REAL,addr TEXT,at TEXT)");}
		public void onUpgrade(SQLiteDatabase d,int o,int n){d.execSQL("DROP TABLE IF EXISTS o");onCreate(d);}

		JSONObject save(JSONObject o,boolean ins,K s)throws Exception{
			synchronized(L){
				SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();
				long id=o.optLong("id",0);
				JSONArray oi=null,of=null;
				if(id>0){
					Cursor c=db.query("o",new String[]{"imgs","files"},"id=?",new String[]{String.valueOf(id)},null,null,null);
					if(c.moveToFirst()){
						if(s!=null){try{oi=new JSONArray(c.getString(0));}catch(Exception e){}try{of=new JSONArray(c.getString(1));}catch(Exception e){}}
						c.close();fil(v,o);db.update("o",v,"id=?",new String[]{String.valueOf(id)});
					}else{
						c.close();
						if(!ins){db.close();throw new Exception("记录 "+id+" 不存在");}
						fil(v,o);id=db.insert("o",null,v);
					}
				}else{fil(v,o);id=db.insert("o",null,v);}
				db.close();
				if(s!=null){try{o.put("id",id);s.upr(id,o,oi,of);}catch(Exception e){throw new Exception("同步失败: "+e.getMessage());}}
				return one((int)id);
			}
		}

		private void fil(ContentValues v,JSONObject o)throws Exception{
			v.put("title",o.optString("title"));v.put("content",enc(o.optString("content")));
			v.put("mood",o.optString("mood"));v.put("tags",o.optString("tags"));
			Object im=o.opt("imgs");v.put("imgs",im instanceof JSONArray?im.toString():(im instanceof String?(String)im:"[]"));
			Object fi=o.opt("files");v.put("files",fi instanceof JSONArray?fi.toString():(fi instanceof String?(String)fi:"[]"));
			if(o.has("lat"))v.put("lat",o.getDouble("lat"));if(o.has("lng"))v.put("lng",o.getDouble("lng"));
			v.put("addr",o.optString("addr",""));
			v.put("at",String.valueOf(Calendar.getInstance(TZ).getTimeInMillis()));
		}

		void raw(JSONObject o)throws Exception{
			synchronized(L){
				SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();
				long id=o.optLong("id",0);
				v.put("title",o.optString("title"));v.put("content",o.optString("content"));
				v.put("mood",o.optString("mood"));v.put("tags",o.optString("tags"));
				Object im=o.opt("imgs");v.put("imgs",im instanceof JSONArray?im.toString():(im instanceof String?(String)im:"[]"));
				Object fi=o.opt("files");v.put("files",fi instanceof JSONArray?fi.toString():(fi instanceof String?(String)fi:"[]"));
				if(o.has("lat"))v.put("lat",o.getDouble("lat"));if(o.has("lng"))v.put("lng",o.getDouble("lng"));
				v.put("addr",o.optString("addr",""));v.put("at",o.optString("at",String.valueOf(Calendar.getInstance(TZ).getTimeInMillis())));
				if(id>0){db.update("o",v,"id=?",new String[]{String.valueOf(id)});}else{db.insert("o",null,v);}
				db.close();
			}
		}

		void remove(int[] ids,K s)throws Exception{
			synchronized(L){
				SQLiteDatabase db=getWritableDatabase();
				for(int id:ids){if(s!=null){Cursor c=db.query("o",new String[]{"imgs","files"},"id=?",new String[]{String.valueOf(id)},null,null,null);if(c.moveToFirst()){delr(s,c.getString(0));delr(s,c.getString(1));}c.close();s.delr(id);}db.delete("o","id=?",new String[]{String.valueOf(id)});}
				db.close();
			}
		}

		void clear(K s)throws Exception{
			synchronized(L){
				if(s!=null){Cursor c=getReadableDatabase().rawQuery("SELECT imgs,files FROM o",null);while(c.moveToNext()){delr(s,c.getString(0));delr(s,c.getString(1));}c.close();}
				getWritableDatabase().delete("o",null,null);
			}
		}

		private void delr(K s,String j){try{JSONArray a=new JSONArray(j);for(int i=0;i<a.length();i++){String u=a.getString(i);if(u.startsWith("/"))try{s.delf(u);}catch(Exception e){}}}catch(Exception e){}}

		void upl(long id,String imgs,String files){synchronized(L){ContentValues v=new ContentValues();v.put("imgs",imgs);v.put("files",files);getWritableDatabase().update("o",v,"id=?",new String[]{String.valueOf(id)});}}

		JSONArray all(){synchronized(L){JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery("SELECT * FROM o ORDER BY at DESC",null);while(c.moveToNext())a.put(row(c));c.close();return a;}}

		JSONObject row(Cursor c){JSONObject o=new JSONObject();try{o.put("id",c.getLong(0));o.put("title",c.getString(1));try{o.put("content",dec(c.getString(2)));}catch(Exception e){o.put("content",c.getString(2));}o.put("mood",c.getString(3));o.put("tags",c.getString(4));o.put("imgs",new JSONArray(c.getString(5)));o.put("files",new JSONArray(c.getString(6)));o.put("lat",c.getDouble(7));o.put("lng",c.getDouble(8));o.put("addr",c.getString(9));o.put("at",c.getString(10));o.put("ao",ft(c.getString(10)));}catch(Exception e){}return o;}

		JSONObject page(JSONObject q,int pg,int sz)throws Exception{
			synchronized(L){
				StringBuilder w=new StringBuilder(" WHERE 1=1");List<String> p=new ArrayList<>();
				if(q!=null){
					if(q.has("kw")&&!q.optString("kw").isEmpty()){w.append(" AND (title LIKE ? OR content LIKE ? OR tags LIKE ?)");String k="%"+q.optString("kw")+"%";p.add(k);p.add(k);p.add(k);}
					if(q.has("mood")&&!q.optString("mood").isEmpty()){w.append(" AND mood=?");p.add(q.optString("mood"));}
					if(q.has("tags")&&!q.optString("tags").isEmpty()){String[] ts=q.optString("tags").split(",");w.append(" AND (");for(int i=0;i<ts.length;i++){if(i>0)w.append(" OR ");w.append("tags LIKE ?");p.add("%"+ts[i].trim()+"%");}w.append(")");}
					if(q.has("start")){w.append(" AND at >= ?");p.add(q.optString("start"));}
					if(q.has("end")){w.append(" AND at <= ?");p.add(q.optString("end"));}
				}
				int total=ct(w.toString(),p.toArray(new String[0]));
				w.append(" ORDER BY at DESC LIMIT ? OFFSET ?");p.add(String.valueOf(sz));p.add(String.valueOf((pg-1)*sz));
				JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery("SELECT * FROM o"+w.toString(),p.toArray(new String[0]));while(c.moveToNext())a.put(row(c));c.close();
				JSONObject r=new JSONObject();r.put("data",a);r.put("total",total);r.put("page",pg);r.put("size",sz);return r;
			}
		}

		int ct(String w,String[] a){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM o"+w,a);c.moveToFirst();int n=c.getInt(0);c.close();return n;}

		JSONObject one(int id){synchronized(L){Cursor c=getReadableDatabase().query("o",null,"id=?",new String[]{String.valueOf(id)},null,null,null);JSONObject o=c.moveToFirst()?row(c):new JSONObject();c.close();return o;}}

		JSONArray multi(int[] ids){synchronized(L){JSONArray a=new JSONArray();for(int id:ids){JSONObject o=one(id);if(o.has("id"))a.put(o);}return a;}}

		JSONArray mem(){synchronized(L){JSONArray a=new JSONArray();Calendar c=Calendar.getInstance(TZ);int y=c.get(Calendar.YEAR);for(int i=1;i<=10;i++){c.set(Calendar.YEAR,y-i);String d=new SimpleDateFormat("MM-dd",Locale.getDefault()).format(c.getTime());Cursor cu=getReadableDatabase().rawQuery("SELECT * FROM o WHERE at LIKE ? ORDER BY at DESC",new String[]{"%"+d+"%"});JSONArray r=new JSONArray();while(cu.moveToNext())r.put(row(cu));cu.close();if(r.length()>0)a.put(r);}return a;}}
	}
}