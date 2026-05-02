const E=cordova.require('cordova/exec'),P=(a,b)=>new Promise((o,x)=>E(o,x,'journiv',a,b||[]));
module.exports={
	sync:_=>P('sync',[_]),
	save:(_,i,s)=>P('save',[_,i,s]),
	remove:(_,s)=>P('remove',[_,s]),
	page:(_,p,s)=>P('page',[_||{},p||1,s||20]),
	one:_=>P('one',[_]),
	multi:_=>P('multi',[_]),
	memory:()=>P('memory'),
	clear:_=>P('clear',[_||0]),
	export:(_,f)=>P('export',[_,f]),
	summary:()=>P('summary'),
	config:_=>P('config',[_])
};