package com.j.plugin;

import android.content.Context;
import android.net.Uri;
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

	private String req(String p,String m,String d)throws Exception{
		HttpURLConnection c;URL u=new URL(U+p);
		if(u.getProtocol().equals("https")){SSLContext s=SSLContext.getInstance("TLS");s.init(null,new TrustManager[]{new X509TrustManager(){public void checkClientTrusted(X509Certificate[] c,String a){}public void checkServerTrusted(X509Certificate[] c,String a){}public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}}},new java.security.SecureRandom());HttpsURLConnection h=(HttpsURLConnection)u.openConnection();h.setSSLSocketFactory(s.getSocketFactory());h.setHostnameVerifier((t,r)->true);c=h;}else c=(HttpURLConnection)u.openConnection();
		c.setRequestMethod(m);c.setConnectTimeout(5000);c.setReadTimeout(5000);
		c.setRequestProperty("Authorization","Basic "+Base64.encodeToString((M+":"+E).getBytes(),Base64.NO_WRAP));
		if(d!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/octet-stream");try(OutputStream o=c.getOutputStream()){o.write(d.getBytes("UTF-8"));}}
		int x=c.getResponseCode();
		if(x>=200&&x<300){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));StringBuilder o=new StringBuilder();String l;while((l=r.readLine())!=null)o.append(l);r.close();c.disconnect();return o.toString();}
		c.disconnect();throw new IOException(x+" "+c.getResponseMessage());
	}

	private byte[] readLocal(String p)throws Exception{
		InputStream i;
		if(p.startsWith("content://"))i=C.getContentResolver().openInputStream(Uri.parse(p));
		else if(p.startsWith("file://")){File f=new File(p.replace("file://",""));if(!f.exists()||!f.canRead())throw new IOException("无法读取");i=new FileInputStream(f);}
		else{File f=new File(p);if(!f.exists()||!f.canRead())throw new IOException("无法读取");i=new FileInputStream(f);}
		ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
		while((n=i.read(b))!=-1)o.write(b,0,n);
		i.close();return o.toByteArray();
	}

	private byte[] download(String u)throws Exception{
		HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
		c.setConnectTimeout(10000);c.setReadTimeout(10000);
		ByteArrayOutputStream o=new ByteArrayOutputStream();
		try(InputStream i=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=i.read(b))!=-1)o.write(b,0,n);}
		c.disconnect();return o.toByteArray();
	}

	private String uploadFile(byte[] d,String n)throws Exception{
		req(X+"/files/"+n,"PUT",Base64.encodeToString(d,Base64.NO_WRAP));
		return U+X+"/files/"+n;
	}

	private String procImg(String s,long id,int idx)throws Exception{
		if(s==null||s.isEmpty()||s.startsWith(U+X))return s;
		String e=".jpg";if(s.contains(".")){String t=s.substring(s.lastIndexOf("."));if(t.length()<=5)e=t;}
		return uploadFile(s.startsWith("http://")||s.startsWith("https://")?download(s):readLocal(s),"img_"+id+"_"+idx+e);
	}

	private String procFile(String s,long id,int idx)throws Exception{
		if(s==null||s.isEmpty()||s.startsWith(U+X))return s;
		String e=".bin";if(s.contains("/"))s=s.substring(s.lastIndexOf("/")+1);
		if(s.contains(".")){String t=s.substring(s.lastIndexOf("."));if(t.length()<=5)e=t;}
		return uploadFile(s.startsWith("http://")||s.startsWith("https://")?download(s):readLocal(s),"file_"+id+"_"+idx+e);
	}

	private void cleanFiles(long id,JSONArray im,JSONArray fi)throws Exception{
		if(im!=null)for(int i=0;i<im.length();i++){String s=im.getString(i);if(s.startsWith(U+X))delRemote(s.replace(U+X+"/files/","files/"));}
		if(fi!=null)for(int i=0;i<fi.length();i++){String s=fi.getString(i);if(s.startsWith(U+X))delRemote(s.replace(U+X+"/files/","files/"));}
	}

	JSONObject packUpload(JSONObject r,JSONArray oi,JSONArray of)throws Exception{
		long id=r.optLong("id",0);cleanFiles(id,oi,of);
		if(r.has("imgs")&&!r.isNull("imgs")){JSONArray im=r.getJSONArray("imgs"),ni=new JSONArray();for(int j=0;j<im.length();j++)ni.put(procImg(im.getString(j),id,j));r.put("imgs",ni);}
		if(r.has("files")&&!r.isNull("files")){JSONArray fi=r.getJSONArray("files"),nf=new JSONArray();for(int j=0;j<fi.length();j++)nf.put(procFile(fi.getString(j),id,j));r.put("files",nf);}
		return r;
	}

	private Map<Long,String> loadList(){
		Map<Long,String> m=new LinkedHashMap<>();
		try{String t=req(X+"/"+L,"GET",null).trim();if(!t.isEmpty())for(String s:t.split(" ")){String[] p=s.split(":");if(p.length==2)m.put(Long.parseLong(p[0]),p[1]);}}catch(Exception e){}
		return m;
	}

	private void saveList(Map<Long,String> m)throws Exception{
		StringBuilder s=new StringBuilder();
		for(Map.Entry<Long,String> e:m.entrySet()){if(s.length()>0)s.append(" ");s.append(e.getKey()).append(":").append(e.getValue());}
		req(X+"/"+L,"PUT",s.toString());
	}

	private String putRecord(long id,JSONObject o)throws Exception{String n=id+".json";req(X+"/"+n,"PUT",J.enc(o.toString()));return n;}

	private void delRemote(String n)throws Exception{try{req(X+"/"+n,"DELETE",null);}catch(Exception e){}}

	public JSONArray lone(long id,J.D d){
		try{
			if(id<=0){
				Map<Long,String> m=loadList();
				JSONArray a=new JSONArray();
				for(long k:m.keySet())a.put(k);
				return a;
			}else{
				Map<Long,String> m=loadList();
				String n=m.get(id);
				if(n==null)return new JSONArray();
				JSONObject r=new JSONObject(J.dec(req(X+"/"+n,"GET",null)));
				r.put("id",id);
				JSONArray a=new JSONArray();
				a.put(r);
				return a;
			}
		}catch(Exception e){
			return new JSONArray();
		}
	}

	public void upRecord(long id,JSONObject r,JSONArray oi,JSONArray of)throws Exception{
		r=packUpload(r,oi,of);String n=putRecord(id,r);Map<Long,String> list=loadList();list.put(id,n);saveList(list);
	}

	public void delRecord(long id)throws Exception{Map<Long,String> list=loadList();String n=list.remove(id);if(n!=null)delRemote(n);saveList(list);}

	public void delFile(String u)throws Exception{req(u.replace(U,""),"DELETE",null);}

	public void clear()throws Exception{Map<Long,String> list=loadList();for(String n:list.values())delRemote(n);saveList(new LinkedHashMap<>());}
}