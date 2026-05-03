package com.koofr.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import com.silkimen.http.HttpRequest;
import java.net.URLEncoder;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Base64;

public class KoofrPlugin extends CordovaPlugin{
	private static final String K="https://app.koofr.net/content/api/v2";
	private static final String H="https://app.koofr.net/api/v2";
	private static final String U="lyuw2026@gmail.com";
	private static final String P="vy75aqa4naicde1r";
	private String M;
	private volatile boolean R;

	@Override
	protected void pluginInitialize(){
		go(new Task(){public void run()throws Exception{
			M=mid();
			R=true;
		}});
	}

	@Override
	public boolean execute(String a,JSONArray x,CallbackContext z){
		if("up".equals(a)){run(z,()->up(x,z),()->!R);return true;}
		if("dn".equals(a)){run(z,()->dn(x,z),()->!R);return true;}
		if("rm".equals(a)){run(z,()->rm(x,z),()->!R);return true;}
		if("ls".equals(a)){run(z,()->ls(x,z),()->!R);return true;}
		return false;
	}

	private interface Task{void run()throws Exception;}

	private void go(Task t){cordova.getThreadPool().execute(()->{try{t.run();}catch(Exception e){}});}

	private void run(CallbackContext z,Task t,Check c){
		cordova.getThreadPool().execute(()->{
			try{
				if(c.ok()){waitInit();}
				t.run();
			}catch(Exception e){z.error(e.getMessage());}
		});
	}

	private interface Check{boolean ok();}

	private synchronized void waitInit()throws Exception{
		while(!R){try{wait(100);}catch(InterruptedException e){throw new Exception("初始化中断");}}
	}

	private String mid()throws Exception{
		return new JSONObject(req("/mounts","GET",null,null)).getJSONArray("mounts").getJSONObject(0).getString("id");
	}

	private String req(String p,String m,byte[] b,String t)throws Exception{
		String u=(p.contains("/files/put?")||p.contains("/files/get/")?K:H)+p;
		HttpRequest r="GET".equals(m)?HttpRequest.get(u):HttpRequest.post(u);
		r.header("Authorization","Basic "+Base64.encodeToString((U+":"+P).getBytes(),Base64.NO_WRAP));
		r.connectTimeout(30000);
		r.readTimeout(30000);
		if(b!=null&&b.length>0){r.contentType(t!=null?t:"application/octet-stream");r.send(b);}
		if(r.ok())return r.body();
		throw new Exception(r.code()+" "+r.message());
	}

	private byte[] arr(JSONArray x,int i)throws Exception{
		if(x.isNull(i))return null;
		JSONArray a=x.getJSONArray(i);
		byte[] b=new byte[a.length()];
		for(int j=0;j<a.length();j++)b[j]=(byte)(a.getInt(j)&0xFF);
		return b;
	}

	private String fp(String p){return p.isEmpty()||p.startsWith("/")?p:"/"+p;}

	private String fc(String p){return p.replaceAll("^/+|/+$","");}

	private void up(JSONArray x,CallbackContext z)throws Exception{
		String p=x.optString(0,""),n=x.optString(1,"");
		byte[] b=arr(x,2);
		if(n.isEmpty()){z.error("文件名不能为空!");return;}
		p=fp(p);
		String o=req("/mounts/"+M+"/files/put?path="+URLEncoder.encode(p,"UTF-8")+"&filename="+URLEncoder.encode(n,"UTF-8")+"&info=true&overwrite=true","POST",b,"application/octet-stream");
		z.success(new JSONObject(o));
	}

	private void dn(JSONArray x,CallbackContext z)throws Exception{
		String p=fc(x.optString(0,"")),n=x.optString(1,"");
		if(n.isEmpty()){z.error("文件名不能为空!");return;}
		p=fp(p);
		String o=req("/mounts/"+M+"/files/get/"+URLEncoder.encode(n,"UTF-8")+"?path="+URLEncoder.encode(p+"/"+n,"UTF-8")+"&force=true","GET",null,null);
		z.success(o);
	}

	private void rm(JSONArray x,CallbackContext z)throws Exception{
		String p=fp(fc(x.optString(0,"")));
		JSONArray s=x.getJSONArray(1),o=new JSONArray();
		for(int i=0;i<s.length();i++){
			String v=fc(s.optString(i,""));
			if(v.isEmpty())continue;
			JSONObject f=new JSONObject();
			f.put("path",p+"/"+v);
			f.put("mountId",M);
			o.put(f);
		}
		JSONObject r=new JSONObject();r.put("files",o);
		z.success(new JSONObject(req("/jobs/files/remove","POST",r.toString().getBytes(),"application/json")));
	}

	private void ls(JSONArray x,CallbackContext z)throws Exception{
		String p=fp(fc(x.optString(0,"")));
		z.success(new JSONObject(req("/mounts/"+M+"/files/list?path="+URLEncoder.encode(p,"UTF-8"),"GET",null,null)));
	}
}