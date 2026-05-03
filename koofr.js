var exec = require('cordova/exec');

var koofr = {
	up:function(p,n,b,s,e){exec(s,e,'KoofrPlugin','up',[p,n,b]);},
	dn:function(p,n,s,e){exec(s,e,'KoofrPlugin','dn',[p,n]);},
	rm:function(p,f,s,e){exec(s,e,'KoofrPlugin','rm',[p,f]);},
	ls:function(p,s,e){exec(s,e,'KoofrPlugin','ls',[p]);}
};

module.exports = koofr;