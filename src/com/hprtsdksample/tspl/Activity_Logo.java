package com.hprtsdksample.tspl;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

public class Activity_Logo  extends Activity 
{	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);	   
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_logo);			
		
		new Thread()  
        {
			public void run()  
            {				
				/*try 
				{										
					Thread.sleep((long)Integer.parseInt(Activity_Logo.this.getString(R.string.logo_show_time)));
				} 
				catch (InterruptedException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				
				Intent itnCall=new Intent(Activity_Logo.this,Activity_Main.class);
				startActivity(itnCall);
				finish();							
            }
        }.start();		
	}

}
