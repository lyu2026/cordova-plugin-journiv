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

	private String req(String P,String m,String D)throws Exception{
		HttpURLConnection c;URL u=new URL(U+P);
		if(u.getProtocol().equals("https")){SSLContext s=SSLContext.getInstance("TLS");s.init(null,new TrustManager[]{new X509TrustManager(){public void checkClientTrusted(X509Certificate[] c,String a){}public void checkServerTrusted(X509Certificate[] c,String a){}public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}}},new java.security.SecureRandom());HttpsURLConnection h=(HttpsURLConnection)u.openConnection();h.setSSLSocketFactory(s.getSocketFactory());h.setHostnameVerifier((t,r)->true);c=h;}else c=(HttpURLConnection)u.openConnection();
		c.setRequestMethod(m);c.setConnectTimeout(5000);c.setReadTimeout(5000);
		c.setRequestProperty("Authorization","Basic "+Base64.encodeToString((M+":"+E).getBytes(),Base64.NO_WRAP));
		if(D!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/octet-stream");try(OutputStream o=c.getOutputStream()){o.write(D.getBytes("UTF-8"));}}
		int x=c.getResponseCode();
		if(x>=200&&x<300){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));StringBuilder o=new StringBuilder();String l;while((l=r.readLine())!=null)o.append(l);r.close();c.disconnect();return o.toString();}
		c.disconnect();throw new IOException(x+" "+c.getResponseMessage());
	}

	// 读取本地文件(支持 file:// 和 content://)
	private byte[] readLocal(String path)throws Exception{
		InputStream i=null;
		if(path.startsWith("content://")){
			i=C.getContentResolver().openInputStream(Uri.parse(path));
		}else if(path.startsWith("file://")){
			String p=path.replace("file://","");
			File f=new File(p);
			if(!f.exists()||!f.canRead())throw new IOException("无法读取文件: "+p);
			i=new FileInputStream(f);
		}else{
			File f=new File(path);
			if(!f.exists()||!f.canRead())throw new IOException("无法读取文件: "+path);
			i=new FileInputStream(f);
		}
		ByteArrayOutputStream o=new ByteArrayOutputStream();
		byte[] b=new byte[4096];int n;
		while((n=i.read(b))!=-1)o.write(b,0,n);
		i.close();return o.toByteArray();
	}

	// 下载远程文件
	private byte[] download(String url)throws Exception{
		HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
		c.setConnectTimeout(10000);c.setReadTimeout(10000);
		ByteArrayOutputStream o=new ByteArrayOutputStream();
		try(InputStream i=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=i.read(b))!=-1)o.write(b,0,n);}
		c.disconnect();return o.toByteArray();
	}

	// 上传文件到线上
	private String uploadFile(byte[] data,String name)throws Exception{
		req(X+"/files/"+name,"PUT",Base64.encodeToString(data,Base64.NO_WRAP));
		return U+X+"/files/"+name;
	}

	// 处理图片
	private String procImg(String src,long recId,int idx)throws Exception{
		if(src==null||src.isEmpty())return src;
		if(src.startsWith(U+X))return src; // 已是线上文件
		String ext=".jpg";
		if(src.contains(".")){String t=src.substring(src.lastIndexOf("."));if(t.length()<=5)ext=t;}
		byte[] data;
		if(src.startsWith("http://")||src.startsWith("https://")){
			if(src.startsWith(U+X))return src;
			data=download(src);
		}else{
			data=readLocal(src);
		}
		return uploadFile(data,"img_"+recId+"_"+idx+ext);
	}

	// 处理附件
	private String procFile(String src,long recId,int idx)throws Exception{
		if(src==null||src.isEmpty())return src;
		if(src.startsWith(U+X))return src;
		String ext=".bin";
		if(src.contains("/"))src=src.substring(src.lastIndexOf("/")+1);
		if(src.contains(".")){String t=src.substring(src.lastIndexOf("."));if(t.length()<=5)ext=t;}
		byte[] data;
		if(src.startsWith("http://")||src.startsWith("https://")){
			data=download(src);
		}else{
			data=readLocal(src);
		}
		return uploadFile(data,"file_"+recId+"_"+idx+ext);
	}

	// 删除指定记录关联的文件
	private void cleanFiles(long recId,JSONArray oldImgs,JSONArray oldFiles)throws Exception{
		if(oldImgs!=null)for(int i=0;i<oldImgs.length();i++){String s=oldImgs.getString(i);if(s.startsWith(U+X))delRemote(s.replace(U+X+"/files/","files/"));}
		if(oldFiles!=null)for(int i=0;i<oldFiles.length();i++){String s=oldFiles.getString(i);if(s.startsWith(U+X))delRemote(s.replace(U+X+"/files/","files/"));}
	}

	JSONObject packUpload(JSONObject r,JSONArray oldImgs,JSONArray oldFiles)throws Exception{
		long recId=r.optLong("id",0);
		cleanFiles(recId,oldImgs,oldFiles);
		if(r.has("imgs")&&!r.isNull("imgs")){JSONArray imgs=r.getJSONArray("imgs"),ni=new JSONArray();for(int j=0;j<imgs.length();j++)ni.put(procImg(imgs.getString(j),recId,j));r.put("imgs",ni);}
		if(r.has("files")&&!r.isNull("files")){JSONArray fs=r.getJSONArray("files"),nf=new JSONArray();for(int j=0;j<fs.length();j++)nf.put(procFile(fs.getString(j),recId,j));r.put("files",nf);}
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

	private String putRecord(long id,JSONObject o)throws Exception{String fn=id+".json";req(X+"/"+fn,"PUT",J.enc(o.toString()));return fn;}

	private void delRemote(String fn)throws Exception{try{req(X+"/"+fn,"DELETE",null);}catch(Exception e){}}

	public JSONObject lmap(){
		Map<Long,String> m=loadList();
		JSONObject o=new JSONObject();
		try{for(Map.Entry<Long,String> e:m.entrySet())o.put(String.valueOf(e.getKey()),e.getValue());}catch(Exception ex){}
		return o;
	}

	public JSONObject lsync(long id,J.D d)throws Exception{
		Map<Long,String> m=loadList();
		String fn=m.get(id);
		if(fn==null)return new JSONObject().put("ok",false).put("msg","记录不存在");
		JSONObject r=new JSONObject(J.dec(req(X+"/"+fn,"GET",null)));
		r.put("id",id);
		d.save(r,null);
		return new JSONObject().put("ok",true);
	}

	public JSONObject sync(J.D d,boolean local)throws Exception{
		Map<Long,String> list=loadList();
		if(local){
			JSONArray all=d.allRaw();Set<Long> ids=new HashSet<>();Map<Long,String> nl=new LinkedHashMap<>();
			for(int i=0;i<all.length();i++){
				JSONObject r=all.getJSONObject(i);long id=r.getLong("id");
				JSONArray oi=r.has("imgs")?r.getJSONArray("imgs"):null;
				JSONArray of=r.has("files")?r.getJSONArray("files"):null;
				r=packUpload(r,oi,of);nl.put(id,putRecord(id,r));ids.add(id);
				d.updateLinks(id,r.optString("imgs","[]"),r.optString("files","[]"));
			}
			for(Map.Entry<Long,String> e:list.entrySet()){if(!ids.contains(e.getKey()))delRemote(e.getValue());}
			saveList(nl);return new JSONObject().put("ok",true);
		}else{
			d.clear(null);int c=0;
			for(Map.Entry<Long,String> e:list.entrySet()){try{JSONObject r=new JSONObject(J.dec(req(X+"/"+e.getValue(),"GET",null)));r.put("id",e.getKey());d.save(r,null);c++;}catch(Exception ex){}}
			return new JSONObject().put("ok",true).put("count",c);
		}
	}

	public void upRecord(long id,JSONObject r,JSONArray oldImgs,JSONArray oldFiles)throws Exception{
		r=packUpload(r,oldImgs,oldFiles);
		String fn=putRecord(id,r);Map<Long,String> list=loadList();list.put(id,fn);saveList(list);
	}

	public void delRecord(long id)throws Exception{Map<Long,String> list=loadList();String fn=list.remove(id);if(fn!=null)delRemote(fn);saveList(list);}

	public void delFile(String url)throws Exception{req(url.replace(U,""),"DELETE",null);}

	public void clear()throws Exception{Map<Long,String> list=loadList();for(String fn:list.values())delRemote(fn);saveList(new LinkedHashMap<>());}
}