const E=require('cordova/exec')
const P=(a,...b)=>new Promise((o,x)=>E(o,x,'Journiv',a,b))

const J={
	// 日记 CRUD
	save:(t,c,m,g)=>P('save',t,c,m,g),
	get:i=>P('get',i),
	all:()=>P('all'),
	update:(i,t,c,m,g)=>P('update',i,t,c,m,g),
	remove:i=>P('remove',i),

	// 那年今日
	memory:()=>P('memory'),
	byMood:m=>P('byMood',m),
	prompt:()=>P('prompt'),

	// 应用锁
	setPass:p=>P('setPass',p),
	checkPass:p=>P('checkPass',p),

	// 图片管理
	addImg:(i,b)=>P('addImg',i,b),
	getImgs:i=>P('getImgs',i),

	// 搜索
	search:q=>P('search',q),
	advSearch:(k,s,e,m,t)=>P('advSearch',k,s,e,m,t),

	// WebDAV 同步
	syncUp:()=>P('syncUp'),
	syncDown:()=>P('syncDown'),
	syncStatus:()=>P('syncStatus'),

	// 提醒
	setRemind:(i,h,m,d,o,t)=>P('setRemind',i,h,m,d,o,t),
	cancelRemind:i=>P('cancelRemind',i),
	allReminds:()=>P('allReminds'),

	// 统计
	stats:(s,e)=>P('stats',s,e),
	moodChart:d=>P('moodChart',d||30),
	streak:()=>P('streak'),
	summary:()=>P('summary'),

	// PDF 导出
	exportPdf:(p,s,e)=>P('exportPdf',p,s,e)
};

module.exports=J