package com.j.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;

public class S{
	private String U="https://app.koofr.net/dav/Koofr/",M="lyuw2026@gmail.com",E="vy75aqa4naicde1r",X="tyan";
	private Context C;

	S(Context c){this.C=c;}

	String getUrl(){return U;}

	private void addFileLog(String n){SharedPreferences p=C.getSharedPreferences("slog",Context.MODE_PRIVATE);JSONArray a=getFileLog();a.put(n);p.edit().putString("fl",a.toString()).apply();}
	private JSONArray getFileLog(){SharedPreferences p=C.getSharedPreferences("slog",Context.MODE_PRIVATE);try{return new JSONArray(p.getString("fl","[]"));}catch(Exception e){return new JSONArray();}}
	private void clearFileLog(){C.getSharedPreferences("slog",Context.MODE_PRIVATE).edit().putString("fl","[]").apply();}

	private String req(String P,String m,String D)throws Exception{
		HttpURLConnection c;URL u=new URL(U+P);
		if(u.getProtocol().equals("https")){SSLContext s=SSLContext.getInstance("TLS");s.init(null,new TrustManager[]{new X509TrustManager(){public void checkClientTrusted(X509Certificate[] c,String a){}public void checkServerTrusted(X509Certificate[] c,String a){}public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}}},new java.security.SecureRandom());HttpsURLConnection h=(HttpsURLConnection)u.openConnection();h.setSSLSocketFactory(s.getSocketFactory());h.setHostnameVerifier((t,r)->true);c=h;}else c=(HttpURLConnection)u.openConnection();
		c.setRequestMethod(m);c.setConnectTimeout(15000);c.setReadTimeout(15000);
		c.setRequestProperty("Authorization","Basic "+Base64.encodeToString((M+":"+E).getBytes(),Base64.NO_WRAP));
		if(D!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/octet-stream");try(OutputStream o=c.getOutputStream()){o.write(D.getBytes("UTF-8"));}}
		int x=c.getResponseCode();
		if(x>=200&&x<300){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));StringBuilder o=new StringBuilder();String l;while((l=r.readLine())!=null)o.append(l);r.close();c.disconnect();return o.toString();}
		c.disconnect();throw new IOException(x+" "+c.getResponseMessage());
	}

	String upFile(String p,String prefix)throws Exception{
		if(p==null||p.isEmpty()||p.startsWith("http"))return p;
		File f=new File(p.replace("file://",""));if(!f.exists())return p;
		byte[] b=new byte[(int)f.length()];try(FileInputStream i=new FileInputStream(f)){i.read(b);}
		String n=prefix+ts()+"_"+f.getName();req(X+"/files/"+n,"PUT",Base64.encodeToString(b,Base64.NO_WRAP));
		addFileLog("files/"+n);return U+X+"/files/"+n;
	}

	private JSONArray packAndUpload(JSONArray a)throws Exception{
		for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);
			if(r.has("imgs")&&!r.isNull("imgs")){JSONArray imgs=r.getJSONArray("imgs"),ni=new JSONArray();for(int j=0;j<imgs.length();j++)ni.put(upFile(imgs.getString(j),"img_"));r.put("imgs",ni);}
			if(r.has("files")&&!r.isNull("files")){JSONArray fs=r.getJSONArray("files"),nf=new JSONArray();for(int j=0;j<fs.length();j++)nf.put(upFile(fs.getString(j),"file_"));r.put("files",nf);}
		}return a;
	}

	void up(JSONArray a)throws Exception{
		String fn=ts()+".json";JSONArray b=packAndUpload(a);
		req(X+"/"+fn,"PUT",J.enc(b.toString()));addFileLog(fn);
	}

	JSONArray down()throws Exception{
		JSONArray fl=getFileLog();int n=fl.length();if(n==0)return new JSONArray();
		String last="";for(int i=n-1;i>=0;i--){String f=fl.getString(i);if(f.endsWith(".json")){last=f;break;}}
		if(last.isEmpty())return new JSONArray();
		return new JSONArray(J.dec(req(X+"/"+last,"GET",null)));
	}

	JSONObject sync(J.D d,boolean local)throws Exception{
		if(local){JSONArray r=d.allRaw(),u=packAndUpload(r);String fn=ts()+".json";req(X+"/"+fn,"PUT",J.enc(u.toString()));addFileLog(fn);for(int i=0;i<u.length();i++){JSONObject o=u.getJSONObject(i);d.updateLinks(o.getLong("id"),o.optString("imgs","[]"),o.optString("files","[]"));}return new JSONObject().put("ok",true).put("file",fn);}
		else{JSONArray a=down();if(a.length()==0)return new JSONObject().put("ok",false).put("msg","无备份数据");d.clear(null);for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);r.put("id",0);d.save(r,null);}return new JSONObject().put("ok",true).put("count",a.length());}
	}

	void delFile(String url)throws Exception{req(url.replace(U,""),"DELETE",null);}

	void clear()throws Exception{JSONArray fl=getFileLog();for(int i=0;i<fl.length();i++){try{req(X+"/"+fl.getString(i),"DELETE",null);}catch(Exception e){}}clearFileLog();}

	private String ts(){return String.valueOf(System.currentTimeMillis());}
}