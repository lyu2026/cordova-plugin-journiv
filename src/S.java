package com.j.plugin;

import java.security.cert.X509Certificate;
import android.content.Context;
import android.util.Base64;
import javax.net.ssl.*;
import org.json.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class S{
	private String U="https://app.koofr.net/dav/Koofr/",M="lyuw2026@gmail.com",E="vy75aqa4naicde1r",X="tyan";

	S(Context c){}

	String getUrl(){return U;}

	private String req(String P,String m,String D)throws Exception{
		HttpURLConnection c;URL u=new URL(U+P);
		if(u.getProtocol().equals("https")){SSLContext s=SSLContext.getInstance("TLS");s.init(null,new TrustManager[]{new X509TrustManager(){public void checkClientTrusted(X509Certificate[] c,String a){}public void checkServerTrusted(X509Certificate[] c,String a){}public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}}},new java.security.SecureRandom());HttpsURLConnection h=(HttpsURLConnection)u.openConnection();h.setSSLSocketFactory(s.getSocketFactory());h.setHostnameVerifier((t,r)->true);c=h;}else c=(HttpURLConnection)u.openConnection();
		c.setRequestMethod(m);c.setConnectTimeout(15000);c.setReadTimeout(15000);
		c.setRequestProperty("Authorization","Basic "+Base64.encodeToString((M+":"+E).getBytes(),Base64.NO_WRAP));
		if(D!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/octet-stream");try(OutputStream o=c.getOutputStream()){o.write(D.getBytes("UTF-8"));}}
		int x=c.getResponseCode();
		if(x>=200&&x<300||x==207){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));StringBuilder o=new StringBuilder();String l;while((l=r.readLine())!=null)o.append(l);r.close();c.disconnect();return o.toString();}
		c.disconnect();throw new IOException(x+" "+c.getResponseMessage());
	}

	String upFile(String p,String prefix)throws Exception{
		if(p==null||p.isEmpty()||p.startsWith("http"))return p;
		File f=new File(p.replace("file://",""));if(!f.exists())return p;
		byte[] b=new byte[(int)f.length()];try(FileInputStream i=new FileInputStream(f)){i.read(b);}
		String n=prefix+ts()+"_"+f.getName();req(X+"/files/"+n,"PUT",Base64.encodeToString(b,Base64.NO_WRAP));
		return U+X+"/files/"+n;
	}

	private JSONArray packAndUpload(JSONArray a)throws Exception{
		try{req(X+"/files","MKCOL",null);}catch(Exception e){}
		for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);
			if(r.has("imgs")&&!r.isNull("imgs")){JSONArray imgs=r.getJSONArray("imgs"),ni=new JSONArray();for(int j=0;j<imgs.length();j++)ni.put(upFile(imgs.getString(j),"img_"));r.put("imgs",ni);}
			if(r.has("files")&&!r.isNull("files")){JSONArray fs=r.getJSONArray("files"),nf=new JSONArray();for(int j=0;j<fs.length();j++)nf.put(upFile(fs.getString(j),"file_"));r.put("files",nf);}
		}return a;
	}

	void up(JSONArray a)throws Exception{req(X+"/"+ts()+".json","PUT",J.enc(packAndUpload(a).toString()));}

	JSONArray down()throws Exception{
		String x=req(X,"PROPFIND","<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:displayname/><d:getlastmodified/></d:prop></d:propfind>");
		List<String> ls=new ArrayList<>();for(String l:x.split("<d:response>")){if(l.contains("<d:displayname>")&&!l.contains("<d:collection/>")){int a=l.indexOf("<d:displayname>")+16,b=l.indexOf("</d:displayname>");if(a>0&&b>a){String n=l.substring(a,b);if(n.startsWith("diary_"))ls.add(n);}}}
		if(ls.isEmpty())return new JSONArray();Collections.sort(ls,Collections.reverseOrder());
		return new JSONArray(J.dec(req(X+"/"+ls.get(0),"GET",null)));
	}

	JSONObject sync(J.D d,boolean local)throws Exception{
		if(local){JSONArray r=d.allRaw(),u=packAndUpload(r);req(X+"/"+ts()+".json","PUT",J.enc(u.toString()));for(int i=0;i<u.length();i++){JSONObject o=u.getJSONObject(i);d.updateLinks(o.getLong("id"),o.optString("imgs","[]"),o.optString("files","[]"));}JSONObject o=new JSONObject();o.put("ok",true);return o;}
		else{JSONArray a=down();d.clear(null);for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);r.put("id",0);d.save(r,null);}JSONObject o=new JSONObject();o.put("ok",true);o.put("count",a.length());return o;}
	}

	void delFile(String url)throws Exception{req(url.replace(U,""),"DELETE",null);}

	void clear()throws Exception{
		String x=req(X,"PROPFIND","<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:displayname/><d:getlastmodified/></d:prop></d:propfind>");
		List<String> ls=new ArrayList<>();for(String l:x.split("<d:response>")){if(l.contains("<d:displayname>")&&!l.contains("<d:collection/>")){int a=l.indexOf("<d:displayname>")+16,b=l.indexOf("</d:displayname>");if(a>0&&b>a){String n=l.substring(a,b);if(n.startsWith("diary_"))ls.add(n);}}}
		for(String n:ls)req(X+"/"+n,"DELETE",null);
	}

	private String ts(){return String.valueOf(System.currentTimeMillis());}
}