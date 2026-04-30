// 数据库助手 - 使用 cordova-sqlite-storage 插件
package com.journiv.plugin;

import android.content.Context;
import com.journiv.plugin.models.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import io.sqlc.SQLiteConnectorDatabase;
import io.sqlc.SQLitePlugin;

public class DBHelper{
	private static final String DB="journiv.db";
	private static final String T="diaries";
	private Context ctx;

	public DBHelper(Context ctx){
		this.ctx=ctx;
		initDB();
	}

	// 初始化数据库和表
	private void initDB(){
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		// 创建日记表
		db.executeSql("CREATE TABLE IF NOT EXISTS "+T+"("
				+"id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+"title TEXT,"
				+"content TEXT,"
				+"mood TEXT,"
				+"tags TEXT,"
				+"imgs TEXT DEFAULT '[]',"
				+"at TEXT"
				+")",new JSONArray(),null,null);
	}

	// 插入日记
	public long insert(String title,String content,String mood,String tags){
		final long[] result={-1};
		String sql="INSERT INTO "+T+"(title,content,mood,tags,at) VALUES(?,?,?,?,?)";
		JSONArray params=new JSONArray();
		params.put(title);
		params.put(content);
		params.put(mood);
		params.put(tags);
		params.put(String.valueOf(System.currentTimeMillis()));
		
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,params,
			(rs)->{
				result[0]=rs.getInsertId();
			},
			(err)->{}
		);
		return result[0];
	}

	// 查询所有日记
	public List<Entry> all(){
		List<Entry> list=new ArrayList<>();
		String sql="SELECT * FROM "+T+" ORDER BY at DESC";
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,new JSONArray(),
			(rs)->{
				for(int i=0;i<rs.getRows().length();i++){
					try{
						JSONObject row=rs.getRows().getJSONObject(i);
						Entry e=new Entry();
						e.id=row.getLong("id");
						e.title=row.getString("title");
						try{e.content=CryptoUtil.dec(row.getString("content"));}
						catch(Exception ex){e.content=row.getString("content");}
						e.mood=row.getString("mood");
						e.tags=row.getString("tags");
						e.imgs=row.optString("imgs","[]");
						e.created=row.getString("at");
						e.updated=row.getString("at");
						list.add(e);
					}catch(Exception ex){}
				}
			},
			(err)->{}
		);
		return list;
	}

	// 按条件查询
	public List<Entry> query(String where,String[] whereArgs,boolean decrypt){
		List<Entry> list=new ArrayList<>();
		String sql="SELECT * FROM "+T+" WHERE "+where+" ORDER BY at DESC";
		JSONArray params=new JSONArray();
		for(String arg:whereArgs) params.put(arg);
		
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,params,
			(rs)->{
				for(int i=0;i<rs.getRows().length();i++){
					try{
						JSONObject row=rs.getRows().getJSONObject(i);
						Entry e=new Entry();
						e.id=row.getLong("id");
						e.title=row.getString("title");
						String raw=row.getString("content");
						if(decrypt){
							try{e.content=CryptoUtil.dec(raw);}
							catch(Exception ex){e.content=raw;}
						}else{
							e.content=raw;
						}
						e.mood=row.getString("mood");
						e.tags=row.getString("tags");
						e.imgs=row.optString("imgs","[]");
						e.created=row.getString("at");
						e.updated=row.getString("at");
						list.add(e);
					}catch(Exception ex){}
				}
			},
			(err)->{}
		);
		return list;
	}

	// 更新日记
	public void update(long id,String title,String content,String mood,String tags){
		String sql="UPDATE "+T+" SET title=?,content=?,mood=?,tags=?,at=? WHERE id=?";
		JSONArray params=new JSONArray();
		params.put(title);
		params.put(content);
		params.put(mood);
		params.put(tags);
		params.put(String.valueOf(System.currentTimeMillis()));
		params.put(id);
		
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,params,null,null);
	}

	// 删除日记
	public void remove(long id){
		String sql="DELETE FROM "+T+" WHERE id=?";
		JSONArray params=new JSONArray();
		params.put(id);
		
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,params,null,null);
	}

	// 获取单条日记
	public Entry get(long id){
		final Entry[] result={null};
		String sql="SELECT * FROM "+T+" WHERE id=?";
		JSONArray params=new JSONArray();
		params.put(id);
		
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,params,
			(rs)->{
				if(rs.getRows().length()>0){
					try{
						JSONObject row=rs.getRows().getJSONObject(0);
						Entry e=new Entry();
						e.id=row.getLong("id");
						e.title=row.getString("title");
						try{e.content=CryptoUtil.dec(row.getString("content"));}
						catch(Exception ex){e.content=row.getString("content");}
						e.mood=row.getString("mood");
						e.tags=row.getString("tags");
						e.imgs=row.optString("imgs","[]");
						e.created=row.getString("at");
						e.updated=row.getString("at");
						result[0]=e;
					}catch(Exception ex){}
				}
			},
			(err)->{}
		);
		return result[0];
	}

	// 全文搜索（简化版，用 LIKE）
	public List<Entry> search(String q){
		List<Entry> list=new ArrayList<>();
		String sql="SELECT * FROM "+T+" WHERE title LIKE ? OR tags LIKE ? ORDER BY at DESC";
		String like="%"+q+"%";
		JSONArray params=new JSONArray();
		params.put(like);
		params.put(like);
		
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,params,
			(rs)->{
				for(int i=0;i<rs.getRows().length();i++){
					try{
						JSONObject row=rs.getRows().getJSONObject(i);
						Entry e=new Entry();
						e.id=row.getLong("id");
						e.title=row.getString("title");
						try{e.content=CryptoUtil.dec(row.getString("content"));}
						catch(Exception ex){e.content=row.getString("content");}
						e.mood=row.getString("mood");
						e.tags=row.getString("tags");
						e.imgs=row.optString("imgs","[]");
						e.created=row.getString("at");
						e.updated=row.getString("at");
						list.add(e);
					}catch(Exception ex){}
				}
			},
			(err)->{}
		);
		return list;
	}

	// 高级组合搜索
	public List<Entry> advSearch(String kw,String start,String end,String mood,String tags){
		List<Entry> list=new ArrayList<>();
		StringBuilder sql=new StringBuilder("SELECT * FROM "+T+" WHERE 1=1");
		JSONArray params=new JSONArray();
		
		if(kw!=null&&!kw.isEmpty()){
			sql.append(" AND (title LIKE ? OR tags LIKE ?)");
			String p="%"+kw+"%";
			params.put(p);
			params.put(p);
		}
		if(start!=null&&!start.isEmpty()){
			sql.append(" AND at >= ?");
			params.put(start);
		}
		if(end!=null&&!end.isEmpty()){
			sql.append(" AND at <= ?");
			params.put(end);
		}
		if(mood!=null&&!mood.isEmpty()){
			sql.append(" AND mood = ?");
			params.put(mood);
		}
		if(tags!=null&&!tags.isEmpty()){
			String[] ts=tags.split(",");
			sql.append(" AND (");
			for(int i=0;i<ts.length;i++){
				if(i>0) sql.append(" OR ");
				sql.append("tags LIKE ?");
				params.put("%"+ts[i].trim()+"%");
			}
			sql.append(")");
		}
		sql.append(" ORDER BY at DESC");
		
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql.toString(),params,
			(rs)->{
				for(int i=0;i<rs.getRows().length();i++){
					try{
						JSONObject row=rs.getRows().getJSONObject(i);
						Entry e=new Entry();
						e.id=row.getLong("id");
						e.title=row.getString("title");
						try{e.content=CryptoUtil.dec(row.getString("content"));}
						catch(Exception ex){e.content=row.getString("content");}
						e.mood=row.getString("mood");
						e.tags=row.getString("tags");
						e.imgs=row.optString("imgs","[]");
						e.created=row.getString("at");
						e.updated=row.getString("at");
						list.add(e);
					}catch(Exception ex){}
				}
			},
			(err)->{}
		);
		return list;
	}

	// 添加图片路径
	public void addImg(long diaryId,String path){
		Entry e=get(diaryId);
		if(e==null) return;
		try{
			JSONArray arr=new JSONArray(e.imgs);
			arr.put(path);
			String sql="UPDATE "+T+" SET imgs=?,at=? WHERE id=?";
			JSONArray params=new JSONArray();
			params.put(arr.toString());
			params.put(String.valueOf(System.currentTimeMillis()));
			params.put(diaryId);
			SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
			db.executeSql(sql,params,null,null);
		}catch(Exception ex){}
	}

	// 获取日记数量
	public int count(){
		final int[] result={0};
		String sql="SELECT COUNT(*) as c FROM "+T;
		SQLitePlugin db=new SQLitePlugin(ctx,DB,null,1);
		db.executeSql(sql,new JSONArray(),
			(rs)->{
				try{
					if(rs.getRows().length()>0){
						result[0]=rs.getRows().getJSONObject(0).getInt("c");
					}
				}catch(Exception ex){}
			},
			(err)->{}
		);
		return result[0];
	}
}