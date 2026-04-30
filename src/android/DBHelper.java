// 数据库助手 - 使用Android原生SQLite，内容用AES加密
package com.journiv.plugin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.journiv.plugin.models.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper{
	private static final String DB="journiv.db";
	private static final int VER=1;
	private static final String T="diaries";
	private static final String C_ID="id";
	private static final String C_TITLE="title";
	private static final String C_CONTENT="content";
	private static final String C_MOOD="mood";
	private static final String C_TAGS="tags";
	private static final String C_IMGS="imgs";
	private static final String C_AT="at";

	public DBHelper(Context ctx){
		super(ctx,DB,null,VER);
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL("CREATE TABLE "+T+"("
				+C_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
				+C_TITLE+" TEXT,"
				+C_CONTENT+" TEXT,"
				+C_MOOD+" TEXT,"
				+C_TAGS+" TEXT,"
				+C_IMGS+" TEXT DEFAULT '[]',"
				+C_AT+" TEXT"
				+")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int oldVer,int newVer){
		db.execSQL("DROP TABLE IF EXISTS "+T);
		onCreate(db);
	}

	// 辅助方法：将一行数据转为Entry对象
	private Entry cursorToEntry(Cursor c,boolean decrypt){
		Entry e=new Entry();
		e.id=c.getLong(0);
		e.title=c.getString(1);
		String raw=c.getString(2);
		if(decrypt){
			try{e.content=CryptoUtil.dec(raw);}
			catch(Exception ex){e.content=raw;}
		}else{
			e.content=raw;
		}
		e.mood=c.getString(3);
		e.tags=c.getString(4);
		e.imgs=c.getString(5);
		e.created=c.getString(6);
		e.updated=c.getString(6);
		return e;
	}

	// 插入日记
	public long insert(String title,String content,String mood,String tags){
		SQLiteDatabase db=getWritableDatabase();
		ContentValues v=new ContentValues();
		String ts=String.valueOf(System.currentTimeMillis());
		v.put(C_TITLE,title);
		v.put(C_CONTENT,content);
		v.put(C_MOOD,mood);
		v.put(C_TAGS,tags);
		v.put(C_IMGS,"[]");
		v.put(C_AT,ts);
		long id=db.insert(T,null,v);
		db.close();
		return id;
	}

	// 查询单条日记
	public Entry get(long id){
		SQLiteDatabase db=getReadableDatabase();
		Cursor c=db.query(T,null,C_ID+"=?",new String[]{String.valueOf(id)},null,null,null);
		Entry e=null;
		if(c.moveToFirst()) e=cursorToEntry(c,true);
		c.close();
		db.close();
		return e;
	}

	// 查询所有日记
	public List<Entry> all(){
		List<Entry> list=new ArrayList<>();
		SQLiteDatabase db=getReadableDatabase();
		Cursor c=db.rawQuery("SELECT * FROM "+T+" ORDER BY "+C_AT+" DESC",null);
		if(c.moveToFirst()){
			do{list.add(cursorToEntry(c,true));}while(c.moveToNext());
		}
		c.close();
		db.close();
		return list;
	}

	// 按条件查询
	public List<Entry> query(String where,String[] args,boolean decrypt){
		List<Entry> list=new ArrayList<>();
		SQLiteDatabase db=getReadableDatabase();
		Cursor c=db.query(T,null,where,args,null,null,C_AT+" DESC");
		if(c.moveToFirst()){
			do{list.add(cursorToEntry(c,decrypt));}while(c.moveToNext());
		}
		c.close();
		db.close();
		return list;
	}

	// 更新日记
	public void update(long id,String title,String content,String mood,String tags){
		SQLiteDatabase db=getWritableDatabase();
		ContentValues v=new ContentValues();
		v.put(C_TITLE,title);
		v.put(C_CONTENT,content);
		v.put(C_MOOD,mood);
		v.put(C_TAGS,tags);
		v.put(C_AT,String.valueOf(System.currentTimeMillis()));
		db.update(T,v,C_ID+"=?",new String[]{String.valueOf(id)});
		db.close();
	}

	// 删除日记
	public void remove(long id){
		SQLiteDatabase db=getWritableDatabase();
		db.delete(T,C_ID+"=?",new String[]{String.valueOf(id)});
		db.close();
	}

	// 获取日记数量
	public int count(){
		SQLiteDatabase db=getReadableDatabase();
		Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+T,null);
		int n=0;
		if(c.moveToFirst()) n=c.getInt(0);
		c.close();
		db.close();
		return n;
	}

	// 全文搜索
	public List<Entry> search(String q){
		List<Entry> list=new ArrayList<>();
		SQLiteDatabase db=getReadableDatabase();
		String sql="SELECT * FROM "+T+" WHERE "+C_TITLE+" LIKE ? OR "+C_TAGS+" LIKE ? ORDER BY "+C_AT+" DESC";
		String p="%"+q+"%";
		Cursor c=db.rawQuery(sql,new String[]{p,p});
		if(c.moveToFirst()){
			do{list.add(cursorToEntry(c,true));}while(c.moveToNext());
		}
		c.close();
		db.close();
		return list;
	}

	// 高级组合搜索
	public List<Entry> advSearch(String kw,String start,String end,String mood,String tags){
		List<Entry> list=new ArrayList<>();
		SQLiteDatabase db=getReadableDatabase();
		StringBuilder sql=new StringBuilder("SELECT * FROM "+T+" WHERE 1=1");
		List<String> args=new ArrayList<>();
		if(kw!=null&&!kw.isEmpty()){
			sql.append(" AND ("+C_TITLE+" LIKE ? OR "+C_TAGS+" LIKE ?)");
			String p="%"+kw+"%";
			args.add(p);
			args.add(p);
		}
		if(start!=null&&!start.isEmpty()){
			sql.append(" AND "+C_AT+" >= ?");
			args.add(start);
		}
		if(end!=null&&!end.isEmpty()){
			sql.append(" AND "+C_AT+" <= ?");
			args.add(end);
		}
		if(mood!=null&&!mood.isEmpty()){
			sql.append(" AND "+C_MOOD+" = ?");
			args.add(mood);
		}
		if(tags!=null&&!tags.isEmpty()){
			String[] ts=tags.split(",");
			sql.append(" AND (");
			for(int i=0;i<ts.length;i++){
				if(i>0) sql.append(" OR ");
				sql.append(C_TAGS+" LIKE ?");
				args.add("%"+ts[i].trim()+"%");
			}
			sql.append(")");
		}
		sql.append(" ORDER BY "+C_AT+" DESC");
		Cursor c=db.rawQuery(sql.toString(),args.toArray(new String[0]));
		if(c.moveToFirst()){
			do{list.add(cursorToEntry(c,true));}while(c.moveToNext());
		}
		c.close();
		db.close();
		return list;
	}

	// 添加图片路径
	public void addImg(long diaryId,String path){
		SQLiteDatabase db=getWritableDatabase();
		Cursor c=db.query(T,new String[]{C_IMGS},C_ID+"=?",new String[]{String.valueOf(diaryId)},null,null,null);
		String json="[]";
		if(c.moveToFirst()) json=c.getString(0);
		c.close();
		try{
			JSONArray arr=new JSONArray(json);
			arr.put(path);
			ContentValues v=new ContentValues();
			v.put(C_IMGS,arr.toString());
			v.put(C_AT,String.valueOf(System.currentTimeMillis()));
			db.update(T,v,C_ID+"=?",new String[]{String.valueOf(diaryId)});
		}catch(JSONException e){}
		db.close();
	}
}