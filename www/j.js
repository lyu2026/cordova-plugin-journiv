const E=cordova.require('cordova/exec')
const P=(a,b)=>new Promise((o,x)=>E(o,x,'journiv',a,b||[]))
module.exports={
	save:(_,i,s)=>P('save',[_,i,s]),
	remove:(_,s)=>P('remove',[_,s]),
	page:(_,p,z)=>P('page',[_||{},p||1,z||20]),
	one:_=>P('one',[_]),
	memory:()=>P('memory'),
	sync:_=>P('sync',[_]),
	clear:_=>P('clear',[_||0]),
	export:(_,f)=>P('export',[_,f]),
	summary:()=>P('summary'),
	config:_=>P('config',[_])
}