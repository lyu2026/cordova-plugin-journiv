// AES加密工具 - 用于日记内容的加解密
package com.journiv.plugin;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil{
	// 加密算法: AES-256-CBC
	private static final String ALG="AES/CBC/PKCS5Padding";
	// 密钥 - 32字节，必须正好16/24/32字节
	private static final String KEY="JournivJournivJournivJourniv12";
	// 初始化向量 - 16字节
	private static final String IV="J0urn1vInitV3ct0";

	// 加密明文 -> Base64密文
	public static String enc(String text)throws Exception{
		SecretKeySpec ks=new SecretKeySpec(KEY.getBytes("UTF-8"),"AES");
		IvParameterSpec iv=new IvParameterSpec(IV.getBytes("UTF-8"));
		Cipher c=Cipher.getInstance(ALG);
		c.init(Cipher.ENCRYPT_MODE,ks,iv);
		byte[] out=c.doFinal(text.getBytes("UTF-8"));
		return Base64.encodeToString(out,Base64.NO_WRAP);
	}

	// 解密Base64密文 -> 明文
	public static String dec(String text)throws Exception{
		SecretKeySpec ks=new SecretKeySpec(KEY.getBytes("UTF-8"),"AES");
		IvParameterSpec iv=new IvParameterSpec(IV.getBytes("UTF-8"));
		Cipher c=Cipher.getInstance(ALG);
		c.init(Cipher.DECRYPT_MODE,ks,iv);
		byte[] out=c.doFinal(Base64.decode(text,Base64.NO_WRAP));
		return new String(out,"UTF-8");
	}
}