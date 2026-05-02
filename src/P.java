package com.j.plugin;

import java.text.SimpleDateFormat;
import android.content.Context;
import android.os.Environment;
import org.json.*;
import java.io.*;
import java.util.*;

public class P{
	P(Context c){}

	String exp(long[] r,String f,J.D d)throws Exception{
		JSONArray a=d.page(null,1,100000).getJSONArray("data");
		List<JSONObject> ls=new ArrayList<>();
		for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);long at=Long.parseLong(o.optString("at","0"));if(at>=r[0]&&at<=r[1])ls.add(o);}
		String fn=r[0]+"-"+r[1],di=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
		if("json".equals(f)){String p=di+"/"+fn+".json";try(FileWriter w=new FileWriter(p)){w.write(ls.toString());}return p;}
		else{String p=di+"/"+fn+".pdf";android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();android.graphics.Paint tp=new android.graphics.Paint();tp.setTextSize(20);tp.setFakeBoldText(true);android.graphics.Paint cp=new android.graphics.Paint();cp.setTextSize(12);android.graphics.pdf.PdfDocument.PageInfo pi=new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create();android.graphics.pdf.PdfDocument.Page pg=pdf.startPage(pi);android.graphics.Canvas c=pg.getCanvas();int y=40;for(JSONObject o:ls){if(y>750){pdf.finishPage(pg);pi=new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create();pg=pdf.startPage(pi);c=pg.getCanvas();y=40;}c.drawText(o.optString("title"),40,y,tp);y+=25;String tx=o.optString("content");int si=0;while(si<tx.length()){int ei=Math.min(si+60,tx.length());c.drawText(tx,si,ei,40,y,cp);y+=18;si=ei;}y+=15;}pdf.finishPage(pg);FileOutputStream os=new FileOutputStream(new File(p));pdf.writeTo(os);os.close();pdf.close();return p;}
	}
}