package com.j.plugin;

import android.content.Context;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;

public class S{
	private String U="https://app.koofr.net/dav/Koofr/",M="lyuw2026@gmail.com",E="vy75aqa4naicde1r",X="tyan";
	private static final String L="list.txt";
	private Context C;

	S(Context c){this.C=c;}

	String getUrl(){return U;}

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

	private Map<Long,String> loadList()throws Exception{
		Map<Long,String> m=new LinkedHashMap<>();
		try{String t=req(X+"/"+L,"GET",null).trim();if(!t.isEmpty())for(String s:t.split(" ")){String[] p=s.split(":");if(p.length==2)m.put(Long.parseLong(p[0]),p[1]);}}catch(Exception e){}
		return m;
	}

	private void saveList(Map<Long,String> m)throws Exception{
		StringBuilder s=new StringBuilder();
		for(Map.Entry<Long,String> e:m.entrySet()){if(s.length()>0)s.append(" ");s.append(e.getKey()).append(":").append(e.getValue());}
		req(X+"/"+L,"PUT",s.toString());
	}

	private String putRecord(long id,JSONObject o)throws Exception{String fn=id+".json";req(X+"/"+fn,"PUT",J.enc(o.toString()));return fn;}

	private JSONObject getRecord(String fn)throws Exception{return new JSONObject(J.dec(req(X+"/"+fn,"GET",null)));}

	private void delRemote(String fn)throws Exception{try{req(X+"/"+fn,"DELETE",null);}catch(Exception e){}}

	String upFile(String p,String prefix)throws Exception{
		if(p==null||p.isEmpty()||p.startsWith("http"))return p;
		String path=p.replace("file://","");File f=new File(path);
		if(!f.exists()||!f.canRead())return p;
		try{byte[] b=new byte[(int)f.length()];try(FileInputStream i=new FileInputStream(f)){int t=0;while(t<b.length)t+=i.read(b,t,b.length-t);}String n=prefix+f.getName();req(X+"/files/"+n,"PUT",Base64.encodeToString(b,Base64.NO_WRAP));return U+X+"/files/"+n;}catch(Exception e){return p;}
	}

	private JSONObject packUpload(JSONObject r)throws Exception{
		if(r.has("imgs")&&!r.isNull("imgs")){JSONArray imgs=r.getJSONArray("imgs"),ni=new JSONArray();for(int j=0;j<imgs.length();j++)ni.put(upFile(imgs.getString(j),"img_"));r.put("imgs",ni);}
		if(r.has("files")&&!r.isNull("files")){JSONArray fs=r.getJSONArray("files"),nf=new JSONArray();for(int j=0;j<fs.length();j++)nf.put(upFile(fs.getString(j),"file_"));r.put("files",nf);}
		return r;
	}

	JSONObject init(J.D d)throws Exception{
		Map<Long,String> list=loadList();
		if(list.isEmpty()){saveList(new LinkedHashMap<>());return new JSONObject().put("ok",true).put("count",0);}
		d.clear(null);
		for(Map.Entry<Long,String> e:list.entrySet()){try{JSONObject r=getRecord(e.getValue());r.put("id",e.getKey());d.save(r,null);}catch(Exception ex){}}
		return new JSONObject().put("ok",true).put("count",list.size());
	}

	JSONObject sync(J.D d,boolean local)throws Exception{
		Map<Long,String> list=loadList();
		if(local){
			JSONArray all=d.allRaw();Set<Long> ids=new HashSet<>();Map<Long,String> nl=new LinkedHashMap<>();
			for(int i=0;i<all.length();i++){JSONObject r=all.getJSONObject(i);long id=r.getLong("id");r=packUpload(r);nl.put(id,putRecord(id,r));ids.add(id);d.updateLinks(id,r.optString("imgs","[]"),r.optString("files","[]"));}
			for(Map.Entry<Long,String> e:list.entrySet()){if(!ids.contains(e.getKey()))delRemote(e.getValue());}
			saveList(nl);return new JSONObject().put("ok",true);
		}else{
			d.clear(null);int c=0;
			for(Map.Entry<Long,String> e:list.entrySet()){try{JSONObject r=getRecord(e.getValue());r.put("id",e.getKey());d.save(r,null);c++;}catch(Exception ex){}}
			return new JSONObject().put("ok",true).put("count",c);
		}
	}

	void upRecord(long id,JSONObject r)throws Exception{r=packUpload(r);String fn=putRecord(id,r);Map<Long,String> list=loadList();list.put(id,fn);saveList(list);}

	void delRecord(long id)throws Exception{Map<Long,String> list=loadList();String fn=list.remove(id);if(fn!=null)delRemote(fn);saveList(list);}

	void delFile(String url)throws Exception{req(url.replace(U,""),"DELETE",null);}

	void clear()throws Exception{Map<Long,String> list=loadList();for(String fn:list.values())delRemote(fn);saveList(new LinkedHashMap<>());}

	private String ts(){return String.valueOf(System.currentTimeMillis());}
}