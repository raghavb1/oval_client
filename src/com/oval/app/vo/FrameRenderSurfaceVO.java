package com.oval.app.vo;

import java.nio.ByteBuffer;

import com.oval.app.activities.RTCActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FrameRenderSurfaceVO extends SurfaceView {

	private SurfaceHolder surfaceHolder;
	private MyThread myThread;

	RTCActivity mainActivity;

	long timeStart;
	long timeA;
	long timeB;
	long timeFillBackground;
	long timeDrawBitmap;
	long timeTotal;

	long numberOfPt;

	public FrameRenderSurfaceVO(Context context) {
		super(context);
		init(context);
	}

	public FrameRenderSurfaceVO(Context context, 
			AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public FrameRenderSurfaceVO(Context context, 
			AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context c){
		mainActivity = (RTCActivity)c;
		numberOfPt = 0;
		myThread = new MyThread(this);

		surfaceHolder = getHolder();


		surfaceHolder.addCallback(new SurfaceHolder.Callback(){

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if(!myThread.running){
					myThread.setRunning(true);
					myThread.start();
				}
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, 
					int format, int width, int height) {
				// TODO Auto-generated method stub

			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
//				Canvas canvas = getHolder().lockCanvas();
//				drawSomething(canvas);
			}});
	}



	protected void drawSomething(Canvas canvas) {

		//		numberOfPt += 500;
		//		if(numberOfPt > (long)((getWidth()*getHeight()))){
		//			numberOfPt = 0;
		//		}
		//
		//		timeStart = System.currentTimeMillis();
		//		Bitmap bmDummy = prepareBitmap_A(getWidth(), getHeight(), numberOfPt);
		//
		//		canvas.drawColor(Color.BLACK);
		//		timeFillBackground = System.currentTimeMillis();
		//		canvas.drawBitmap(bmDummy, 
		//				0, 0, null);
		//		timeDrawBitmap = System.currentTimeMillis();
		if(mainActivity.piex!=null && mainActivity.piex.length != 1){
//			Bitmap bm = Bitmap.createBitmap(360, 640, Bitmap.Config.RGB_565);
//			ByteBuffer buffer = ByteBuffer.wrap(mainActivity.piex);
//			bm.copyPixelsFromBuffer(buffer);
			Bitmap bm = BitmapFactory.decodeByteArray(mainActivity.piex, 0, mainActivity.piex.length);
			
			Rect dest = new Rect(0, 0, getWidth(), getHeight());
			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawColor(Color.BLACK);
			canvas.drawBitmap(bm,null,dest,paint);
		}

		//		mainActivity.runOnUiThread(new Runnable() {
		//
		//			@Override
		//			public void run() {
		//				mainActivity.showDur(
		//						timeA - timeStart,
		//						timeB - timeA,
		//						timeFillBackground - timeB,
		//						timeDrawBitmap - timeFillBackground,
		//						timeDrawBitmap - timeStart);
		//			}
		//		});
	}

	public class MyThread extends Thread {

		FrameRenderSurfaceVO myView;
		private boolean running = false;

		public MyThread(FrameRenderSurfaceVO view) {
			myView = view;
		}

		public void setRunning(boolean run) {
			running = run;    
		}

		@Override
		public void run() {
			while(running){

				Canvas canvas = myView.getHolder().lockCanvas();

				if(canvas != null){
					synchronized (myView.getHolder()) {
						myView.drawSomething(canvas);
					}
					myView.getHolder().unlockCanvasAndPost(canvas);
				}

				try {
					sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	}
}




