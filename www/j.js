const E=cordova.require('cordova/exec'),P=(a,b)=>new Promise((o,x)=>E(o,x,'journiv',a,b||[]));
module.exports={
	save:(_,s)=>P('save',[_,s]),
	remove:(_,s)=>P('remove',[_,s]),
	page:(_,p,s)=>P('page',[_||{},p||1,s||20]),
	one:_=>P('one',[_]),
	multi:_=>P('multi',[_]),
	memory:()=>P('memory'),
	sync:_=>P('sync',[_]),
	clear:_=>P('clear',[_||0]),
	export:(_,t)=>P('export',[_,t]),
	summary:()=>P('summary'),
	config:_=>P('config',[_])
};