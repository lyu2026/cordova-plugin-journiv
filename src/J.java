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
	// 时区: 东八区
	static final TimeZone Z=TimeZone.getTimeZone("Asia/Shanghai");
	// AES加密密钥(32字节)和初始化向量(16字节)
	private static final String K="abcdefghijklmnopqrstuvwxyz123456",V="abcdefghijklmnop";
	// 各功能模块实例
	private D _d; // 数据库
	private S _s; // 客户端
	private X _x; // 导出
	private R _r; // 提醒
	private T _t; // 统计

	// [J.initialize] 初始化
	public void initialize(CordovaInterface _,CordovaWebView w){
		super.initialize(_,w);
		Context c=_.getContext();
		_d=new D(c);_s=new S(c);_x=new X(c);_r=new R(c);_t=new T(c);
	}

	// [J.execute] 执行JS端发来的操作 _=操作名称 x=参数 c=回调
	public boolean execute(String _,JSONArray x,CallbackContext c)throws JSONException{
		try{switch(_){
			// 配置提醒 - 设置每天提醒时间
			case "config":
				_r.config(x.getJSONObject(0));
				c.success();
				break;
			// 同步 - true=本地覆盖线上 false=线上覆盖本地
			case "sync":
				cordova.getThreadPool().execute(()->{
					try{c.success(_s.sync(_d,x.optBoolean(0,true)));}
					catch(Exception e){c.error(e.getMessage());}
				});
				return true;
			// 保存记录(增/改) - 参数:数据对象,不存在时是否插入,是否同步
			case "save":{
				JSONObject o=x.getJSONObject(0);
				c.success(_d.save(o,x.optBoolean(1,true),true,x.optBoolean(2,false)?null:_s,x.optBoolean(2,false)));
				break;
			}
			// 删除记录 - 参数:id或id数组,是否同步删除线上
			case "remove":{
				Object v=x.get(0);
				int[] s=v instanceof JSONArray?iaay((JSONArray)v):new int[]{x.getInt(0)};
				_d.remove(s,x.optBoolean(1,false)?_s:null);
				c.success();
				break;
			}
			// 分页查询 - 参数:查询条件,页码,每页条数
			case "page":
				c.success(_d.page(x.optJSONObject(0),x.optInt(1,1),x.optInt(2,20)));
				break;
			// 单条查询 - 参数:记录id
			case "one":
				c.success(_d.one(x.optInt(0)));
				break;
			// 那年今日 - 获取以往年份今日的记录
			case "memory":
				c.success(_d.memory());
				break;
			// 清空 - 0=本地+线上 1=只本地 2=只线上
			case "clear":{
				int v=x.optInt(0,0);
				if(v==0){_d.clear(_s);_s.clear();}
				else if(v==1)_d.clear(null);
				else _s.clear();
				c.success();
				break;
			}
			// 导出 - 参数:[开始时间戳,结束时间戳],格式(json/pdf)
			case "export":
				c.success(_x.export(itwo(x.getJSONArray(0)),x.getString(1),_d));
				break;
			// 统计 - 获取日记统计数据
			case "summary":
				c.success(_t.summary(_d));
				break;
			default:
				c.error("未知操作: "+_);
				return false;
		}}catch(Exception e){c.error(e.getMessage());}
		return true;
	}

	// [J.iaay] JSONArray转int数组
	private int[] iaay(JSONArray _)throws JSONException{
		int[] o=new int[_.length()];
		for(int i=0;i<_.length();i++)o[i]=_.getInt(i);
		return o;
	}
	// [J.itwo] JSONArray转long数组(两个元素:开始和结束时间戳)
	private long[] itwo(JSONArray _)throws JSONException{
		return new long[]{_.getLong(0),_.getLong(1)};
	}

	// [J.encode] AES加密 - 明文转Base64密文
	static String encode(String _)throws Exception{
		Cipher o=Cipher.getInstance("AES/CBC/PKCS5Padding");
		o.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(K.getBytes("UTF-8"),"AES"),new IvParameterSpec(V.getBytes("UTF-8")));
		return Base64.encodeToString(o.doFinal(_.getBytes("UTF-8")),Base64.NO_WRAP);
	}
	// [J.decode] AES解密 - Base64密文转明文
	static String decode(String _)throws Exception{
		Cipher o=Cipher.getInstance("AES/CBC/PKCS5Padding");
		o.init(Cipher.DECRYPT_MODE,new SecretKeySpec(K.getBytes("UTF-8"),"AES"),new IvParameterSpec(V.getBytes("UTF-8")));
		return new String(o.doFinal(Base64.decode(_,Base64.NO_WRAP)),"UTF-8");
	}



	// 数据库操作内部类 - SQLite增删改查
	class D extends SQLiteOpenHelper{
		private final Object L=new Object(); // 同步锁
		D(Context _){super(_,"journiv",null,2);}
		public void onCreate(SQLiteDatabase _){
			_.execSQL("CREATE TABLE O(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,content TEXT,mood TEXT,tags TEXT,imgs TEXT DEFAULT '[]',files TEXT DEFAULT '[]',lat REAL,lng REAL,addr TEXT,at TEXT)");
		}
		public void onUpgrade(SQLiteDatabase _,int o,int n){
			_.execSQL("DROP TABLE IF EXISTS O");
			onCreate(_);
		}

		// [D.fill] 填充记录 - e=true加密content字段
		private ContentValues fill(JSONObject _,boolean e)throws Exception{
			ContentValues o=new ContentValues();
			o.put("title",_.optString("title"));
			o.put("content",e?encode(_.optString("content")):_.optString("content"));
			o.put("mood",_.optString("mood"));
			o.put("tags",_.optString("tags"));
			Object si=_.opt("imgs"),sf=_.opt("files");
			o.put("imgs",si instanceof JSONArray?si.toString():(si instanceof String?(String)si:"[]"));
			o.put("files",sf instanceof JSONArray?sf.toString():(sf instanceof String?(String)sf:"[]"));
			if(_.has("lat"))o.put("lat",_.getDouble("lat"));
			if(_.has("lng"))o.put("lng",_.getDouble("lng"));
			if(_.has("at"))o.put("at",_.optString("at"));
			return o;
		}

		// [D.save] 保存记录 - i=不存在时插入 e=加密 _s=网络客户端 s=同步
		JSONObject save(JSONObject _,boolean i,boolean e,S _s,boolean s)throws Exception{
			synchronized(L){
				SQLiteDatabase z=getWritableDatabase();
				ContentValues x=fill(_,e);
				long id=_.optLong("id",0);
				JSONArray si=null,sf=null;
				if(id>0){
					x.put("id",id);
					if(!x.containsKey("at"))x.put("at",String.valueOf(System.currentTimeMillis()));
					Cursor c=z.query("O",new String[]{"imgs","files"},"id=?",new String[]{String.valueOf(id)},null,null,null);
					if(c.moveToFirst()){
						if(s&&_s!=null){
							try{si=new JSONArray(c.getString(0));}catch(Exception ex){}
							try{sf=new JSONArray(c.getString(1));}catch(Exception ex){}
						}
						c.close();
						z.update("O",x,"id=?",new String[]{String.valueOf(id)});
					}else{
						c.close();
						if(!i){z.close();throw new Exception("记录 "+id+" 不存在");}
						id=z.insert("O",null,x);
						z.close();
					}
				}else{
					id=z.insert("O",null,x);
					z.close();
				}
				if(s&&_s!=null){
					try{_.put("id",id);_s.log(id,_,si,sf);}
					catch(Exception e){throw new Exception("同步失败: "+e.getMessage());}
				}
				return one((int)id);
			}
		}

		// [D.remove] 删除记录 - _=要删除的id数组 _s=网络客户端(非null时同步删除线上)
		void remove(int[] _,S _s)throws Exception{
			synchronized(L){
				SQLiteDatabase z=getWritableDatabase();
				JSONArray ns=new JSONArray();
				for(int id:_){
					if(_s!=null){
						Cursor c=z.query("O",new String[]{"imgs","files"},"id=?",new String[]{String.valueOf(id)},null,null,null);
						if(c.moveToFirst()){
							JSONArray s=new JSONArray(c.getString(0));
							for(int i=0;i<s.length();i++){
								String n=s.getString(i);
								if(n.startsWith("/tyan/files/"))ns.put(n.replaceFirst("/tyan/",""));
							}
							s=new JSONArray(c.getString(1));
							for(int i=0;i<s.length();i++){
								String n=s.getString(i);
								if(n.startsWith("/tyan/files/"))ns.put(n.replaceFirst("/tyan/",""));
							}
						}
						ns.put(id+".json");
						c.close();
					}
					z.delete("O","id=?",new String[]{String.valueOf(id)});
				}
				z.close();
				if(_s!=null)_s.remove(ns);
			}
		}

		// [D.clear] 清空表 - _=网络客户端(非null时删除远程文件)
		void clear(S _)throws Exception{
			synchronized(L){
				getWritableDatabase().delete("O",null,null);
				if(_!=null)_.clear();
			}
		}

		// [D.srm] 删除远程文件 - x=JSON数组字符串
		private void srm(S _,String x){
			try{
				JSONArray o=new JSONArray(x);
				for(int i=0;i<o.length();i++){
					String n=o.getString(i);
					if(n.startsWith("/tyan/files/"))_.srm(n);
				}
			}catch(Exception e){}
		}

		// [D.list] 查询所有记录(不解密内容)
		JSONArray list(){
			synchronized(L){
				JSONArray o=new JSONArray();
				Cursor c=getReadableDatabase().rawQuery("SELECT * FROM O ORDER BY at DESC",null);
				while(c.moveToNext())o.put(rbuild(c));
				c.close();
				return o;
			}
		}

		// [D.rbuild] 行数据转JSON对象 - 解密content字段
		JSONObject rbuild(Cursor _){
			JSONObject o=new JSONObject();
			try{
				o.put("id",_.getLong(0));
				o.put("title",_.getString(1));
				try{o.put("content",decode(_.getString(2)));}catch(Exception e){o.put("content",_.getString(2));}
				o.put("mood",_.getString(3));
				o.put("tags",_.getString(4));
				o.put("imgs",new JSONArray(_.getString(5)));
				o.put("files",new JSONArray(_.getString(6)));
				o.put("lat",_.getDouble(7));
				o.put("lng",_.getDouble(8));
				o.put("addr",_.getString(9));
				o.put("at",_.getString(10));
			}catch(Exception e){}
			return o;
		}

		// [D.page] 分页查询 - _=查询条件 p=页码 s=每页条数
		JSONObject page(JSONObject _,int p,int s)throws Exception{
			synchronized(L){
				List<String> v=new ArrayList<>();
				StringBuilder w=new StringBuilder(" WHERE 1=1");
				if(_!=null){
					if(_.has("kw")&&!_.optString("kw").isEmpty()){
						w.append(" AND (title LIKE ? OR tags LIKE ?)");
						String k="%"+_.optString("kw")+"%";
						v.add(k);
						v.add(k);
					}
					if(_.has("mood")&&!_.optString("mood").isEmpty()){
						w.append(" AND mood=?");
						v.add(_.optString("mood"));
					}
					if(_.has("tags")&&!_.optString("tags").isEmpty()){
						String[] ts=_.optString("tags").split(",");
						w.append(" AND (");
						for(int i=0;i<ts.length();i++){
							if(i>0)w.append(" OR ");
							w.append("tags LIKE ?");
							v.add("%"+ts[i].trim()+"%");
						}
						w.append(")");
					}
					if(_.has("start")){w.append(" AND at>=?");v.add(_.optString("start"));}
					if(_.has("end")){w.append(" AND at<=?");v.add(_.optString("end"));}
				}
				int t=count(w.toString(),v.toArray(new String[0]));
				w.append(" ORDER BY at DESC LIMIT ? OFFSET ?");
				v.add(String.valueOf(s));
				v.add(String.valueOf((p-1)*s));
				JSONArray x=new JSONArray();
				Cursor c=getReadableDatabase().rawQuery("SELECT * FROM O"+w.toString(),v.toArray(new String[0]));
				while(c.moveToNext())x.put(rbuild(c));
				c.close();
				JSONObject o=new JSONObject();
				o.put("list",x);o.put("total",t);o.put("page",p);o.put("size",s);
				return o;
			}
		}

		// [D.count] 计数 - 返回符合条件的记录总数
		int count(String _,String[] s){
			Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM O"+_,s);
			c.moveToFirst();
			int o=c.getInt(0);
			c.close();
			return o;
		}

	// [D.trange] 按时间范围查询 - s=开始时间戳 e=结束时间戳
	JSONArray trange(long s,long e){
		synchronized(L){
			JSONArray o=new JSONArray();
			Cursor c=getReadableDatabase().rawQuery("SELECT * FROM O WHERE at>=? AND at<=? ORDER BY at DESC",new String[]{String.valueOf(s),String.valueOf(e)});
			while(c.moveToNext())o.put(rbuild(c));
			c.close();
			return o;
		}
	}

		// [D.one] 单条查询 - 根据id返回记录
		JSONObject one(int _){
			synchronized(L){
				Cursor c=getReadableDatabase().query("O",null,"id=?",new String[]{String.valueOf(_)},null,null,null);
				JSONObject o=c.moveToFirst()?rbuild(c):new JSONObject();
				c.close();
				return o;
			}
		}

		// [D.memory] 那年今日 - 获取最近10年同月同日的记录
		JSONArray memory(){
			synchronized(L){
				JSONArray o=new JSONArray();
				Calendar z=Calendar.getInstance(Z);
				int y=z.get(Calendar.YEAR);
				String x=String.format("%02d",(z.get(Calendar.MONTH)+1))+"-"+String.format("%02d",z.get(Calendar.DATE));
				for(int i=1;i<=10;i++){
					Cursor c=getReadableDatabase().rawQuery("SELECT * FROM O WHERE strftime('%y-%m-%d',at/1000,'unixepoch')=? ORDER BY at DESC",new String[]{(y%100)+"-"+x});
					JSONArray _=new JSONArray();
					while(c.moveToNext())_.put(rbuild(c));
					c.close();
					if(_.length()>0)o.put(_);
					y=y-1;
				}
				return o;
			}
		}
	}
}