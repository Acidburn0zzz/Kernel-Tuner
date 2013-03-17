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
package rs.pedjaapps.KernelTuner.ui;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import rs.pedjaapps.KernelTuner.R;
import rs.pedjaapps.KernelTuner.entry.SysCtlEntry;
import rs.pedjaapps.KernelTuner.helpers.SysCtlAdapter;
import rs.pedjaapps.KernelTuner.tools.RootExecuter;
import rs.pedjaapps.KernelTuner.helpers.DatabaseHandler;
import rs.pedjaapps.KernelTuner.entry.SysCtlDatabaseEntry;
import android.content.Intent;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.app.SherlockActivity;
import rs.pedjaapps.KernelTuner.tools.Tools;



public class SysCtl extends SherlockActivity
{
	ListView sysListView;
	SysCtlAdapter sysAdapter;
	List<SysCtlEntry> entries;
	ProgressDialog pd;
	CheckBox kernel, vm, fs, net;
	SharedPreferences preferences;
	DatabaseHandler db = new DatabaseHandler(this);

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String theme = preferences.getString("theme", "light");

		setTheme(Tools.getPreferedTheme(theme));
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.sysctl);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		/**
		 * Load ads if enabled in settings*/
		final boolean ads = preferences.getBoolean("ads", true);
		if (ads == true)
		{
			AdView adView = (AdView)findViewById(R.id.ad);
			adView.loadAd(new AdRequest());
		}

		kernel = (CheckBox)findViewById(R.id.kernel);
		vm = (CheckBox)findViewById(R.id.vm);
		fs = (CheckBox)findViewById(R.id.fs);
		net = (CheckBox)findViewById(R.id.net);
		
		kernel.setChecked(preferences.getBoolean("sysctl_kernel", true));
		vm.setChecked(preferences.getBoolean("sysctl_vm", true));
		fs.setChecked(preferences.getBoolean("sysctl_fs", true));
		net.setChecked(preferences.getBoolean("sysctl_net", false));
		
		kernel.setOnCheckedChangeListener(new Listener());
		net.setOnCheckedChangeListener(new Listener());
		vm.setOnCheckedChangeListener(new Listener());
		fs.setOnCheckedChangeListener(new Listener());
		
		
		
		sysListView = (ListView) findViewById(R.id.list);
		sysAdapter = new SysCtlAdapter(this, R.layout.sysctl_row);
		sysListView.setAdapter(sysAdapter);

		sysListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View v, final int pos,
										long is)
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
					final SysCtlEntry tmpEntry = sysAdapter.getItem(pos);
					builder.setTitle(tmpEntry.getName());

					builder.setMessage("Set new value!");

					builder.setIcon(R.drawable.sysctl);
					

					final EditText input = new EditText(v.getContext());
					input.setText(tmpEntry.getValue());
					input.selectAll();
					input.setGravity(Gravity.CENTER_HORIZONTAL);
					input.requestFocus();
					builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which)
							{

								RootExecuter.exec(new String[]{"sysctl -w " + tmpEntry.getName().trim() + "=" + input.getText().toString().trim()+"\n"});
								
								sysAdapter.remove(tmpEntry);
								sysAdapter.insert(new SysCtlEntry(tmpEntry.getName(), input.getText().toString()), pos);
								sysAdapter.notifyDataSetChanged();
								
								if(db.sysEntryExists(tmpEntry.getName()))
								{
									db.updateSysEntry(new SysCtlDatabaseEntry(tmpEntry.getName(), input.getText().toString()));
								}
								else{
								    db.addSysCtlEntry(new SysCtlDatabaseEntry(tmpEntry.getName(), input.getText().toString()));
                                }
								
							}
						});
					builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface arg0, int arg1)
							{


							}

						});
					builder.setView(input);

					AlertDialog alert = builder.create();

					alert.show();
				}
			});

		try{
			String line;
			Process p = Runtime.getRuntime().exec("which sysctl");
			InputStream inputStream = p.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			 line = bufferedReader.readLine();
			
				if(line!=null && line.startsWith("/")){
					System.out.println(line+"sys");
					new GetSysCtlEntries().execute();
				}
				else{
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle("Error!");

					builder.setMessage("sysctl executable not found!\nDid you install busybox?");

					builder.setIcon(R.drawable.sysctl);
					
					builder.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface arg0, int arg1)
							{
								finish();
							}

						});
					AlertDialog alert = builder.create();
					alert.setCancelable(false);
					alert.setCanceledOnTouchOutside(false);
					alert.show();
				}
			
		}
		catch(Exception e){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Error!");

			builder.setMessage("Something went wrong\n"+e.getMessage());

			builder.setIcon(R.drawable.sysctl);
			
			builder.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						finish();
					}

				});
			AlertDialog alert = builder.create();
			alert.setCancelable(false);
			alert.setCanceledOnTouchOutside(false);
			alert.show();
		}
		

	}

	private class Listener implements CompoundButton.OnCheckedChangeListener{

		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
			new GetSysCtlEntries().execute();
		}
		
	}
	
	private class GetSysCtlEntries extends AsyncTask<String, SysCtlEntry, Void>
	{
		String line;
		@Override
		protected Void doInBackground(String... args)
		{
			entries = new ArrayList<SysCtlEntry>();
			Process proc = null;
			try
			{
				proc = Runtime.getRuntime().exec("sysctl -a\n");
				InputStream inputStream = proc.getInputStream();
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				while ((line = bufferedReader.readLine()) != null)
				{
					if (line.startsWith("sysctl:"))
					{
						//	System.out.println(line);
					}
					else
					{
						//line = line.replaceAll("\\s", "");
						String[] temp = line.split("=");
						List<String> tmp = Arrays.asList(temp);

						//System.out.println(line);
						//	System.out.println(tmp.get(0));
						SysCtlEntry tmpEntry = new SysCtlEntry(tmp.get(0), tmp.get(1));
						entries.add(tmpEntry);
						publishProgress(tmpEntry);
					}

				}
			}
			catch (Exception e)
			{
				Log.e("du", "error " + e.getMessage());
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(SysCtlEntry... values)
		{
			if(values[0].getName().startsWith("kernel")){
				if(kernel.isChecked()){
					sysAdapter.add(values[0]);
					sysAdapter.notifyDataSetChanged();
				}
			}
			else if(values[0].getName().startsWith("vm")){
				if(vm.isChecked()){
					sysAdapter.add(values[0]);
					sysAdapter.notifyDataSetChanged();
				}
			}
			else if(values[0].getName().startsWith("fs")){
				if(fs.isChecked()){
					sysAdapter.add(values[0]);
					sysAdapter.notifyDataSetChanged();
				}
			}
			else if(values[0].getName().startsWith("net")){
				if(net.isChecked()){
					sysAdapter.add(values[0]);
					sysAdapter.notifyDataSetChanged();
				}
			}
			else {
					sysAdapter.add(values[0]);
					sysAdapter.notifyDataSetChanged();
			}
			super.onProgressUpdate();
		}

		@Override
		protected void onPostExecute(Void res)
		{
			SharedPreferences.Editor editor = preferences.edit();
			     editor.putBoolean("sysctl_kernel", kernel.isChecked())
				 .putBoolean("sysctl_vm", vm.isChecked())
				 .putBoolean("sysctl_fs",fs.isChecked()).
				 putBoolean("sysctl_net",net.isChecked()).
				 apply();
			setProgressBarIndeterminateVisibility(false);

		}
		@Override
		protected void onPreExecute()
		{
			setProgressBarIndeterminateVisibility(true);
			sysAdapter.clear();
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, KernelTuner.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

				return true;

		}
		return super.onOptionsItemSelected(item);
	}


}