package com.j.plugin;

import android.graphics.pdf.PdfDocument;
import java.text.SimpleDateFormat;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Environment;
import android.graphics.Paint;
import org.json.*;
import java.util.*;
import java.io.*;

// 导出管理器 - 将日记导出为JSON或PDF文件
public class X{
	X(Context _){}

	// 将单条记录转为文本块 - 空字段跳过，经纬度同行，情绪标签同行
	private String block(JSONObject _){
		StringBuilder o=new StringBuilder();
		SimpleDateFormat f=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.getDefault());
		f.setTimeZone(J.Z);

		String t=_.optString("title");
		if(t!=null&&!t.isEmpty())o.append("标题：").append(t).append("\n");
		String c=_.optString("content");
		if(c!=null&&!c.isEmpty())o.append("内容：").append(c).append("\n");
		String m=_.optString("mood"),g=_.optString("tags");
		if((m!=null&&!m.isEmpty())||(g!=null&&!g.isEmpty())){
			o.append("情绪：").append(m!=null?m:"").append("  标签：").append(g!=null?g:"").append("\n");
		}
		JSONArray sm=_.optJSONArray("imgs");
		if(sm!=null&&sm.length()>0)o.append("图片：").append(sm.toString()).append("\n");
		JSONArray sf=_.optJSONArray("files");
		if(sf!=null&&sf.length()>0)o.append("附件：").append(sf.toString()).append("\n");
		double lt=_.optDouble("lat",Double.NaN),ln=_.optDouble("lng",Double.NaN);
		if(!Double.isNaN(lt)||!Double.isNaN(ln))o.append("经纬度：").append(lt).append(", ").append(ln).append("\n");
		String a=_.optString("addr");
		if(a!=null&&!a.isEmpty())o.append("地址：").append(a).append("\n");
		try{
			long ts=Long.parseLong(_.optString("at","0"));
			o.append("时间：").append(f.format(new Date(ts)));
		}catch(Exception e){o.append("时间：").append(_.optString("at"));}
		return o.toString();
	}

	// [X.export] 导出 - _=[开始时间戳,结束时间戳](-1表示不限) f=格式(json/pdf)
	String export(long[] _,String f,J.D _d)throws Exception{
		JSONArray a=_d.trange(_[0],_[1]);
		String n=(_[0]==-1?"0":String.valueOf(_[0]))+"-"+(_[1]==-1?String.valueOf(System.currentTimeMillis()):String.valueOf(_[1]));
		String dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
		String o="";
		if("json".equals(f)){
			o=dir+"/"+n+".json";
			try(FileWriter w=new FileWriter(o)){w.write(a.toString());}
			return o;
		}
		o=dir+"/"+n+".pdf";
		PdfDocument pdf=new PdfDocument();
		Paint tp=new Paint();tp.setTextSize(16);tp.setFakeBoldText(true);
		Paint cp=new Paint();cp.setTextSize(11);
		PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(595,842,1).create();
		PdfDocument.Page pg=pdf.startPage(pi);
		Canvas c=pg.getCanvas();int y=40;
		for(int i=0;i<a.length();i++){
			String b=block(a.getJSONObject(i));
			String[] s=b.split("\n");
			for(String line:s){
				if(y>800){
					pdf.finishPage(pg);
					pi=new PdfDocument.PageInfo.Builder(595,842,1).create();
					pg=pdf.startPage(pi);
					c=pg.getCanvas();y=40;
				}
				if(line.startsWith("标题："))c.drawText(line,40,y,tp);
				else c.drawText(line,40,y,cp);
				y+=18;
			}
			y+=10; // 块间隔
		}
		pdf.finishPage(pg);
		FileOutputStream z=new FileOutputStream(new File(o));
		pdf.writeTo(z);z.close();pdf.close();
		return o;
	}
}