// PDF导出器 - 使用Android原生API生成PDF
package com.journiv.plugin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import com.journiv.plugin.models.Entry;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfExport{
	private Context ctx;

	public PdfExport(Context ctx){
		this.ctx=ctx;
	}

	// 导出日记到PDF
	public String export(String path,String start,String end) throws Exception{
		// 设置默认路径
		if(path==null||path.isEmpty()){
			File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
			if(!dir.exists()) dir.mkdirs();
			String name="journiv_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault())
					.format(new Date())+".pdf";
			path=new File(dir,name).getAbsolutePath();
		}

		// 查询日记
		DBHelper db=new DBHelper(ctx);
		String where="at >= ? AND at <= ?";
		List<Entry> list=db.query(where,new String[]{start,end},true);

		// 创建PDF文档
		PdfDocument doc=new PdfDocument();

		// A4页面大小
		int pw=595;
		int ph=842;

		// 画笔
		Paint titlePaint=new Paint();
		titlePaint.setTextSize(20);
		titlePaint.setFakeBoldText(true);
		titlePaint.setColor(0xFF333333);

		Paint contentPaint=new Paint();
		contentPaint.setTextSize(12);
		contentPaint.setColor(0xFF333333);

		Paint smallPaint=new Paint();
		smallPaint.setTextSize(10);
		smallPaint.setColor(0xFF666666);

		Paint linePaint=new Paint();
		linePaint.setColor(0xFFCCCCCC);
		linePaint.setStrokeWidth(1);

		// 开始第一页
		PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(pw,ph,1).create();
		PdfDocument.Page page=doc.startPage(pi);
		Canvas canvas=page.getCanvas();

		int y=40;

		// 标题
		canvas.drawText("Journiv 日记导出",40,y,titlePaint);
		y+=30;

		// 导出时间
		String time="导出时间: "+new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date());
		canvas.drawText(time,40,y,smallPaint);
		y+=20;

		// 分隔线
		canvas.drawLine(40,y,pw-40,y,linePaint);
		y+=15;

		// 逐条输出日记
		for(Entry e:list){
			// 检查换页
			if(y>ph-120){
				doc.finishPage(page);
				pi=new PdfDocument.PageInfo.Builder(pw,ph,1).create();
				page=doc.startPage(pi);
				canvas=page.getCanvas();
				y=40;
			}

			// 日期和情绪
			String dateStr="";
			try{
				long ts=Long.parseLong(e.created);
				dateStr=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(ts));
			}catch(Exception ex){}
			canvas.drawText(dateStr+"  |  "+e.mood,40,y,smallPaint);
			y+=20;

			// 标题
			canvas.drawText(e.title,40,y,titlePaint);
			y+=25;

			// 标签
			if(e.tags!=null&&!e.tags.isEmpty()){
				canvas.drawText("标签: "+e.tags,40,y,smallPaint);
				y+=18;
			}

			// 正文 - 自动换行
			String content=e.content;
			float maxWidth=pw-80;
			int start=0;
			while(start<content.length()){
				int end=start;
				while(end<content.length()){
					float w=contentPaint.measureText(content,start,end+1);
					if(w>maxWidth) break;
					end++;
				}
				if(end==start) end=start+1;
				canvas.drawText(content,start,end,40,y,contentPaint);
				y+=18;
				start=end;
			}

			y+=10;
			canvas.drawLine(40,y,pw-40,y,linePaint);
			y+=15;
		}

		doc.finishPage(page);

		// 写入文件
		File file=new File(path);
		FileOutputStream fos=new FileOutputStream(file);
		doc.writeTo(fos);
		fos.close();
		doc.close();

		return path;
	}
}