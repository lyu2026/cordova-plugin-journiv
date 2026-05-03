package com.koofr.plugin;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import org.apache.cordova.*;
import java.net.URLEncoder;
import android.util.Base64;
import javax.net.ssl.*;
import org.json.*;
import okhttp3.*;

public class O extends CordovaPlugin{
	private static final String H="https://app.koofr.net/api/v2";
	private String U="lyuw2026@gmail.com",P="vy75aqa4naicde1r",M;
	private OkHttpClient Z;

	@Override
	protected void pluginInitialize(){
		try{
			SSLSocketFactory f=tls();
			if(f==null)throw new Exception("SSL初始化失败");
			Z=new OkHttpClient.Builder()
				.connectTimeout(10,TimeUnit.SECONDS)
				.sslSocketFactory(f,new X509TrustManager(){
					public void checkClientTrusted(X509Certificate[] c,String a){}
					public void checkServerTrusted(X509Certificate[] c,String a){}
					public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
				}).hostnameVerifier(new HostnameVerifier(){
					public boolean verify(String h,SSLSession s){return true;}
				}).build();
		}catch(Exception e){
			android.util.Log.e("Koofr","插件初始化失败",e);
		}
	}

	private SSLSocketFactory tls(){
		try{
			SSLContext s=SSLContext.getInstance("TLS");
			s.init(null,new TrustManager[]{new X509TrustManager(){
				public void checkClientTrusted(X509Certificate[] c,String a){}
				public void checkServerTrusted(X509Certificate[] c,String a){}
				public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
			}},new java.security.SecureRandom());
			return s.getSocketFactory();
		}catch(Exception e){return null;}
	}

	@Override
	public boolean execute(String a,JSONArray x,CallbackContext c){
		cordova.getThreadPool().execute(new Runnable(){
			public void run(){
				try{
					switch(a){
						case "up":up(x,c);break;
						case "dn":dn(x,c);break;
						case "rm":rm(x,c);break;
						case "ls":ls(x,c);break;
						case "cr":cr(x,c);break;
						default:c.error("未知操作: "+a);
					}
				}catch(Exception e){c.error(e.getMessage());}
			}
		});
		return true;
	}

	private String call(String x,String m,String b)throws Exception{
		String u=H+x;
		if(x.contains("/files/put?")||x.contains("/files/get/"))u="https://app.koofr.net/content/api/v2"+x;
		Request.Builder r=new Request.Builder().url(u).header("Authorization","Basic "+Base64.encodeToString((U+":"+P).getBytes(),Base64.NO_WRAP));
		if(b!=null&&!b.isEmpty())r.method(m,RequestBody.create(b,MediaType.parse(b.startsWith("{")?"application/json":"application/octet-stream")));
		else r.method(m,null);
		try(Response z=Z.newCall(r.build()).execute()){
			if(!z.isSuccessful())throw new Exception(z.code()+" "+z.message());
			ResponseBody o=z.body();
			return o!=null?o.string():"";
		}
	}

	private String mid()throws Exception{
		if(M==null||M.isEmpty())M=new JSONObject(call("/mounts","GET",null)).getJSONArray("mounts").getJSONObject(0).getString("id");
		return M;
	}

	void up(JSONArray x,CallbackContext c)throws Exception{
		String p=x.optString(0,""),n=x.optString(1,""),b=x.optString(2,"");
		if(n.isEmpty()){
			c.error("文件名不能为空!");
			return;
		}
		if(p.isEmpty())p="/";
		else if(!p.isEmpty())p="/"+p;
		String o=call("/mounts/"+mid()+"/files/put?path="+URLEncoder.encode(p,"UTF-8")+"&filename="+URLEncoder.encode(n,"UTF-8")+"&info=true&overwrite=true","POST",b);
		c.success(new JSONObject(o));
	}

	void dn(JSONArray x,CallbackContext c)throws Exception{
		String p=x.optString(0,"").replaceAll("^/+|/+$",""),n=x.optString(1,"");
		if(n.isEmpty()){
			c.error("文件名不能为空!");
			return;
		}
		if(p.isEmpty())p="/";
		else if(!p.isEmpty())p="/"+p;
		String o=call("/mounts/"+mid()+"/files/get/"+n+"?path="+URLEncoder.encode(p+"/"+n,"UTF-8")+"&force=true","GET",null);
		c.success(o);
	}

	void rm(JSONArray x,CallbackContext c)throws Exception{
		JSONArray s=x.getJSONArray(1),o=new JSONArray();
		String p=x.optString(0,"").replaceAll("^/+|/+$",""),m=mid();
		if(p.isEmpty())p="/";
		else if(!p.isEmpty())p="/"+p;
		for(int i=0;i<s.length();i++){
			String v=s.optString(i,"").replaceAll("^/+|/+$","");
			if(v.isEmpty())continue;
			JSONObject f=new JSONObject();
			f.put("path",p+"/"+v);
			f.put("mountId",m);
			o.put(f);
		}
		JSONObject z=new JSONObject();z.put("files",o);
		c.success(new JSONObject(call("/jobs/files/remove","POST",z.toString())));
	}

	void ls(JSONArray x,CallbackContext c)throws Exception{
		String p=x.optString(0,"").replaceAll("^/+|/+$","");
		if(p.isEmpty())p="/";
		else if(!p.isEmpty())p="/"+p;
		String o=call("/mounts/"+mid()+"/bundle?path="+URLEncoder.encode(p,"UTF-8"),"GET",null);
		c.success(new JSONObject(o).optJSONArray("files"));
	}

	void cr(JSONArray x,CallbackContext c)throws Exception{
		String p=x.optString(0,"").replaceAll("^/+|/+$","");
		if(p.isEmpty())p="/";
		else if(!p.isEmpty())p="/"+p;
		JSONArray xs=new JSONObject(call("/mounts/"+mid()+"/bundle?path="+URLEncoder.encode(p,"UTF-8"),"GET",null)).optJSONArray("files");
		JSONArray s=new JSONArray();
		for(int i=0;i<xs.length();i++)s.put(xs.getJSONObject(i).getString("name"));
		xs=new JSONObject(call("/mounts/"+mid()+"/bundle?path="+URLEncoder.encode(p+"/files","UTF-8"),"GET",null)).optJSONArray("files");
		for(int i=0;i<xs.length();i++)s.put("files/"+xs.getJSONObject(i).getString("name"));
		if(s.length()>0)rm((new JSONArray().put(p)).put(s),c);
		call("/trash","DELETE",null);
		c.success();
	}
}