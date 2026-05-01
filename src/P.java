package com.j.plugin;

import java.text.SimpleDateFormat;
import android.os.Environment;
import org.json.*;
import java.util.*;
import java.io.*;

public class P{
	String export(long[] range,String fmt,J.D d)throws Exception{
		JSONArray a=d.page(null,1,100000).getJSONArray("data");
		List<JSONObject> list=new ArrayList<>();
		for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);long at=Long.parseLong(r.optString("at","0"));if(at>=range[0]&&at<=range[1])list.add(r);}
		String f=range[0]+"-"+range[1],dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
		if("json".equals(fmt)){String p=dir+"/"+f+".json";try(FileWriter w=new FileWriter(p)){w.write(list.toString());}return p;}
		else{String p=dir+"/"+f+".pdf";android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();android.graphics.Paint tp=new android.graphics.Paint();tp.setTextSize(20);tp.setFakeBoldText(true);android.graphics.Paint cp=new android.graphics.Paint();cp.setTextSize(12);android.graphics.pdf.PdfDocument.PageInfo pi=new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create();android.graphics.pdf.PdfDocument.Page pg=pdf.startPage(pi);android.graphics.Canvas c=pg.getCanvas();int y=40;for(JSONObject r:list){if(y>750){pdf.finishPage(pg);pi=new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create();pg=pdf.startPage(pi);c=pg.getCanvas();y=40;}c.drawText(r.optString("title"),40,y,tp);y+=25;String tx=r.optString("content");int si=0;while(si<tx.length()){int ei=Math.min(si+60,tx.length());c.drawText(tx,si,ei,40,y,cp);y+=18;si=ei;}y+=15;}pdf.finishPage(pg);FileOutputStream o=new FileOutputStream(new File(p));pdf.writeTo(o);o.close();pdf.close();return p;}
	}
}