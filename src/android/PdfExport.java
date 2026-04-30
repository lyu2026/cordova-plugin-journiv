// PDF导出器 - 将日记导出为PDF文件
package com.journiv.plugin;

import android.content.Context;
import android.os.Environment;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.journiv.plugin.models.Entry;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

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
			String name="journiv_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date())+".pdf";
			path=new File(dir,name).getAbsolutePath();
		}
		
		// 查询日记
		DBHelper db=new DBHelper(ctx);
		String where="at >= ? AND at <= ?";
		List<Entry> list=db.query(where,new String[]{start,end},true);
		
		// 中文字体
		com.itextpdf.io.font.PdfFont font;
		try{
			// 尝试从assets加载中文字体
			font=com.itextpdf.io.font.PdfFontFactory.createFont("STSong-Light","UniGB-UCS2-H",com.itextpdf.io.font.PdfEncodings.IDENTITY_H);
		}catch(Exception e){
			// 降级使用内置字体
			font=com.itextpdf.io.font.PdfFontFactory.createFont();
		}
		
		// 创建PDF
		PdfWriter writer=new PdfWriter(path);
		PdfDocument pdf=new PdfDocument(writer);
		Document doc=new Document(pdf);
		
		// 标题
		Paragraph title=new Paragraph("Journiv 日记导出")
				.setFont(font)
				.setFontSize(20)
				.setTextAlignment(TextAlignment.CENTER);
		doc.add(title);
		
		// 导出日期
		Paragraph date=new Paragraph("导出时间: "+new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date())).setFont(font).setFontSize(10);
		doc.add(date);
		
		// 分隔线
		doc.add(new Paragraph("─".repeat(50)).setFont(font));
		
		// 逐条输出日记
		for(Entry e:list){
			// 日记标题和日期
			String time="";
			try{
				long ts=Long.parseLong(e.created);
				time=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault())
						.format(new Date(ts));
			}catch(Exception ex){}
			
			doc.add(new Paragraph("\n"+time+"  |  "+e.mood).setFont(font).setFontSize(10).setBold());
			doc.add(new Paragraph(e.title).setFont(font).setFontSize(16).setBold());
			
			// 标签
			if(e.tags!=null&&!e.tags.isEmpty()){
				doc.add(new Paragraph("标签: "+e.tags).setFont(font).setFontSize(10));
			}
			
			// 正文
			doc.add(new Paragraph("\n"+e.content+"\n").setFont(font).setFontSize(12));
			
			// 分隔线
			doc.add(new Paragraph("─".repeat(50)).setFont(font).setFontSize(8));
		}
		
		doc.close();
		return path;
	}
}