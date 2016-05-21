package org.mitre.svmp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.mitre.svmp.protocol.SVMPProtocol;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class StreamHandler {
	public Context context;

	private static final String TAG = "Stream Handler";
	public ImageView imageView;
	private boolean isProcessing = false;
	private int counter = 0;
	private long myTime = System.currentTimeMillis();
	

	public StreamHandler(Context context) {
		this.context = context;
	}

	//	int[] toIntArray(List<Integer> list){
	//		  int[] ret = new int[list.size()];
	//		  for(int i = 0;i < ret.length;i++)
	//		    ret[i] = list.get(i);
	//		  return ret;
	//		}

	public void handleScreenInfoResponse(SVMPProtocol.Response msg) throws IOException, DataFormatException {
		System.out.println("Receive time" + System.currentTimeMillis());
		counter ++;
		if(System.currentTimeMillis() - myTime > 1000){
			
			System.out.println("***********************frames in 1 sec :" + counter + "*************");
			myTime = System.currentTimeMillis();
			counter = 0;
		}
		if(!isProcessing){
			isProcessing = true;
			byte[] piex = msg.getStream().getFrameBytes().toByteArray();
//			System.out.println(piex.length);

			byte [] uncompressed = decompress(piex);

//			System.out.println(uncompressed.length);
//			System.out.println(System.currentTimeMillis());

			final Bitmap bm = Bitmap.createBitmap(360, 640, Bitmap.Config.RGB_565);

			ByteBuffer buffer = ByteBuffer.wrap(uncompressed);

			bm.copyPixelsFromBuffer(buffer);
			
			((Activity)context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					imageView.setImageBitmap(bm);
					System.out.println("Render Time "+System.currentTimeMillis());
				}});
			isProcessing = false;
		}else{
			System.out.println("Ignoring as in process");
		}
	}

	public static byte[] decompress(byte[] data) throws IOException, DataFormatException {  
		Inflater inflater = new Inflater();   
		inflater.setInput(data);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();
		inflater.end();
		byte[] output = outputStream.toByteArray();  
		//		   LOG.debug("Original: " + data.length);  
		//		   LOG.debug("Compressed: " + output.length);  
		return output;  
	}  
}
