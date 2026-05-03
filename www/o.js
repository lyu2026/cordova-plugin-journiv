const x=require('cordova/exec')
module.exports={
	up:(p,n,o,s,e)=>x(s,e,'Koofr','up',[p,n,o]),
	dn:(p,n,s,e)=>x(s,e,'Koofr','dn',[p,n]),
	rm:(p,x,s,e)=>x(s,e,'Koofr','rm',[p,x]),
	ls:(p,s,e)=>x(s,e,'Koofr','ls',[p]),
	cr:(p,s,e)=>x(s,e,'Koofr','cr',[p])
}