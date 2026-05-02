package com.j.plugin;

import java.security.cert.X509Certificate;
import android.content.SharedPreferences;
import java.net.HttpURLConnection;
import javax.net.ssl.TrustManager;
import android.content.Context;
import javax.net.ssl.SSLContext;
import android.util.Base64;
import javax.net.ssl.*;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.io.*;

// Koofr REST API 客户端
public class K{
	private static final String H="https://app.koofr.net/api/v2";
	private String U="lyuw2026@gmail.com",P="vy75aqa4naicde1r",X="tyan";
	private String M;
	private Context C;

	K(Context c){this.C=c;lod();}

	private void lod(){SharedPreferences p=C.getSharedPreferences("jcfg",Context.MODE_PRIVATE);U=p.getString("u",U);P=p.getString("p",P);X=p.getString("x",X);M=p.getString("m","");}

	private String mid()throws Exception{if(M==null||M.isEmpty()){M=new JSONObject(req("/mounts","GET",null)).getJSONArray("mounts").getJSONObject(0).getString("id");sav();}return M;}

	private void sav(){C.getSharedPreferences("jcfg",Context.MODE_PRIVATE).edit().putString("u",U).putString("p",P).putString("x",X).putString("m",M).apply();}

	private String req(String p,String m,String d)throws Exception{
		HttpURLConnection c;URL u=new URL(H+p);
		if(u.getProtocol().equals("https")){SSLContext s=SSLContext.getInstance("TLS");s.init(null,new TrustManager[]{new X509TrustManager(){public void checkClientTrusted(X509Certificate[] c,String a){}public void checkServerTrusted(X509Certificate[] c,String a){}public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}}},new java.security.SecureRandom());HttpsURLConnection h=(HttpsURLConnection)u.openConnection();h.setSSLSocketFactory(s.getSocketFactory());h.setHostnameVerifier((t,r)->true);c=h;}else c=(HttpURLConnection)u.openConnection();
		c.setRequestMethod(m);c.setConnectTimeout(10000);c.setReadTimeout(10000);
		c.setRequestProperty("Authorization","Basic "+Base64.encodeToString((U+":"+P).getBytes(),Base64.NO_WRAP));
		if(d!=null&&!d.isEmpty()){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(d.getBytes("UTF-8"));}}
		int x=c.getResponseCode();
		if(x>=200&&x<300){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));StringBuilder o=new StringBuilder();String l;while((l=r.readLine())!=null)o.append(l);r.close();c.disconnect();return o.toString();}
		c.disconnect();throw new IOException(x+" "+c.getResponseMessage());
	}

	private String enc(String s)throws Exception{return java.net.URLEncoder.encode(s,"UTF-8");}

	// 列出文件
	JSONArray ls(String p)throws Exception{return new JSONObject(req("/mounts/"+mid()+"/bundle?path="+enc(p),"GET",null)).optJSONArray("files");}
	// 上传文件
	JSONObject up(String n,String d,String p)throws Exception{return new JSONObject(req("/mounts/"+mid()+"/files/put?path="+enc(p)+"&filename="+enc(n)+"&info=true&overwrite=true","POST",d));}
	// 下载文件
	String dl(String p)throws Exception{return req("/mounts/"+mid()+"/files/get?path="+enc(p),"GET",null);}
	// 删除文件
	void rm(String p)throws Exception{req("/jobs/files/remove","POST","{\"files\":[{\"mountId\":\""+mid()+"\",\"path\":\""+p+"\"}]}");}
	// 清空回收站
	void et()throws Exception{req("/trash","DELETE",null);}
	// 创建文件夹
	void mkd(String p)throws Exception{req("/mounts/"+mid()+"/files/folder?path="+enc(p),"POST","{}");}

	// 读取本地文件
	byte[] rf(String p)throws Exception{
		InputStream i;
		if(p.startsWith("content://"))i=C.getContentResolver().openInputStream(Uri.parse(p));
		else if(p.startsWith("file://")){File f=new File(p.replace("file://",""));if(!f.exists())throw new IOException("文件不存在");i=new FileInputStream(f);}
		else{File f=new File(p);if(!f.exists())throw new IOException("文件不存在");i=new FileInputStream(f);}
		ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
		while((n=i.read(b))!=-1)o.write(b,0,n);
		i.close();return o.toByteArray();
	}

	// 下载远程文件
	byte[] dw(String u)throws Exception{
		HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
		c.setConnectTimeout(10000);c.setReadTimeout(10000);
		ByteArrayOutputStream o=new ByteArrayOutputStream();
		try(InputStream i=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=i.read(b))!=-1)o.write(b,0,n);}
		c.disconnect();return o.toByteArray();
	}

	// 上传单个文件(图片/附件)
	String uf(String src,String pf)throws Exception{
		if(src==null||src.isEmpty()||src.startsWith("https://"))return src;
		byte[] d=src.startsWith("http")?dw(src):rf(src);
		String n=pf+System.currentTimeMillis();
		return up(n,new String(d,"ISO-8859-1"),"/"+X+"/files/").optString("id",src);
	}

	// 同步
	JSONObject sync(J.D d,boolean local)throws Exception{
		if(local){JSONArray all=d.all();for(int i=0;i<all.length();i++){JSONObject r=all.getJSONObject(i);long id=r.getLong("id");JSONArray oi=r.has("imgs")?r.getJSONArray("imgs"):null,of=r.has("files")?r.getJSONArray("files"):null;upr(id,r,oi,of);}return new JSONObject().put("ok",true).put("count",all.length());}
		else{JSONArray fs=ls("/"+X+"/");d.clear(null);int c=0;if(fs!=null)for(int i=0;i<fs.length();i++){String n=fs.getJSONObject(i).getString("name");if(n.endsWith(".json")&&n.matches("\\d+\\.json")){long id=Long.parseLong(n.replace(".json",""));try{JSONObject r=new JSONObject(J.dec(dl("/"+X+"/"+n)));r.put("id",id);d.raw(r);c++;}catch(Exception e){}}}return new JSONObject().put("ok",true).put("count",c);}
	}

	// 上传记录
	void upr(long id,JSONObject o,JSONArray oi,JSONArray of)throws Exception{
		if(oi!=null)for(int i=0;i<oi.length();i++){String s=oi.getString(i);if(s.startsWith("/"))try{rm(s);}catch(Exception e){}}
		if(of!=null)for(int i=0;i<of.length();i++){String s=of.getString(i);if(s.startsWith("/"))try{rm(s);}catch(Exception e){}}
		if(o.has("imgs")&&!o.isNull("imgs")){JSONArray im=o.getJSONArray("imgs"),ni=new JSONArray();for(int j=0;j<im.length();j++)ni.put(uf(im.getString(j),"img_"+id+"_"+j));o.put("imgs",ni);}
		if(o.has("files")&&!o.isNull("files")){JSONArray fi=o.getJSONArray("files"),nf=new JSONArray();for(int j=0;j<fi.length();j++)nf.put(uf(fi.getString(j),"file_"+id+"_"+j));o.put("files",nf);}
		up(id+".json",J.enc(o.toString()),"/"+X+"/");
	}

	// 删除记录
	void delr(long id)throws Exception{rm("/"+X+"/"+id+".json");}

	// 删除远程文件(按路径)
	void delf(String p)throws Exception{if(p!=null&&p.startsWith("/"))rm(p);}

	// 清空线上
	void clear()throws Exception{JSONArray fs=ls("/"+X+"/");if(fs!=null)for(int i=0;i<fs.length();i++)try{rm("/"+X+"/"+fs.getJSONObject(i).getString("name"));}catch(Exception e){}et();}
}