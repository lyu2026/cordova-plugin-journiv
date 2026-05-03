const exec=require('cordova/exec'),koofr={
	/**
	* 上传文件
	* @param {string} path - 路径
	* @param {string} name - 文件名
	* @param {ArrayBuffer} buffer - 文件流
	* @param {function} sc
	* @param {function} ec
	*/
	up:function(path,name,buffer,sc,ec){
		exec(sc,ec,'KoofrPlugin','up',[path,name,buffer]);
	},

	/**
	* 下载文件
	* @param {string} path - 路径
	* @param {string} name - 文件名
	* @param {function} sc
	* @param {function} ec
	*/
	dn:function(path,name,sc,ec){
		exec(sc,ec,'KoofrPlugin','dn',[path,name]);
	},

	/**
	* 删除文件
	* @param {string} path - 路径
	* @param {string[]} files - 文件列表
	* @param {function} sc
	* @param {function} ec
	*/
	rm:function(path,files,sc,ec){
		exec(sc,ec,'KoofrPlugin','rm',[path,files]);
	},

	/**
	* 列出目录文件
	* @param {string} path - 路径
	* @param {function} sc
	* @param {function} ec
	*/
	ls:function(path,sc,ec){
		exec(sc,ec,'KoofrPlugin','ls',[path]);
	}
}
module.exports=koofr