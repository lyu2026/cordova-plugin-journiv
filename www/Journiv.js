const E=require('cordova/exec')
const J={
	// 日记 CRUD
	save:function(title,content,mood,tags,o,x){E(o,x,'Journiv','save',[title,content,mood,tags])},
	get:function(id,o,x){E(o,x,'Journiv','get',[id])},
	all:function(o,x){E(o,x,'Journiv','all',[])},
	update:function(id,title,content,mood,tags,o,x){E(o,x,'Journiv','update',[id,title,content,mood,tags])},
	remove:function(id,o,x){E(o,x,'Journiv','remove',[id])},

	// 那年今日
	memory:function(o,x){E(o,x,'Journiv','memory',[])},
	// 按情绪筛选
	byMood:function(mood,o,x){E(o,x,'Journiv','byMood',[mood])},
	// 随机写作提示
	prompt:function(o,x){E(o,x,'Journiv','prompt',[])},

	// 应用锁
	setPass:function(pwd,o,x){E(o,x,'Journiv','setPass',[pwd])},
	checkPass:function(pwd,o,x){E(o,x,'Journiv','checkPass',[pwd])},

	// 图片管理
	addImg:function(diaryId,b64,o,x){E(o,x,'Journiv','addImg',[diaryId,b64])},
	getImgs:function(diaryId,o,x){E(o,x,'Journiv','getImgs',[diaryId])},

	// 搜索
	search:function(q,o,x){E(o,x,'Journiv','search',[q])},
	advSearch:function(kw,start,end,mood,tags,o,x){E(o,x,'Journiv','advSearch',[kw,start,end,mood,tags])},

	// WebDAV 同步
	syncUp:function(o,x){E(o,x,'Journiv','syncUp',[])},
	syncDown:function(o,x){E(o,x,'Journiv','syncDown',[])},
	syncStatus:function(o,x){E(o,x,'Journiv','syncStatus',[])},

	// 提醒
	setRemind:function(id,hour,min,days,on,type,o,x){E(o,x,'Journiv','setRemind',[id,hour,min,days,on,type])},
	cancelRemind:function(id,o,x){E(o,x,'Journiv','cancelRemind',[id])},
	allReminds:function(o,x){E(o,x,'Journiv','allReminds',[])},

	// 统计
	stats:function(start,end,o,x){E(o,x,'Journiv','stats',[start,end])},
	moodChart:function(days,o,x){E(o,x,'Journiv','moodChart',[days||30])},
	streak:function(o,x){E(o,x,'Journiv','streak',[])},

	// PDF 导出
	exportPdf:function(path,start,end,o,x){E(o,x,'Journiv','exportPdf',[path,start,end])}
};

module.exports=J