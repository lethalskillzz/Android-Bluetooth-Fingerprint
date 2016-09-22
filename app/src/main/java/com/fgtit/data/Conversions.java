package com.fgtit.data;

import android.util.Base64;

public class Conversions {
	
	private static Conversions mCom=null;
	
	public static Conversions getInstance(){
		if(mCom==null){
			mCom=new Conversions();
		}
		return mCom;
	}
	
	public native int StdToIso(int itype,byte[] input,byte[] output);
	public native int IsoToStd(int itype,byte[] input,byte[] output);
	public native int GetDataType(byte[] input);
	public native int StdChangeCoord(byte[] input,int size,byte[] output,int dk);
	
	public String IsoChangeCoord(byte[] input,int dk){
		int dt=GetDataType(input);
		if(dt==3){
			byte output[] =new byte[512];
			byte stddat[]=new byte[512];
			byte crddat[]=new byte[512];
			IsoToStd(2,input,stddat);
			StdChangeCoord(stddat,256,crddat,dk);
			StdToIso(2,crddat,output);
			return Base64.encodeToString(output,0,378,Base64.DEFAULT);
		}
		return "";
	}
	
	
	static {
		System.loadLibrary("conversions");
	}
}
