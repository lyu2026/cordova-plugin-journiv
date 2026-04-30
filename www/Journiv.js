const E=require('cordova/exec')
const J={
	// 日记 CRUD
	save:function(title,content,mood,tags,ok,err){E(ok,err,'Journiv','save',[title,content,mood,tags])},
	get:function(id,ok,err){E(ok,err,'Journiv','get',[id])},
	all:function(ok,err){E(ok,err,'Journiv','all',[])},
	update:function(id,title,content,mood,tags,ok,err){E(ok,err,'Journiv','update',[id,title,content,mood,tags])},
	remove:function(id,ok,err){E(ok,err,'Journiv','remove',[id])},

	// 那年今日
	memory:function(ok,err){E(ok,err,'Journiv','memory',[])},
	// 按情绪筛选
	byMood:function(mood,ok,err){E(ok,err,'Journiv','byMood',[mood])},
	// 随机写作提示
	prompt:function(ok,err){E(ok,err,'Journiv','prompt',[])},

	// 应用锁
	setPass:function(pwd,ok,err){E(ok,err,'Journiv','setPass',[pwd])},
	checkPass:function(pwd,ok,err){E(ok,err,'Journiv','checkPass',[pwd])},

	// 图片管理
	addImg:function(diaryId,b64,ok,err){E(ok,err,'Journiv','addImg',[diaryId,b64])},
	getImgs:function(diaryId,ok,err){E(ok,err,'Journiv','getImgs',[diaryId])},

	// 搜索
	search:function(q,ok,err){E(ok,err,'Journiv','search',[q])},
	advSearch:function(kw,start,end,mood,tags,ok,err){E(ok,err,'Journiv','advSearch',[kw,start,end,mood,tags])},

	// WebDAV 同步
	syncSetup:function(url,user,pass,folder,ok,err){E(ok,err,'Journiv','syncSetup',[url,user,pass,folder||'journiv'])},
	syncUp:function(ok,err){E(ok,err,'Journiv','syncUp',[])},
	syncDown:function(ok,err){E(ok,err,'Journiv','syncDown',[])},
	syncStatus:function(ok,err){E(ok,err,'Journiv','syncStatus',[])},

	// 提醒
	setRemind:function(id,hour,min,days,on,type,ok,err){E(ok,err,'Journiv','setRemind',[id,hour,min,days,on,type])},
	cancelRemind:function(id,ok,err){E(ok,err,'Journiv','cancelRemind',[id])},
	allReminds:function(ok,err){E(ok,err,'Journiv','allReminds',[])},

	// 统计
	stats:function(start,end,ok,err){E(ok,err,'Journiv','stats',[start,end])},
	moodChart:function(days,ok,err){E(ok,err,'Journiv','moodChart',[days||30])},
	streak:function(ok,err){E(ok,err,'Journiv','streak',[])},

	// PDF 导出
	exportPdf:function(path,start,end,ok,err){E(ok,err,'Journiv','exportPdf',[path,start,end])}
};

module.exports=J