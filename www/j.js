const E=cordova.require('cordova/exec'),P=(a,b)=>new Promise((o,x)=>E(o,x,'journiv',a,b||[]));
module.exports={
	init:()=>P('init'),
	save:(d,s)=>P('save',[d,s]),
	remove:(ids,s)=>P('remove',[ids,s]),
	page:(q,pg,sz)=>P('page',[q||{},pg||1,sz||20]),
	one:(id)=>P('one',[id]),
	multi:(ids)=>P('multi',[ids]),
	memory:()=>P('memory'),
	sync:(local)=>P('sync',[local]),
	clear:(t)=>P('clear',[t||0]),
	export:(range,fmt)=>P('export',[range,fmt]),
	summary:()=>P('summary'),
	config:(cfg)=>P('config',[cfg])
};