// 图片管理器 - 处理图片的存储、压缩和Base64转换
package com.journiv.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.*;
import java.util.UUID;

public class ImgManager{
	private Context ctx;
	private static final String DIR="journiv_imgs"; // 图片存储目录

	public ImgManager(Context ctx){
		this.ctx=ctx;
	}

	// 获取图片目录，不存在则创建
	private File getDir(){
		File d=new File(ctx.getFilesDir(),DIR);
		if(!d.exists()) d.mkdirs();
		return d;
	}

	// 保存Base64图片 -> 返回文件路径
	public String save(long diaryId,String b64)throws IOException{
		// 解码Base64
		byte[] data=Base64.decode(b64,Base64.DEFAULT);
		
		// 解码为Bitmap并压缩
		Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);
		if(bm==null) throw new IOException("图片解码失败");
		Bitmap small=scale(bm,1920); // 最大1920px
		bm.recycle();
		
		// 生成唯一文件名
		String name="img_"+diaryId+"_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,8)+".jpg";
		File f=new File(getDir(),name);
		
		// 写入JPEG
		try(FileOutputStream out=new FileOutputStream(f)){
			small.compress(Bitmap.CompressFormat.JPEG,85,out);
		}
		small.recycle();
		return f.getAbsolutePath();
	}

	// 读取图片转Base64 - 用于返回给前端显示
	public String toBase64(String path)throws IOException{
		File f=new File(path);
		if(!f.exists()) throw new FileNotFoundException("图片不存在: "+path);

		// 读取文件转字节数组
		byte[] data=getBytes(f);

		// 解码并缩放
		Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);
		if(bm==null) throw new IOException("图片解码失败");
		Bitmap small=scale(bm,800); // 前端显示800px足够
		bm.recycle();

		// 转Base64
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		small.compress(Bitmap.CompressFormat.JPEG,80,baos);
		small.recycle();

		return "data:image/jpeg;base64,"+Base64.encodeToString(baos.toByteArray(),Base64.NO_WRAP);
	}

	// 删除日记关联的所有图片
	public void removeByDiary(long diaryId){
		File[] files=getDir().listFiles((dir,name)->name.startsWith("img_"+diaryId+"_"));
		if(files!=null) for(File f:files) f.delete();
	}

	// 获取日记的所有图片路径
	public String[] getByDiary(long diaryId){
		File[] files=getDir().listFiles((dir,name)->name.startsWith("img_"+diaryId+"_"));
		if(files==null) return new String[0];
		String[] paths=new String[files.length];
		for(int i=0;i<files.length;i++) paths[i]=files[i].getAbsolutePath();
		return paths;
	}

	// 缩放Bitmap
	private Bitmap scale(Bitmap src,int max){
		int w=src.getWidth();
		int h=src.getHeight();
		if(w<=max&&h<=max) return src;
		float r=Math.min((float)max/w,(float)max/h);
		return Bitmap.createScaledBitmap(src,Math.round(w*r),Math.round(h*r),true);
	}

	// 读取文件全部字节
	private byte[] getBytes(File f)throws IOException{
		byte[] data=new byte[(int)f.length()];
		try(FileInputStream in=new FileInputStream(f)){
			in.read(data);
		}
		return data;
	}
}