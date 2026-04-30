// 日记数据模型
package com.journiv.plugin.models;

public class Entry{
	// 数据库字段
	public long id; // 日记ID
	public String title; // 标题
	public String content; // 内容(加密存储)
	public String mood; // 情绪标签
	public String tags; // 标签(逗号分隔)
	public String imgs; // 图片路径(JSON数组)
	public String created; // 创建时间戳
	public String updated; // 更新时间戳

	// 从JSON字符串解析图片列表
	public String[] getImgList(){
		try{
			org.json.JSONArray s=new org.json.JSONArray(imgs);
			String[] o=new String[s.length()];
			for(int i=0;i<s.length();i++)o[i]=s.getString(i);
			return o;
		}catch(Exception e){
			return new String[0];
		}
	}
}