// 加密数据库助手 - 管理日记数据的SQLite存储，使用SQLCipher加密
package com.journiv.plugin;

import android.content.ContentValues;
import android.content.Context;
import com.journiv.plugin.models.Entry;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import net.sqlcipher.Cursor;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper{
	// 数据库常量
	private static final String DB="journiv.db"; // 数据库文件名
	private static final int VER=1; // 数据库版本
	private static final String PWD="JournivDbPassword2026!!"; // 数据库密码

	// 表名
	private static final String T="diaries"; // 日记主表
	private static final String F="diaries_fts"; // 全文搜索虚拟表

	// 列名
	private static final String C_ID="id";
	private static final String C_TITLE="title";
	private static final String C_CONTENT="content";
	private static final String C_MOOD="mood";
	private static final String C_TAGS="tags";
	private static final String C_IMGS="imgs";
	private static final String C_AT="at"; // 创建/更新时间

	public DBHelper(Context ctx){
		super(ctx,DB,null,VER);
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		// 创建日记表
		String sql="CREATE TABLE "+T+"("
				+C_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
				+C_TITLE+" TEXT,"
				+C_CONTENT+" TEXT,"
				+C_MOOD+" TEXT,"
				+C_TAGS+" TEXT,"
				+C_IMGS+" TEXT DEFAULT '[]',"
				+C_AT+" TEXT"
				+")";
		db.execSQL(sql);

		// 创建全文搜索虚拟表 - 支持中文分词
		String fts="CREATE VIRTUAL TABLE IF NOT EXISTS "+F+" USING fts4(title,content,tags,tokenize=unicode61)";
		db.execSQL(fts);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int oldVer,int newVer){
		db.execSQL("DROP TABLE IF EXISTS "+T);
		db.execSQL("DROP TABLE IF EXISTS "+F);
		onCreate(db);
	}

	// 辅助方法：将一行数据转为Entry对象
	private Entry cursorToEntry(Cursor c,boolean decrypt){
		Entry e=new Entry();
		e.id=c.getLong(0);
		e.title=c.getString(1);
		String raw=c.getString(2);
		// 可选解密内容
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
		e.updated=c.getString(6); // 时间戳共用at列
		return e;
	}

	// 插入日记
	public long insert(String title,String content,String mood,String tags){
		SQLiteDatabase db=getWritableDatabase(PWD);
		String ts=String.valueOf(System.currentTimeMillis()); // 时间戳

		// 插入主表
		ContentValues v=new ContentValues();
		v.put(C_TITLE,title);
		v.put(C_CONTENT,content); // 已加密内容
		v.put(C_MOOD,mood);
		v.put(C_TAGS,tags);
		v.put(C_IMGS,"[]");
		v.put(C_AT,ts);
		long id=db.insert(T,null,v);

		// 同步插入FTS表 - 用于全文搜索
		ContentValues fv=new ContentValues();
		fv.put("docid",id);
		fv.put("title",title);
		// FTS存储明文以便搜索
		try{fv.put("content",CryptoUtil.dec(content));}
		catch(Exception e){fv.put("content",content);}
		fv.put("tags",tags);
		db.insert(F,null,fv);
		db.close();
		return id;
	}

	// 查询单条日记
	public Entry get(long id){
		SQLiteDatabase db=getReadableDatabase(PWD);
		Cursor c=db.query(T,null,C_ID+"=?",new String[]{String.valueOf(id)},null,null,null);
		Entry e=null;
		if(c.moveToFirst()) e=cursorToEntry(c,true);
		c.close();
		db.close();
		return e;
	}

	// 查询所有日记 - 按更新时间倒序
	public List<Entry> all(){
		List<Entry> list=new ArrayList<>();
		SQLiteDatabase db=getReadableDatabase(PWD);
		Cursor c=db.rawQuery("SELECT * FROM "+T+" ORDER BY "+C_AT+" DESC",null);
		if(c.moveToFirst()){
			do{list.add(cursorToEntry(c,true));}while(c.moveToNext());
		}
		c.close();
		db.close();
		return list;
	}

	// 按条件查询 - 用于统计和筛选
	public List<Entry> query(String where,String[] args,boolean decrypt){
		List<Entry> list=new ArrayList<>();
		SQLiteDatabase db=getReadableDatabase(PWD);
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
		SQLiteDatabase db=getWritableDatabase(PWD);
		String ts=String.valueOf(System.currentTimeMillis());

		// 更新主表
		ContentValues v=new ContentValues();
		v.put(C_TITLE,title);
		v.put(C_CONTENT,content);
		v.put(C_MOOD,mood);
		v.put(C_TAGS,tags);
		v.put(C_AT,ts);
		db.update(T,v,C_ID+"=?",new String[]{String.valueOf(id)});

		// 更新FTS索引
		ContentValues fv=new ContentValues();
		fv.put("title",title);
		try{fv.put("content",CryptoUtil.dec(content));}
		catch(Exception e){fv.put("content",content);}
		fv.put("tags",tags);
		db.update(F,fv,"docid=?",new String[]{String.valueOf(id)});
		db.close();
	}

	// 删除日记
	public void remove(long id){
		SQLiteDatabase db=getWritableDatabase(PWD);
		db.delete(T,C_ID+"=?",new String[]{String.valueOf(id)});
		db.delete(F,"docid=?",new String[]{String.valueOf(id)});
		db.close();
	}

	// 获取日记数量
	public int count(){
		SQLiteDatabase db=getReadableDatabase(PWD);
		Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+T,null);
		int n=0;
		if(c.moveToFirst()) n=c.getInt(0);
		c.close();
		db.close();
		return n;
	}

	// 全文搜索 - 支持中英文
	public List<Entry> search(String q){
		List<Entry> list=new ArrayList<>();
		SQLiteDatabase db=getReadableDatabase(PWD);
		// 构建FTS4查询
		String[] words=q.trim().split("\\s+");
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<words.length;i++){
			if(i>0) sb.append(" AND ");
			sb.append(words[i]).append("*"); // 前缀匹配
		}
		String sql="SELECT d.* FROM "+T+" d INNER JOIN "+F+" f "+"ON d."+C_ID+"=f.docid WHERE "+F+" MATCH ? ORDER BY rank";
		Cursor c=db.rawQuery(sql,new String[]{sb.toString()});
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
		SQLiteDatabase db=getReadableDatabase(PWD);

		StringBuilder sql=new StringBuilder("SELECT * FROM "+T+" WHERE 1=1");
		List<String> args=new ArrayList<>();

		// 关键词模糊匹配标题和标签
		if(kw!=null&&!kw.isEmpty()){
			sql.append(" AND ("+C_TITLE+" LIKE ? OR "+C_TAGS+" LIKE ?)");
			String p="%"+kw+"%";
			args.add(p);
			args.add(p);
		}
		// 日期范围筛选
		if(start!=null&&!start.isEmpty()){
			sql.append(" AND "+C_AT+" >= ?");
			args.add(start);
		}
		if(end!=null&&!end.isEmpty()){
			sql.append(" AND "+C_AT+" <= ?");
			args.add(end);
		}
		// 情绪筛选
		if(mood!=null&&!mood.isEmpty()){
			sql.append(" AND "+C_MOOD+" = ?");
			args.add(mood);
		}
		// 标签筛选 - 支持多个标签OR匹配
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
		SQLiteDatabase db=getWritableDatabase(PWD);
		// 读取现有图片列表
		Cursor c=db.query(T,new String[]{C_IMGS},C_ID+"=?",new String[]{String.valueOf(diaryId)},null,null,null);
		String json="[]";
		if(c.moveToFirst()) json=c.getString(0);
		c.close();

		// 追加新图片路径
		try{
			org.json.JSONArray arr=new org.json.JSONArray(json);
			arr.put(path);
			ContentValues v=new ContentValues();
			v.put(C_IMGS,arr.toString());
			v.put(C_AT,String.valueOf(System.currentTimeMillis()));
			db.update(T,v,C_ID+"=?",new String[]{String.valueOf(diaryId)});
		}catch(Exception e){
			// JSON解析失败则忽略
		}
		db.close();
	}
}