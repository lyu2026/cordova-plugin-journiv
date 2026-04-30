// WebDAV同步管理器 - 将日记备份到WebDAV服务器
package com.journiv.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;

public class SyncManager{
	private Context ctx;
	private String url; // WebDAV服务器地址
	private String user; // 用户名
	private String pass; // 密码
	private String folder; // 备份文件夹名

	public SyncManager(Context ctx){
		this.ctx=ctx;
		loadCfg();
	}

	// 加载保存的配置
	private void loadCfg(){
		this.url="https://app.koofr.net/dav/Koofr/";
		this.user="lyuw2026@gmail.com";
		this.pass="vy75aqa4naicde1r";
		this.folder="tyan";
		SharedPreferences p=ctx.getSharedPreferences("sync",Context.MODE_PRIVATE);
		p.edit()
		 .putString("url",this.url)
		 .putString("user",this.user)
		 .putString("pass",this.pass)
		 .putString("folder",this.folder)
		 .apply();
	}

	// 是否已配置
	public boolean isReady(){
		return url!=null&&!url.isEmpty()&&user!=null&&!user.isEmpty();
	}

	// 上传日记数据
	public JSONObject up(JSONArray diaries)throws Exception{
		JSONObject res=new JSONObject();
		try{
			String name="diary_"+ts()+".json";
			String data=CryptoUtil.enc(diaries.toString());
			String fullPath=folder+"/"+name;
			boolean ok=put(fullPath,data);
			if(ok){
				res.put("ok",true);
				res.put("file",name);
				ctx.getSharedPreferences("sync",Context.MODE_PRIVATE).edit().putLong("last",System.currentTimeMillis()).apply();
			}else{
				// 如果PUT失败，尝试先创建目录再上传
				try{mkdir(folder);}catch(Exception ex){}
				ok=put(fullPath,data);
				if(ok){
					res.put("ok",true);
					res.put("file",name);
				}else{
					res.put("ok",false);
					res.put("msg","上传失败");
				}
			}
		}catch(Exception e){
			res.put("ok",false);
			res.put("msg",e.getMessage());
		}
		return res;
	}

	// 上传文件 PUT
	private boolean put(String path,String body)throws Exception{
		HttpURLConnection c=conn(path,"PUT");
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type","application/octet-stream");
		try(OutputStream out=c.getOutputStream()){
			out.write(body.getBytes("UTF-8"));
		}
		int code=c.getResponseCode();
		c.disconnect();
		return code==201||code==204||code==200;
	}

	// 创建文件夹 MKCOL
	private void mkdir(String path)throws Exception{
		HttpURLConnection c=conn(path,"MKCOL");
		c.disconnect();
	}

	// 下载最新备份
	public JSONObject down()throws Exception{
		JSONObject res=new JSONObject();
		try{
			// 列出备份文件
			List<String> files=ls(folder);
			if(files.isEmpty()){
				res.put("ok",false);
				res.put("msg","无备份文件");
				return res;
			}
			// 取最新文件
			Collections.sort(files,Collections.reverseOrder());
			String latest=files.get(0);
			String enc=get(folder+"/"+latest);
			String json=CryptoUtil.dec(enc);
			res.put("ok",true);
			res.put("data",new JSONArray(json));
			res.put("file",latest);
		}catch(Exception e){
			res.put("ok",false);
			res.put("msg",e.getMessage());
		}
		return res;
	}

	// 创建HTTP连接并设置认证
	private HttpURLConnection conn(String path,String method)throws Exception{
		URL u=new URL(url+path);
		HttpURLConnection c;
		if(u.getProtocol().equals("https")){
			// HTTPS: 信任所有证书
			SSLContext ssl=SSLContext.getInstance("TLS");
			ssl.init(null,new TrustManager[]{new X509TrustManager(){
				public void checkClientTrusted(X509Certificate[] c,String a){}
				public void checkServerTrusted(X509Certificate[] c,String a){}
				public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
			}},new java.security.SecureRandom());
			HttpsURLConnection hc=(HttpsURLConnection)u.openConnection();
			hc.setSSLSocketFactory(ssl.getSocketFactory());
			hc.setHostnameVerifier((host,session)->true);
			c=hc;
		}else{
			c=(HttpURLConnection)u.openConnection();
		}
		c.setRequestMethod(method);
		c.setConnectTimeout(15000);
		c.setReadTimeout(15000);
		// Basic认证
		if(user!=null&&!user.isEmpty()){
			String auth=user+":"+pass;
			c.setRequestProperty("Authorization","Basic "+Base64.encodeToString(auth.getBytes(),Base64.NO_WRAP));
		}
		return c;
	}

	// 下载文件 GET
	private String get(String path)throws Exception{
		HttpURLConnection c=conn(path,"GET");
		int code=c.getResponseCode();
		if(code!=200) throw new IOException("HTTP "+code);
		String r=read(c.getInputStream());
		c.disconnect();
		return r;
	}

	// 列出文件 PROPFIND
	private List<String> ls(String path)throws Exception{
		List<String> list=new ArrayList<>();
		HttpURLConnection c=conn(path,"PROPFIND");
		c.setRequestProperty("Depth","1");
		// PROPFIND请求体
		String body="<?xml version=\"1.0\"?>"
				+"<d:propfind xmlns:d=\"DAV:\">"
				+"<d:prop><d:displayname/><d:getlastmodified/></d:prop>"
				+"</d:propfind>";
		c.setDoOutput(true);
		try(OutputStream out=c.getOutputStream()){
			out.write(body.getBytes("UTF-8"));
		}
		if(c.getResponseCode()==207){
			String xml=read(c.getInputStream());
			// 简单解析XML提取文件名
			for(String line:xml.split("<d:response>")){
				if(line.contains("<d:displayname>")&&!line.contains("<d:collection/>")){
					int a=line.indexOf("<d:displayname>")+16;
					int b=line.indexOf("</d:displayname>");
					if(a>0&&b>a){
						String name=line.substring(a,b);
						if(name.startsWith("diary_")) list.add(name);
					}
				}
			}
		}
		c.disconnect();
		return list;
	}

	// 读取流为字符串
	private String read(InputStream in)throws IOException{
		BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));
		StringBuilder sb=new StringBuilder();
		String line;
		while((line=r.readLine())!=null) sb.append(line);
		r.close();
		return sb.toString();
	}

	// 获取时间戳
	private String ts(){
		return String.valueOf(System.currentTimeMillis());
	}

	// 获取同步状态
	public JSONObject status(){
		JSONObject s=new JSONObject();
		try{
			SharedPreferences p=ctx.getSharedPreferences("sync",Context.MODE_PRIVATE);
			s.put("ready",isReady());
			s.put("last",p.getLong("last",0));
			s.put("url",url);
			s.put("folder",folder);
		}catch(Exception e){}
		return s;
	}
}