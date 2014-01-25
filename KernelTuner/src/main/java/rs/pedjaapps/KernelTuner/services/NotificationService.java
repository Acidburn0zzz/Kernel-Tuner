/*
* This file is part of the Kernel Tuner.
*
* Copyright Predrag Čokulov <predragcokulov@gmail.com>
*
* Kernel Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Kernel Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Kernel Tuner. If not, see <http://www.gnu.org/licenses/>.
*/
package rs.pedjaapps.KernelTuner.services;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import rs.pedjaapps.KernelTuner.*;
import rs.pedjaapps.KernelTuner.ui.*;





public class NotificationService extends Service
{
	private boolean thread;
	private final Handler mHandler = new Handler();
	private static final String CPU0_CURR_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
	private String cpu0freq;
	private static final String  CPU1_CURR_FREQ = "/sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq";
	private String cpu1freq;
	private float fLoad;
	private int load;
	private String items;
	private NotificationManager mNotificationManager;
	private static final int NOTIFICATION_ID = 1;
	private final static int PREFERENCES_MODE = Context.MODE_MULTI_PROCESS;
	
	@Override
	public IBinder onBind(Intent intent)
	{
		
		return null;
	}

	

	SharedPreferences sharedPrefs;
	@Override
	public void onCreate()
	{
		Log.d("rs.pedjaapps.KernelTuner","NotificationService created");
		thread = true;
		
		
		super.onCreate();

	}
	
	private  final void getPrefs(){
		
		sharedPrefs = this.getSharedPreferences("rs.pedjaapps.KernelTuner_preferences", PREFERENCES_MODE);
		items = sharedPrefs.getString("notif", "freq");
		System.out.println(items);
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       	Log.d("rs.pedjaapps.KernelTuner","NotificationService started");
       	
       	Runnable runnable = new Runnable() {
			@Override
			public void run() {
				while(thread) {
					getPrefs();
					
					if(items.equals("freq") || items.equals("both")){
					try
					{

						File myFile = new File(CPU0_CURR_FREQ);
						FileInputStream fIn = new FileInputStream(myFile);
						BufferedReader myReader = new BufferedReader(
							new InputStreamReader(fIn));
						String aDataRow = "";
						String aBuffer = "";
						while ((aDataRow = myReader.readLine()) != null)
						{
							aBuffer += aDataRow + "\n";
						}
						cpu0freq = aBuffer.trim().substring(0, aBuffer.trim().length()-3)+"Mhz";
						myReader.close();
						fIn.close();

					}
					catch (Exception e)
					{
						cpu0freq = "offline";
					}
					try
					{

						File myFile = new File(CPU1_CURR_FREQ);
						FileInputStream fIn = new FileInputStream(myFile);
						BufferedReader myReader = new BufferedReader(
							new InputStreamReader(fIn));
						String aDataRow = "";
						String aBuffer = "";
						while ((aDataRow = myReader.readLine()) != null)
						{
							aBuffer += aDataRow + "\n";
						}
						cpu1freq = aBuffer.trim().substring(0, aBuffer.trim().length()-3)+"Mhz";
						myReader.close();
						fIn.close();

					}
					catch (Exception e)
					{
						cpu1freq = "offline";
					}
					}
					if(items.equals("load") || items.equals("both")){
					try {
						RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
						String load = reader.readLine();

						String[] toks = load.split(" ");

						long idle1 = Long.parseLong(toks[5]);
						long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
							+ Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

						try {
							Thread.sleep(360);
						} catch (Exception e) {}

						reader.seek(0);
						load = reader.readLine();
						reader.close();

						toks = load.split(" ");

						long idle2 = Long.parseLong(toks[5]);
						long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
							+ Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

						fLoad =	 (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

					} catch (IOException ex) {
						ex.printStackTrace();
					}
					load =(int) (fLoad*100);
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mHandler.post(new Runnable() {
							@Override
							public void run() {
							if(thread){
								createNotification();
							}
							}
						});
				}
			}
		};
		new Thread(runnable).start();
       	
        return START_STICKY;
    }

	@Override
	public void onDestroy()
	{
		thread=false;
		Log.d("rs.pedjaapps.KernelTuner","NotificationService destroyed");
		items = null;
		if(mNotificationManager!=null){
		mNotificationManager.cancel(NOTIFICATION_ID);
		}
		super.onDestroy();
	}	
	
@SuppressWarnings("deprecation")
private  void createNotification(){
	String ns = Context.NOTIFICATION_SERVICE;
	mNotificationManager = (NotificationManager) getSystemService(ns);
	int icon = R.drawable.ic_launcher;
	//CharSequence tickerText = "Temperature Warning";
	long when = System.currentTimeMillis();
	Notification notification = new Notification(icon, null, when);
notification.flags |= Notification.FLAG_ONGOING_EVENT;
Context context = getApplicationContext();
	CharSequence contentTitle = "CPU Usage:";
	CharSequence contentText = null;
	if(items.equals("load") ){
	contentText = "CPU Load: "+load+"%";
	}
	else if(items.equals("freq") ){
		contentText = "CPU0: "+cpu0freq+", CPU1: "+ cpu1freq;
		}
	if(items.equals("both") ){
		contentText = "CPU0: "+cpu0freq+", CPU1: "+ cpu1freq+", CPU Load: "+load+"%";
		}
	Intent notificationIntent = new Intent(this, KernelTuner.class);
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	
	mNotificationManager.notify(NOTIFICATION_ID, notification);
	

}
	
}
