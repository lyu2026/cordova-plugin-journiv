package com.j.plugin;

import android.content.SharedPreferences;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.util.Base64;
import android.net.Uri;
import javax.net.ssl.*;
import org.json.*;
import java.util.*;
import okhttp3.*;
import java.io.*;

public class S{
	private static final String H="https://app.koofr.net/api/v2";
	private String U="lyuw2026@gmail.com"; // Koofr邮箱
	private String P="vy75aqa4naicde1r"; // 应用密码
	private String X="/tyan"; // 存储日记的文件夹名
	private String M; // 挂载点id

	private OkHttpClient Z; // HTTP客户端
	private Context C; // Android上下文

	S(Context _){
		this.C=_;
		Z=new OkHttpClient.Builder()
			.readTimeout(10,TimeUnit.SECONDS)
			.connectTimeout(10,TimeUnit.SECONDS)
			.sslSocketFactory(tls(),new X509TrustManager(){
				public void checkClientTrusted(X509Certificate[] _,String a){}
				public void checkServerTrusted(X509Certificate[] _,String a){}
				public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
			}).hostnameVerifier((h,s)->true).build();
	}
	private SSLSocketFactory tls(){
		try{
			SSLContext s=SSLContext.getInstance("TLS");
			s.init(null,new TrustManager[]{new X509TrustManager(){
				public void checkClientTrusted(X509Certificate[] _,String a){}
				public void checkServerTrusted(X509Certificate[] _,String a){}
				public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
			}},new java.security.SecureRandom());
			return s.getSocketFactory();
		}catch(Exception e){return null;}
	}

	// [S.call] HTTP请求 - _=路径 m=方法 x=请求数据
	private String call(String _,String m,String x)throws Exception{
		String u=H+_;
		if(_.contains("/files/put?")||_.contains("/files/get/"))u="https://app.koofr.net/content/api/v2"+_;
		Request.Builder r=new Request.Builder().url(u).header("Authorization","Basic "+Base64.encodeToString((U+":"+P).getBytes(),Base64.NO_WRAP));
		if(x!=null&&!x.isEmpty())r.method(m,RequestBody.create(x,MediaType.parse(x.startsWith("{")?"application/json":"application/octet-stream")));
		else r.method(m,null);
		try(Response z=Z.newCall(r.build()).execute()){
			if(!z.isSuccessful())throw new IOException(z.code()+" "+z.message());
			ResponseBody o=z.body();
			return o!=null?o.string():"";
		}
	}

	// [S.mid] 获取挂载
	private String mid()throws Exception{
		if(M==null||M.isEmpty())M=new JSONObject(call("/mounts","GET",null)).getJSONArray("mounts").getJSONObject(0).getString("id");
		return M;
	}

	// [S.list] 列出文件
	JSONArray list(String _)throws Exception{
		String o=call("/mounts/"+mid()+"/bundle?path="+java.net.URLEncoder.encode(_,"UTF-8"),"GET",null);
		return new JSONObject(o).optJSONArray("files");
	}

	// [S.upload] 上传文件
	JSONObject upload(String _,String p,String x)throws Exception{
		String o=call("/mounts/"+mid()+"/files/put?path="+java.net.URLEncoder.encode(p,"UTF-8")+"&filename="+java.net.URLEncoder.encode(_,"UTF-8")+"&info=true&overwrite=true","POST",x);
		return new JSONObject(o);
	}

	// [S.download] 下载文件
	String download(String _,String p)throws Exception{
		return call("/mounts/"+mid()+"/files/get/"+_+"?path="+java.net.URLEncoder.encode(p+"/"+_,"UTF-8")+"&force=true","GET",null);
	}

	// [S.remove] 删除文件
	JSONObject remove(JSONArray _)throws Exception{
		if(_.length()<1)return new JSONObject();
		JSONObject x=new JSONObject();
		JSONArray e=new JSONArray();
		String m=mid();
		for(int i=0;i<_.length();i++){
			JSONObject v=new JSONObject();
			v.put("path",X+"/"+_.optString(i));
			v.put("mountId",m);
			e.put(v);
		}
		x.put("files",e);
		String o=call("/jobs/files/remove","POST",x.string());
		return new JSONObject(o);
	}

	// [S.tclear] 清空回收
	void tclear()throws Exception{
		call("/trash","DELETE",null);
	}
	// [S.clear] 清空所有线上记录
	JSONObject clear()throws Exception{
		JSONArray xs=list(X),s=new JSONArray();
		for(int i=0;i<xs.length();i++)s.put(xs.getJSONObject(i).getString("name"));
		xs=list(X+"/files");
		for(int i=0;i<xs.length();i++)s.put("files/"+xs.getJSONObject(i).getString("name"));
		JSONObject o=null;
		try{o=remove(s);tclear();}catch(Exception e){}
		return o;
	}

	// [S.mkdir] 创建档夹
	void mkdir(String _,String p)throws Exception{
		call("/mounts/"+mid()+"/files/folder?path="+java.net.URLEncoder.encode(p,"UTF-8"),"POST","{\"name\":\""+_+"\"}");
	}

	// [S.cread] 读取本地文件 - 支持content://
	byte[] cread(String _)throws Exception{
		if(!_.startsWith("content://"))throw new IOException("文件不支持");
		InputStream z=C.getContentResolver().openInputStream(Uri.parse(_));
		ByteArrayOutputStream o=new ByteArrayOutputStream();
		byte[] x=new byte[4096];int n;
		while((n=z.read(x))!=-1)o.write(x,0,n);
		z.close();
		return o.toByteArray();
	}

	// [S.fetch] 下载URL文件
	byte[] fetch(String _)throws Exception{
		try(Response z=Z.newCall(new Request.Builder().url(_).build()).execute()){
			ResponseBody o=z.body();
			return o!=null?o.bytes():new byte[0];
		}
	}

	// [S.xpload] 上传单个文件(图片/附件) - _=源路径 p=文件名前缀
	String xpload(String _,String p)throws Exception{
		if(_==null||_.isEmpty()||_.startsWith(X+"/files/"))return _;
		byte[] b=_.startsWith("http")?fetch(_):cread(_);
		String[] v=_.split("\\?")[0].split("\\.");
		String x=v[v.length-1];
		if(x.contains("/")){
			v=x.split("/");
			x=v[v.length-1];
		}
		x=(x!=null&&!x.isEmpty())?("."+x):"";
		String o=p+"_"+System.currentTimeMillis()+x;
		JSONObject z=upload(o,X+"/files",new String(b,"ISO-8859-1"));
		return X+"/files/"+z.optString("name",o);
	}

	// [S.sync] 同步 - x=true本地覆盖线上 false=线上覆盖本地
	JSONObject sync(J.D _,boolean x)throws Exception{
		JSONArray ks=list(X);
		if(x){ // 本地覆盖线上：先清空远程再上传本地全部
			JSONArray kf=list(X+"/files"),ls=_.list();
			JSONObject os=new JSONObject();
			for(int i=0;i<kf.length();i++)os.put("files/"+kf.getJSONObject(i).getString("name"),"");
			for(int i=0;i<ls.length();i++){
				JSONObject v=ls.getJSONObject(i);
				JSONArray si=new JSONArray(v.optString("imgs")),sf=new JSONArray(v.optString("files"));
				if(si.length>0)for(int j=0;j<si.length;j++){
					String n=si.optString(j).replaceFirst(X+"/","");
					if(os.has(n))os.remove(n);
				}
				if(sf.length>0)for(int j=0;j<sf.length;j++){
					String n=sf.optString(j).replaceFirst(X+"/","");
					if(os.has(n))os.remove(n);
				}
			}
			for(int i=0;i<ks.length();i++)os.put(ks.getJSONObject(i).getString("name"),"");
			JSONArray ns=new JSONArray();
			Iterator<String> kk=os.keys();
			while(kk.hasNext())ns.put(kk.next());
			if(ns.length()>0)try{remove(ns);tclear();}catch(Exception e){}
			for(int i=0;i<ls.length();i++){
				JSONObject v=ls.getJSONObject(i);
				upload(v.optLong("id",0)+".json",J.encode(v.toString()),X);
			}
			return new JSONObject().put("ok",true);
		}
		// 线上覆盖本地：清空本地再下载远程全部
		_.clear(null);
		for(int i=0;i<ks.length();i++){
			JSONObject v=ks.getJSONObject(i);
			String n=v.getString("name");
			if(n.endsWith(".json")&&n.matches("\\d+\\.json")){
				try{
					JSONObject o=new JSONObject(J.decode(download(n,X)));
					o.put("id",Long.parseLong(n.replace(".json","")));
					_.save(o,true,false,null,false);
				}catch(Exception e){}
			}
		}
		return new JSONObject().put("ok",true);
	}

	// [S.log] 上传记录(含图片/附件清理) - _=记录id o=记录数据 oi=旧图片 of=旧附件
	void log(long _,JSONObject o,JSONArray si,JSONArray sf)throws Exception{
		JSONArray s=new JSONArray();
		if(si!=null){ // 删除旧图片
			for(int i=0;i<si.length();i++){
				String n=si.getString(i);
				if(n.startsWith(X+"/files/"))s.put("files/"+n);
			}
		}
		if(sf!=null){ // 删除旧附件
			for(int i=0;i<sf.length();i++){
				String n=sf.getString(i);
				if(n.startsWith(X+"/files/"))s.put("files/"+n);
			}
		}
		if(s.length>0)try{remove(s);}catch(Exception e){}
		// 上传新图片
		if(o.has("imgs")&&!o.isNull("imgs")){
			s=o.getJSONArray("imgs");
			JSONArray x=new JSONArray();
			for(int i=0;i<s.length();i++)x.put(xpload(s.getString(i),_+"_img_"+i+"_"));
			o.put("imgs",x);
		}
		// 上传新附件
		if(o.has("files")&&!o.isNull("files")){
			s=o.getJSONArray("files");
			JSONArray x=new JSONArray();
			for(int i=0;i<s.length();i++)x.put(xpload(s.getString(i),_+"_file_"+i+"_"));
			o.put("files",x);
		}
		// 上传记录JSON文件
		upload(_+".json",J.encode(o.toString()),X);
	}

}