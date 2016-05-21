package com.oval.app.activities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

//import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.mitre.svmp.activities.AppRTCActivity;
import org.mitre.svmp.client.ConfigHandler;
import org.mitre.svmp.client.KeyHandler;
import org.mitre.svmp.client.RotationHandler;
import org.mitre.svmp.client.TouchHandler;
import org.mitre.svmp.performance.PerformanceAdapter;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.AppsRequest;
import org.mitre.svmp.protocol.SVMPProtocol.Ping;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;

import com.citicrowd.oval.R;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.IntentCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RTCActivity  extends AppRTCActivity{



	protected PerformanceAdapter performanceAdapter;

	private static final String TAG = RTCActivity.class.getName();

	private TouchHandler touchHandler;
	private RotationHandler rotationHandler;
	private ConfigHandler configHandler;
	private KeyHandler keyHandler;
//	private View tutorialView;

	private String pkgName;
	private String apkPath;
	public byte[] piex = new byte[1];
	private boolean localFrameRender = false;
	private String tag = "maxFrames";
	private int switchTime = 2000;
	int currentQuality = 70;

	private int frameCounter=0;
	//	private Queue<byte[]> frameFifo = new CircularFifoQueue<byte[]>(10);
	private Map<Integer, byte[]> queue = new HashMap<Integer, byte[]>();

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		//super.connectToRoom();
		setContentView(R.layout.activity_splash);
		// Get info passed to Intent
		final Intent intent = getIntent();

//		tutorialView = findViewById(R.id.tutorialView);
//		tutorialView.setVisibility(View.GONE);
		
		pkgName = intent.getStringExtra("pkgName");
		apkPath = intent.getStringExtra("apkPath");

	}

	@Override
	protected void connectToRoom() {
		Point deviceDisplaySize = new Point();
		// displaySize.set(720, 1280);
		getWindowManager().getDefaultDisplay().getSize(deviceDisplaySize);

		//		Point displaySize = new Point();
		//		displaySize.set(deviceDisplaySize.x, deviceDisplaySize.y);

		touchHandler = new TouchHandler(this, deviceDisplaySize, performanceAdapter);
		rotationHandler = new RotationHandler(this);
		keyHandler = new KeyHandler(this);
		configHandler = new ConfigHandler(this);

		super.connectToRoom();
		// package AppsRequest into a Request and send it

	}


	@Override
	public void onOpen() {
		super.onOpen();
		sendTimezoneMessage();
		touchHandler.sendScreenInfoMessage();
		//rotationHandler.initRotationUpdates();
		sendConfigMessage();
		sendAppsMessage();
		//		makePingRequest();
		startThreadInternal(5000);
	}

	private void sendTimezoneMessage(){
		Request.Builder request = Request.newBuilder();
		request.setType(Request.RequestType.TIMEZONE);
		request.setTimezoneId(TimeZone.getDefault().getID());
		sendMessage(request.build());
	}
	private void sendConfigMessage(){
		// send the initial configuration to the VM
		Configuration config = getResources().getConfiguration();
		configHandler.handleConfiguration(config);
	}
	private void sendAppsMessage() {

		AppsRequest.Builder aBuilder = AppsRequest.newBuilder();

		aBuilder.setType(AppsRequest.AppsRequestType.LAUNCH);
		// if we've been given a package name, start that app
		aBuilder.setPkgName(pkgName);
		aBuilder.setApkPath(apkPath);
		Request.Builder rBuilder = Request.newBuilder();

		rBuilder.setType(Request.RequestType.APPS);

		rBuilder.setApps(aBuilder);
		sendMessage(rBuilder.build());

	}


	private void makePingRequest() {
		Ping.Builder pBuilder = Ping.newBuilder();
		pBuilder.setStartDate(System.currentTimeMillis());

		sendMessage(Request.newBuilder()
				.setType(Request.RequestType.PING)
				.setPingRequest(pBuilder)
				.build());
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// send the updated configuration to the VM (i.e. whether or not a
		// hardware keyboard is plugged in)
		configHandler.handleConfiguration(newConfig);
	}

	@Override
	protected void onDisconnectAndExit() {
		if (rotationHandler != null)
			rotationHandler.cleanupRotationUpdates();
	}

	public boolean onMessage(Response data) {
		switch (data.getType()) {
		case STREAM:

			try {
				handleScreenInfoResponse(data);
			} catch (Exception e){
				
			}

			break;
		case SCREENINFO:

			handleScreenInfo(data);
			break;
		case APPS:
			if (data.hasApps() && data.getApps().getType() == SVMPProtocol.AppsResponse.AppsResponseType.EXIT) {
				// we have exited a remote app; exit back to our parent activity
				// and act accordingly
				disconnectAndExit();
			}else if(data.getApps().getType() == SVMPProtocol.AppsResponse.AppsResponseType.LAUNCH){
				startRemoteAppUI();
			}
			break;
		case PING:
			int pingTime = (int)(System.currentTimeMillis() - data.getPingResponse().getStartDate());
			int tempQuality = qualityCalculator(pingTime);
			TextView ll = (TextView) findViewById(R.id.pingTime);
			ll.setText(Integer.toString(pingTime));
			
			System.out.println(pingTime);
			System.out.println(tempQuality);
			if(tempQuality != currentQuality){
				currentQuality = tempQuality;
				sendRTCMessage(tempQuality);
			}
			break;
		default:
			// any messages we don't understand, pass to our parent for
			// processing
			super.onMessage(data);
		}
		return true;
	}
	private void startRemoteAppUI(){
		setContentView(R.layout.activity_rtc);
		
		Point deviceDisplaySize = new Point();
		// displaySize.set(720, 1280);
		getWindowManager().getDefaultDisplay().getSize(deviceDisplaySize);
		
		LinearLayout ll = (LinearLayout) findViewById(R.id.vsvLinear);
		((ViewGroup) ll.getParent()).removeView(ll);
		
		ll.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

			}
		});
		addContentView(ll,
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50*deviceDisplaySize.y / 1280));
	
		ImageView homeStreamingBtn = (ImageView) findViewById(R.id.homeStreamingBtn);
		homeStreamingBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				Intent intent = new Intent(RTCActivity.this, OvalDrawerActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
				//disconnectAndExit();

			}
		});
	
	}
	
	private void createTopPanel() {
		// TODO Auto-generated method stub

		
	}
	private void handleScreenInfo(Response msg) {
		touchHandler.handleScreenInfoResponse(msg);
	}

	private void sendRTCMessage(long currentQuality){
		SVMPProtocol.RTCMessage.Builder rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();
		rtcBuilder.setQuality((int)currentQuality);
		rtcBuilder.setFormat(Bitmap.CompressFormat.WEBP.toString());
		rtcBuilder.setPeriod(100);
		rtcBuilder.setTag("minFrames");
		rtcBuilder.setToScale(false);
		rtcBuilder.setToDeflate(false);
		rtcBuilder.setCompressLevel(9);
		rtcBuilder.setCompressionStrategy(1);

		SVMPProtocol.Request.Builder rBuilder = SVMPProtocol.Request.newBuilder();
		rBuilder.setType(SVMPProtocol.Request.RequestType.STREAM);
		rBuilder.setStream(rtcBuilder);

		sendMessage(rBuilder.build());

		rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();
		rtcBuilder.setQuality(70);
		rtcBuilder.setFormat(Bitmap.CompressFormat.WEBP.toString());
		rtcBuilder.setPeriod(1000);
		rtcBuilder.setTag("maxFrames");
		rtcBuilder.setToScale(false);
		rtcBuilder.setToDeflate(false);
		rtcBuilder.setCompressLevel(9);
		rtcBuilder.setCompressionStrategy(2);
		
		rBuilder = SVMPProtocol.Request.newBuilder();
		rBuilder.setType(SVMPProtocol.Request.RequestType.STREAM);
		rBuilder.setStream(rtcBuilder);

		sendMessage(rBuilder.build());

	}


	// intercept KeyEvent before it is dispatched to the window
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return keyHandler.tryConsume(event) || super.dispatchKeyEvent(event);
	}

	private float mDownX;
	private float mDownY;
	private final float SCROLL_THRESHOLD = 10;
	private boolean isOnClick;
	// private boolean isVsvPaused = false;
	Handler handler = new Handler();
	Runnable r;
	@Override
	public boolean onTouchEvent(MotionEvent ev) {


		//		
		//		frameFifo = new CircularFifoQueue<byte[]>(10);
		tag = "minFrames";
		handler.removeCallbacks(r);
		r = new Runnable() {

			@Override
			public void run() {
				tag = "maxFrames";
			}
		};

		//		Log.e(TAG, "inside activity on touch.");
		Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mDownX = ev.getX();
			mDownY = ev.getY();
			isOnClick = true;
			//vb.vibrate(50);
			break;
		case MotionEvent.ACTION_CANCEL:
			System.out.println("action cancel");
		case MotionEvent.ACTION_UP:
//			handleLocalScroll();
			handler.postDelayed(r, switchTime);


		case MotionEvent.ACTION_MOVE:
			if (isOnClick && (Math.abs(mDownX - ev.getX()) > SCROLL_THRESHOLD
					|| Math.abs(mDownY - ev.getY()) > SCROLL_THRESHOLD)) {

				//				Log.i(TAG, "movement detected");
				isOnClick = false;



			}
			break;
		default:

			break;
		}
		touchHandler.onTouchEvent(ev);
		//sendRTCMessage();



		return true;
	}

	//	private boolean isProcessing = false;
	//	private int counter = 0;
	//	private long myTime = System.currentTimeMillis();

	public void handleScreenInfoResponse(SVMPProtocol.Response msg) throws IOException, DataFormatException {


		if(tag.equalsIgnoreCase(msg.getStream().getTag())){

			piex = (msg.getStream().getFrameBytes().toByteArray());
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

	private int qualityCalculator(int pingTime){
		int tempQuality;
//		tutorialView.setVisibility(View.GONE);
		if(pingTime >0 && pingTime <= 300){
			tempQuality = 10;
		}
		else if(pingTime > 300 && pingTime <= 400){
			tempQuality = 10;
		}
		else if(pingTime >400 && pingTime <= 500){
			tempQuality = 10;
//			tutorialView.setVisibility(View.VISIBLE);
		}
		else if(pingTime >500 && pingTime <= 600){
			tempQuality = 5;
			
		}else{
			tempQuality = 0;
//			tutorialView.setVisibility(View.VISIBLE);
		}
		return tempQuality;
	}

	private ScheduledExecutorService startThreadInternal(int time){
		ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
		/*This schedules a runnable task every second*/

		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {


			@Override
			public void run() {
				makePingRequest();
			}

		}, 0, time, TimeUnit.MILLISECONDS);

		return scheduleTaskExecutor;
	}
	
	private void handleLocalScroll(){
		
		if(queue.size() > 19){
			localFrameRender = true;
			for(int i=19; i>=0; i--){
				piex = (byte[]) queue.get(i);
				try {
					//thread to sleep for the specified number of milliseconds
					Thread.sleep(50);
				} catch ( java.lang.InterruptedException ie) {
					System.out.println(ie);
				}
			}
			localFrameRender = false;
			queue.clear();
		}
	}
	
	private void enqueueLocalFrames(Request msg){
		byte[] tempBytes = null;
		frameCounter = frameCounter == 21 ? 0 : frameCounter;
		if("minFrames".equalsIgnoreCase(msg.getStream().getTag())){
			tempBytes = msg.getStream().getFrameBytes().toByteArray();
			queue.put(frameCounter, tempBytes);
			frameCounter++;
		}
	}

}

