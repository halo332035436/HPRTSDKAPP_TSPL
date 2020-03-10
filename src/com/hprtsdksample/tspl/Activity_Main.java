package com.hprtsdksample.tspl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tspl.HPRTPrinterHelper;
import tspl.IPort;
import tspl.Print;
import tspl.PublicFunction;


public class Activity_Main extends Activity 
{
	private Context thisCon=null;
	private BluetoothAdapter mBluetoothAdapter;
	private PublicFunction PFun=null;
	private PublicAction PAct=null;
	
	private Button btnWIFI=null;
	private Button btnBT=null;
	private Button btnUSB=null;
	
	private Spinner spnPrinterList=null;
	private TextView txtTips=null;
	private Button btnOpenCashDrawer=null;
	private Button btnSampleReceipt=null;	
	private Button btn1DBarcodes=null;
	private Button btnQRCode=null;
	private Button btnPDF417=null;
	private Button btnCut=null;
	private Button btnPageMode=null;
	private Button btnImageManage=null;
	private Button btnGetRemainingPower=null;
	
	private EditText edtTimes=null;
	
	private ArrayAdapter arrPrinterList; 
	private String ConnectType="";
//	private String PrinterName="";
	private String PortParam="";
	
	private UsbManager mUsbManager=null;	
	private UsbDevice device=null;
	private static final String ACTION_USB_PERMISSION = "com.HPRTSDKSample";
	private PendingIntent mPermissionIntent=null;
	private static IPort Printer=null;
	private Handler handler;
	private ProgressDialog dialog;
	private static String[] PERMISSIONS_STORAGE = {
			"android.permission.READ_EXTERNAL_STORAGE",
			"android.permission.WRITE_EXTERNAL_STORAGE" };
	private Print print;
	private ExecutorService executorService = Executors.newSingleThreadExecutor();
	private String mStatusStr="";

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setTitle(BuildConfig.VERSION_NAME);
		try
		{
			thisCon=this.getApplicationContext();
			initPrint();
			btnWIFI = (Button) findViewById(R.id.btnWIFI);
			btnUSB = (Button) findViewById(R.id.btnUSB);
			btnBT = (Button) findViewById(R.id.btnBT);
			
			//edtTimes = (EditText) findViewById(R.id.edtTimes);
			
			spnPrinterList = (Spinner) findViewById(R.id.spn_printer_list);	
			txtTips = (TextView) findViewById(R.id.txtTips);
			btnSampleReceipt = (Button) findViewById(R.id.btnSampleReceipt);
			btnOpenCashDrawer = (Button) findViewById(R.id.btnOpenCashDrawer);
			btn1DBarcodes = (Button) findViewById(R.id.btn1DBarcodes);
			btnQRCode = (Button) findViewById(R.id.btnQRCode);
			btnPDF417 = (Button) findViewById(R.id.btnPDF417);
			btnCut = (Button) findViewById(R.id.btnCut);
			btnPageMode = (Button) findViewById(R.id.btnPageMode);
			btnImageManage = (Button) findViewById(R.id.btnImageManage);
			btnGetRemainingPower = (Button) findViewById(R.id.btnGetRemainingPower);
					
			mPermissionIntent = PendingIntent.getBroadcast(thisCon, 0, new Intent(ACTION_USB_PERMISSION), 0);
	        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			thisCon.registerReceiver(mUsbReceiver, filter);

			IntentFilter intent = new IntentFilter();
			intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			registerReceiver(mReceiver, intent);
			
			PFun=new PublicFunction(thisCon);
			PAct=new PublicAction(thisCon);
			InitSetting();
			InitCombox();
			this.spnPrinterList.setOnItemSelectedListener(new OnItemSelectedPrinter());
			//Enable Bluetooth
			EnableBluetooth();
			handler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					// TODO Auto-generated method stub
					super.handleMessage(msg);
					if (msg.what==1) {
						Toast.makeText(thisCon, "succeed", Toast.LENGTH_SHORT).show();
						dialog.cancel();
					}else {
						Toast.makeText(thisCon, "failure",Toast.LENGTH_SHORT).show();
						txtTips.setText(thisCon.getString(R.string.activity_main_disconnected));
						dialog.cancel();
					}
				}
			};
		}
		catch (Exception e) 
		{			
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> onCreate ")).append(e.getMessage()).toString());
		}

	}

	private void initPrint() {
		print = new Print();
//		print.setWifi(false);
	}

	private void InitSetting()
	{
		String SettingValue="";
		SettingValue=PFun.ReadSharedPreferencesData("Codepage");
		if(SettingValue.equals(""))		
			PFun.WriteSharedPreferencesData("Codepage", "0,PC437(USA:Standard Europe)");			
		
		SettingValue=PFun.ReadSharedPreferencesData("Cut");
		if(SettingValue.equals(""))		
			PFun.WriteSharedPreferencesData("Cut", "0");
			
		SettingValue=PFun.ReadSharedPreferencesData("Cashdrawer");
		if(SettingValue.equals(""))			
			PFun.WriteSharedPreferencesData("Cashdrawer", "0");
					
		SettingValue=PFun.ReadSharedPreferencesData("Buzzer");
		if(SettingValue.equals(""))			
			PFun.WriteSharedPreferencesData("Buzzer", "0");
					
		SettingValue=PFun.ReadSharedPreferencesData("Feeds");
		if(SettingValue.equals(""))			
			PFun.WriteSharedPreferencesData("Feeds", "0");				
	}
	
	//add printer list
	private void InitCombox()
	{
		try
		{
			arrPrinterList = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
			String strSDKType=thisCon.getString(com.hprtsdksample.tspl.R.string.sdk_type);
			if(strSDKType.equals("tspl"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_tspl, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("hprt"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_hprt, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("mkt"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_mkt, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("mprint"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_mprint, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("sycrown"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_sycrown, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("mgpos"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_mgpos, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("ds"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_ds, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("cst"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_cst, android.R.layout.simple_spinner_item);
			if(strSDKType.equals("other"))
				arrPrinterList=ArrayAdapter.createFromResource(this, R.array.printer_list_other, android.R.layout.simple_spinner_item);
			arrPrinterList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			PrinterName=arrPrinterList.getItem(0).toString();
			spnPrinterList.setAdapter(arrPrinterList);
		}
		catch (Exception e) 
		{			
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> InitCombox ")).append(e.getMessage()).toString());
		}
	}
	
	private class OnItemSelectedPrinter implements OnItemSelectedListener
	{				
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) 
		{

//			PrinterName=arrPrinterList.getItem(arg2).toString();
//			HPRTPrinter=new HPRTPrinterHelper(thisCon,PrinterName);
			CapturePrinterFunction();
	//		GetPrinterProperty();
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) 
		{
			// TODO Auto-generated method stub			
		}
	}
	
	//EnableBluetooth
	private boolean EnableBluetooth()
    {
        boolean bRet = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter != null)
        {
            if(mBluetoothAdapter.isEnabled())
                return true;
            mBluetoothAdapter.enable();
            try 
    		{
    			Thread.sleep(500);
    		} 
    		catch (InterruptedException e) 
    		{			
    			e.printStackTrace();
    		}
            if(!mBluetoothAdapter.isEnabled())
            {
                bRet = true;
                Log.d("PRTLIB", "BTO_EnableBluetooth --> Open OK");
            }
//			IntentFilter intent = new IntentFilter();
//			intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//			registerReceiver(mReceiver, intent);
        } 
        else
        {
        	Log.d("HPRTSDKSample", (new StringBuilder("Activity_Main --> EnableBluetooth ").append("Bluetooth Adapter is null.")).toString());
        }
        return bRet;
    }
//	public final BroadcastReceiver mReceiver=new BroadcastReceiver(){
//
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
//				txtTips.setText(thisCon.getString(R.string.activity_main_connected));
//			}
//		}
//	};
	
	//call back by scan bluetooth printer
	@Override  
  	protected void onActivityResult(int requestCode, int resultCode, final Intent data)  
  	{  
  		try
  		{  		
  			String strIsConnected;
	  		switch(resultCode)
	  		{
	  			case HPRTPrinterHelper.ACTIVITY_CONNECT_BT:
	  				String strBTAddress="";
	  				strIsConnected=data.getExtras().getString("is_connected");
	  	        	if (strIsConnected.equals("NO"))
	  	        	{
	  	        		txtTips.setText(thisCon.getString(R.string.activity_main_scan_error));	  	        		
  	                	return;
	  	        	}
	  	        	else
	  	        	{	  	        		
	  						txtTips.setText(thisCon.getString(R.string.activity_main_connected));
	  					return;
	  	        	}		  	        	
	  			case HPRTPrinterHelper.ACTIVITY_CONNECT_WIFI:		
	  				String strIPAddress="";
	  				String strPort="";
	  				strIsConnected=data.getExtras().getString("is_connected");
	  	        	if (strIsConnected.equals("NO"))
	  	        	{
	  	        		txtTips.setText(thisCon.getString(R.string.activity_main_scan_error));	  	        		
  	                	return;
	  	        	}
	  	        	else
	  	        	{	  	        		
	  	        		strIPAddress=data.getExtras().getString("IPAddress");
	  	        		strPort=data.getExtras().getString("Port");
	  	        		if(strIPAddress==null || !strIPAddress.contains("."))	  					
	  						return;	  						  					
//	  	        		HPRTPrinter=new HPRTPrinterHelper(thisCon,spnPrinterList.getSelectedItem().toString().trim());
	  					if(HPRTPrinterHelper.PortOpen("WiFi,"+strIPAddress+","+strPort)!=0)	  						  						
	  						txtTips.setText(thisCon.getString(R.string.activity_main_connecterr));	  	                	
	  					else
	  						txtTips.setText(thisCon.getString(R.string.activity_main_connected));
	  					return;
	  	        	}		  	        	
	  			case HPRTPrinterHelper.ACTIVITY_IMAGE_FILE:	  				
//	  		    	PAct.LanguageEncode();
	  				dialog = new ProgressDialog(Activity_Main.this);
					dialog.setMessage("Printing.....");
					dialog.setProgress(100);
					dialog.show();
		  				new Thread(){
		  					public void run() {
		  						try {
		  							String strImageFile=data.getExtras().getString("FilePath");
		  							Bitmap bmp=BitmapFactory.decodeFile(strImageFile);
		  							int height = bmp.getHeight()/8;
									if(HPRTPrinterHelper.printAreaSize("100",""+ height)==-1){
										Toast.makeText(thisCon,getString(R.string.activity_main_disconnected),Toast.LENGTH_LONG).show();
										return;
									}
		  							HPRTPrinterHelper.CLS();
		  							int a=HPRTPrinterHelper.printImage("0","0",strImageFile,true);
		  							HPRTPrinterHelper.Print("1", "1");
		  							if (a>0) {
										handler.sendEmptyMessage(1);
									}else {
										handler.sendEmptyMessage(0);
									}
		  							}catch (Exception e) {
										handler.sendEmptyMessage(0);
									}
		  					}
		  				}.start();
	  				return;
	  			case HPRTPrinterHelper.ACTIVITY_PRNFILE:	  				
	  				String strPRNFile=data.getExtras().getString("FilePath");
	  				HPRTPrinterHelper.PrintBinaryFile(strPRNFile);  					  				
	  				
	  				return;
  			}
  		}
  		catch(Exception e)
  		{
  			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> onActivityResult ")).append(e.getMessage()).toString());
  		}
        super.onActivityResult(requestCode, resultCode, data);  
  	} 
	
	@SuppressLint("NewApi")
	public void onClickConnect(View view) 
	{		
    	if (!checkClick.isClickEvent()) return;
    	
    	try
    	{
//	    	if(HPRTPrinter!=null)
//			{
//			}

			HPRTPrinterHelper.PortClose();
			if(view.getId()==R.id.btnBT)
	    	{
                if (Build.VERSION.SDK_INT >= 23) {
                    //校验是否已具有模糊定位权限
                    if (ContextCompat.checkSelfPermission(Activity_Main.this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(Activity_Main.this,
                                new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                100);
                    } else {
                        //具有权限
//                 connectBluetooth();
                        ConnectType="Bluetooth";
                        Intent serverIntent = new Intent(thisCon,Activity_DeviceList.class);
                        startActivityForResult(serverIntent, HPRTPrinterHelper.ACTIVITY_CONNECT_BT);
                        return;
                    }
                } else {
                    //系统不高于6.0直接执行
                    ConnectType="Bluetooth";
                    Intent serverIntent = new Intent(thisCon,Activity_DeviceList.class);
                    startActivityForResult(serverIntent, HPRTPrinterHelper.ACTIVITY_CONNECT_BT);
//             connectBluetooth();
                }
				return;
	    	}
	    	else if(view.getId()==R.id.btnWIFI)
	    	{	    		
	    		ConnectType="WiFi";
	    		Intent serverIntent = new Intent(thisCon,Activity_Wifi.class);
//				serverIntent.putExtra("PN", PrinterName);
				startActivityForResult(serverIntent, HPRTPrinterHelper.ACTIVITY_CONNECT_WIFI);				
				return;	
	    	}
	    	else if(view.getId()==R.id.btnUSB)
	    	{
	    		ConnectType="USB";							
//				HPRTPrinter=new HPRTPrinterHelper(thisCon,arrPrinterList.getItem(spnPrinterList.getSelectedItemPosition()).toString());
				//USB not need call "iniPort"				
				mUsbManager = (UsbManager) thisCon.getSystemService(Context.USB_SERVICE);				
		  		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();  		
		  		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		  		boolean HavePrinter=false;
		  		while(deviceIterator.hasNext())
		  		{
		  		    device = deviceIterator.next();
		  		    int count = device.getInterfaceCount();
		  		    for (int i = 0; i < count; i++) 
		  	        {
		  		    	UsbInterface intf = device.getInterface(i); 
		  	            if (intf.getInterfaceClass() == 7)
		  	            {
		  	            	HavePrinter=true;
		  	            	mUsbManager.requestPermission(device, mPermissionIntent);		  	            	
		  	            }
		  	        }
		  		}
		  		if(!HavePrinter)
		  			txtTips.setText(thisCon.getString(R.string.activity_main_connect_usb_printer));	
	    	}
    	}
		catch (Exception e) 
		{			
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> onClickConnect "+ConnectType)).append(e.getMessage()).toString());
		}
    }
		   			
	private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
	{
	    public void onReceive(Context context, Intent intent) 
	    {
	    	try
	    	{
		        String action = intent.getAction();	       
		        if (ACTION_USB_PERMISSION.equals(action))
		        {
			        synchronized (this) 
			        {		        	
			            device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
				        {			 
				        	if(HPRTPrinterHelper.PortOpen(thisCon,device)!=0)
							{					
//				        		HPRTPrinter=null;
								txtTips.setText(thisCon.getString(R.string.activity_main_connecterr));												
			                	return;
							}
				        	else
				        		txtTips.setText(thisCon.getString(R.string.activity_main_connected));
				        		
				        }		
				        else
				        {			        	
				        	return;
				        }
			        }
			    }
				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
				{
					device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (device != null)
					{
						int count = device.getInterfaceCount();
						for (int i = 0; i < count; i++)
						{
							UsbInterface intf = device.getInterface(i);
							//Class ID 7代表打印机
							if (intf.getInterfaceClass() == 7)
							{
								HPRTPrinterHelper.PortClose();
								txtTips.setText(R.string.activity_main_tips);
							}
						}
					}
				}
	    	} 
	    	catch (Exception e) 
	    	{
	    		Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> mUsbReceiver ")).append(e.getMessage()).toString());
	    	}
		}
	};
	
	public void onClickClose(View view) 
	{
    	if (!checkClick.isClickEvent()) return;
    	
    	try
    	{
//	    	if(HPRTPrinter!=null)
//			{
//			}
			HPRTPrinterHelper.PortClose();
			this.txtTips.setText(R.string.activity_main_tips);
			return;	
    	}
		catch (Exception e) 
		{			
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> onClickClose ")).append(e.getMessage()).toString());
		}
    }
	
	public void onClickbtnSetting(View view) 
	{
    	if (!checkClick.isClickEvent()) return;
    	
    	try
    	{
    		startActivity(new Intent(Activity_Main.this, Activity_Setting.class));
    	}
		catch (Exception e) 
		{			
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> onClickClose ")).append(e.getMessage()).toString());
		}
    }
	
	public void onClickDo(View view) 
	{
		if (!checkClick.isClickEvent()) return;
		
		if(!HPRTPrinterHelper.IsOpened())
		{
			Toast.makeText(thisCon, thisCon.getText(R.string.activity_main_tips), Toast.LENGTH_SHORT).show();				
			return;
		}
		try {
			if(HPRTPrinterHelper.WriteData(new byte[1])==-1){
				Toast.makeText(thisCon,thisCon.getString(R.string.activity_main_disconnected),Toast.LENGTH_SHORT).show();
				txtTips.setText(thisCon.getString(R.string.activity_main_disconnected));
				return;
            }
		} catch (Exception e) {
//			e.printStackTrace();
		}

		if(view.getId()==R.id.btnGetStatus)
    	{
    		Intent myIntent = new Intent(this, Activity_Status.class);
        	myIntent.putExtra("StatusMode", PrinterProperty.StatusMode);
        	startActivityFromChild(this, myIntent, 0);
    	}
    	else if(view.getId()==R.id.btnSampleReceipt)
    	{
    		PrintSampleReceipt();
    		
    	}
    	else if(view.getId()==R.id.btn1DBarcodes)
    	{
    		Intent myIntent = new Intent(this, Activity_1DBarcodes.class);    		
        	startActivityFromChild(this, myIntent, 0);
    	}
    	else if(view.getId()==R.id.btnTextFormat)
    	{
    		Intent myIntent = new Intent(this, Activity_TextFormat.class);
        	startActivityFromChild(this, myIntent, 0);
    	}
    	else if(view.getId()==R.id.btnPrintImageFile)
    	{
			Utility.checkBlueboothPermission(Activity_Main.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSIONS_STORAGE, new Utility.Callback() {
				@Override
				public void permit() {
					Intent myIntent = new Intent(Activity_Main.this, Activity_PRNFile.class);
					myIntent.putExtra("Folder", android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
					myIntent.putExtra("FileFilter", "jpg,gif,png,bmp,");
					startActivityForResult(myIntent, HPRTPrinterHelper.ACTIVITY_IMAGE_FILE);
				}

				@Override
				public void pass() {
					Intent myIntent = new Intent(Activity_Main.this, Activity_PRNFile.class);
					myIntent.putExtra("Folder", android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
					myIntent.putExtra("FileFilter", "jpg,gif,png,bmp,");
					startActivityForResult(myIntent, HPRTPrinterHelper.ACTIVITY_IMAGE_FILE);
				}
			});
    	}
    	else if(view.getId()==R.id.btnPrintPRNFile)
    	{
    		Intent myIntent = new Intent(this, Activity_PRNFile.class);    	
        	myIntent.putExtra("Folder", android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
        	myIntent.putExtra("FileFilter", "prn,");
        	startActivityForResult(myIntent, HPRTPrinterHelper.ACTIVITY_PRNFILE);
    	}
    	else if(view.getId()==R.id.btnQRCode)
    	{
    		Intent myIntent = new Intent(this, Activity_QRCode.class);
        	startActivityFromChild(this, myIntent, 0);
    	}    	
    	else if(view.getId()==R.id.btnPrintTestPage)
    	{
    		try {
				HPRTPrinterHelper.SelfTest();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> onClickWIFI ")).append(e.getMessage()).toString());
			}
    	}else if(view.getId()==R.id.btnPrintStatus){
    		getPrintStatus();
		}
    }

	private void getPrintStatus() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try{
					int printerStatus = HPRTPrinterHelper.getPrinterStatus();
					switch(printerStatus){
					    case HPRTPrinterHelper.STATUS_DISCONNECT:
							mStatusStr=getString(R.string.status_disconnect);
					        break;
					    case HPRTPrinterHelper.STATUS_TIMEOUT:
							mStatusStr=getString(R.string.status_timeout);
					        break;
					    case HPRTPrinterHelper.STATUS_OK:
							mStatusStr=getString(R.string.status_ok);
					        break;
					    case HPRTPrinterHelper.STATUS_COVER_OPENED:
							mStatusStr=getString(R.string.status_cover_opened);
					        break;
					    case HPRTPrinterHelper.STATUS_NOPAPER:
							mStatusStr=getString(R.string.status_nopaper);
					        break;
					    case HPRTPrinterHelper.STATUS_OVER_HEATING:
							mStatusStr=getString(R.string.status_over_heating);
					        break;
					    case HPRTPrinterHelper.STATUS_PRINTING:
							mStatusStr=getString(R.string.status_printing);
					        break;
					    default:
					        break;
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(thisCon,mStatusStr,Toast.LENGTH_SHORT).show();
						}
					});
				}catch (Exception e){}
			}
		});
	}


	private void CapturePrinterFunction()
	{
		try
		{
//			if (PrinterName.equals("LPQ58")|PrinterName.equals("LPG4")|PrinterName.equals("LPQ118")|PrinterName.equals("108B")|PrinterName.equals("R42")|PrinterName.equals("106B")
//					|PrinterName.equals("HM-T300")|PrinterName.equals("SM-L300")|PrinterName.equals("HM-E200")|PrinterName.equals("HM-E300")|PrinterName.equals("HM-A300")) {
//				btnCut.setVisibility(View.GONE);
//				btnOpenCashDrawer.setVisibility(View.GONE);
//				btn1DBarcodes.setVisibility(View.VISIBLE);
//				btnQRCode.setVisibility(View.VISIBLE);
//				btnPageMode.setVisibility(View.GONE);
//				btnPDF417.setVisibility(View.GONE);
//				btnGetRemainingPower.setVisibility(View.GONE);
//				btnWIFI.setVisibility(View.VISIBLE);
//				btnUSB.setVisibility(View.VISIBLE);
//				btnBT.setVisibility(View.VISIBLE);
//				btnSampleReceipt.setVisibility(View.VISIBLE);
//			}
			btnWIFI.setVisibility(print.isWifi()?View.VISIBLE:View.GONE);
			btnUSB.setVisibility(print.isUsb()?View.VISIBLE:View.GONE);
			btnBT.setVisibility(print.isBT()?View.VISIBLE:View.GONE);
			btnCut.setVisibility(View.GONE);
			btnOpenCashDrawer.setVisibility(View.GONE);
			btnPageMode.setVisibility(View.GONE);
			btnPDF417.setVisibility(View.GONE);
			btnGetRemainingPower.setVisibility(View.GONE);
		}
		catch(Exception e)
		{
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> CapturePrinterFunction ")).append(e.getMessage()).toString());
		}
	}
	

	
	private void PrintSampleReceipt()
	{
		try
		{
		    if(HPRTPrinterHelper.printAreaSize("100", "100")==-1){
				Toast.makeText(thisCon,thisCon.getString(R.string.activity_main_disconnected),Toast.LENGTH_SHORT).show();
				txtTips.setText(thisCon.getString(R.string.activity_main_disconnected));
				return;
			}
		    HPRTPrinterHelper.CLS();
			String[] ReceiptLines = getResources().getStringArray(R.array.activity_main_sample_2inch_receipt);
			for(int i=0;i<ReceiptLines.length;i++){
				HPRTPrinterHelper.printText("50", ""+(i*30), "9", "0", 2, ReceiptLines[i]);
			}
			HPRTPrinterHelper.printQRcode("10", "640", "M", "5", "M1", "0", "123ABC");
			HPRTPrinterHelper.printQRcode("200", "640", "M", "5", "M1", "0", "123ABC");
			HPRTPrinterHelper.Print("1", "1");




		}
		catch(Exception e)
		{
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> PrintSampleReceipt ")).append(e.getMessage()).toString());
		}
	}

	public void printmodel(View view) {
		// TODO Auto-generated method stub
		try
		{
			HashMap<String, String> pum=new HashMap<String, String>();
			pum.put("[number]", "021D-123-789");
			pum.put("[barcode]", "AFC7150124715012424");
			pum.put("[receiver_name]", "齐齐哈尔木鱼");
			pum.put("[receiver_phone]", "15605883677 0571-53992320");
			pum.put("[receiver_address]", "黑龙江齐齐哈尔市建华区文化大街42号齐齐哈尔大学计算机工程学院001班");
			pum.put("[sender_name]", "浙江杭州行者");//收件人地址第一行
			pum.put("[sender_phone]", "18000989090 0571-53992320");//收件人第二行（若是没有，赋值""）
			pum.put("[sender_address]", "浙江省杭州市余杭区文一西路1001号阿里巴巴淘宝城5号办公楼5号小邮局");//收件人第三行（若是没有，赋值""）
			pum.put("[Orderdetails1]", "我是厦门高崎路飞机场金砖回忆");
			pum.put("[Orderdetails2]", "Orderdetails2");
			pum.put("[Orderdetails3]", "Orderdetails3");//寄件人地址第一行
//			pum.put("[Sender_address2]", "滨盛路1505号1706室信息部");//寄件人第二行（若是没有，赋值""）
//			pum.put("[Barcode]", "998016450402");
//			pum.put("[Waybill]", "运单号：998016450402");
//			pum.put("[Product_types]", "数码产品");
//			pum.put("[Quantity]", "数量：22");
//			pum.put("[Weight]", "重量：22.66KG");
			Set<String> keySet = pum.keySet();
			Iterator<String> iterator = keySet.iterator();
			InputStream afis =this.getResources().getAssets().open("TSPL.txt");//打印模版放在assets文件夹里
			String path = new String(InputStreamToByte(afis ),"utf-8");//打印模版以utf-8无bom格式保存
			while (iterator.hasNext()) {
				String string = (String)iterator.next();
				path = path.replace(string, pum.get(string));
			}
			if(HPRTPrinterHelper.PrintData(path)==-1){
				Toast.makeText(thisCon,thisCon.getString(R.string.activity_main_disconnected),Toast.LENGTH_SHORT).show();
				txtTips.setText(thisCon.getString(R.string.activity_main_disconnected));
			}
		}
		catch(Exception e)
		{
			Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> printmodel ")).append(e.getMessage()).toString());
		}
	}
	private byte[] InputStreamToByte(InputStream is) throws IOException {
		ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
		int ch;
		while ((ch = is.read()) != -1) {
			bytestream.write(ch);
		}
		byte imgdata[] = bytestream.toByteArray();
		bytestream.close();
		return imgdata;
	}
	public final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
				try {
					HPRTPrinterHelper.PortClose();
				} catch (Exception e) {
					e.printStackTrace();
				}
				txtTips.setText(R.string.activity_main_tips);
			}
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			HPRTPrinterHelper.PortClose();
			if (mUsbReceiver!=null){
				unregisterReceiver(mUsbReceiver);
			}
			if (mReceiver!=null){
				unregisterReceiver(mReceiver);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
