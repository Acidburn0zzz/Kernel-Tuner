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
package rs.pedjaapps.KernelTuner.helpers;


import android.app.*;
import android.content.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.widget.SeekBar.*;
import java.io.*;
import java.util.*;
import rs.pedjaapps.KernelTuner.*;
import rs.pedjaapps.KernelTuner.entry.*;
import rs.pedjaapps.KernelTuner.tools.*;

import android.view.View.OnClickListener;
public final class VoltageAdapter extends ArrayAdapter<VoltageEntry>
{

	public static ProgressDialog pd = null;
	private final int voltageItemLayoutResource;
	List<IOHelper.VoltageList> voltageList = IOHelper.voltages();
	static List<Integer> voltages = new ArrayList<Integer>();
	static List<String> voltageFreqs =  new ArrayList<String>();
	static List<String> voltageFreqNames =  new ArrayList<String>();

	public VoltageAdapter(final Context context, final int voltageItemLayoutResource)
	{
		super(context, 0);
		this.voltageItemLayoutResource = voltageItemLayoutResource;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent)
	{

		final View view = getWorkingView(convertView);
		final ViewHolder viewHolder = getViewHolder(view);
		final VoltageEntry entry = getItem(position);
		for(IOHelper.VoltageList v: voltageList){
			voltageFreqs.add((v.getFreq()));
		}
		for(IOHelper.VoltageList v: voltageList){
			voltages.add(v.getVoltage());
		}
		for(IOHelper.VoltageList v: voltageList){
			voltageFreqNames.add(v.getFreqName());
		}

		viewHolder.voltageSeekBarView.setMax(700000);
		if(entry.getType()==0){
		viewHolder.voltageSeekBarView.setProgress(entry.getVoltage()-700000);
        }
		else if(entry.getType()==1){
			viewHolder.voltageSeekBarView.setProgress(entry.getVoltage()*1000-700000);
		}
		viewHolder.voltageSeekBarView.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){




				int prog;
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
											  boolean fromUser)
				{
					prog = progress;
					
					viewHolder.buttonView.setText(String.valueOf((progress+700000) / 1000) + "mV");
					
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
				{			
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
				{

					if(entry.getType()==0){
					VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, VoltageAdapter.this.getContext().getResources().getString(R.string.changing_voltage), true, false);
					new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleseek", String.valueOf(prog+700000), voltageFreqs.get(position)});
					}
					else if(entry.getType()==1){
						VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, VoltageAdapter.this.getContext().getResources().getString(R.string.changing_voltage), true, false);
						new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleseek", String.valueOf((prog+700000)/1000), position+""});
					}
				}

			});

		
		if(entry.getType()==0){
			viewHolder.buttonView.setText(String.valueOf((entry.getVoltage()) / 1000) + "mV");
		}
		else if(entry.getType()==1){
			viewHolder.buttonView.setText(String.valueOf((entry.getVoltage())) + "mV");
		}
		viewHolder.buttonView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v)
				{

					AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
					builder.setTitle(voltageFreqNames.get(position));
					builder.setMessage("Set new value: ");
					builder.setIcon(R.drawable.edit_dark);

					final EditText input = new EditText(view.getContext());              
					input.setHint(voltages.get(position).toString());
					input.setInputType(InputType.TYPE_CLASS_NUMBER);
					input.setGravity(Gravity.CENTER_HORIZONTAL);

					builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which)
							{

								if (!input.getText().toString().equals(""))
								{
									if (new File(Constants.VOLTAGE_PATH).exists())
									{
										if (Integer.parseInt(input.getText().toString()) >= 700000 && Integer.parseInt(input.getText().toString()) <= 1400000)
										{
											VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, VoltageAdapter.this.getContext().getResources().getString(R.string.changing_voltage), true, false);
											new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleseek", input.getText().toString(), voltageFreqs.get(position)});
										}
										else
										{
											Toast.makeText(VoltageAdapter.this.getContext(), "Value must be between 700000 and 1400000", Toast.LENGTH_LONG).show();
										}
									}
									else if (new File(Constants.VOLTAGE_PATH_TEGRA_3).exists())
									{
										if (Integer.parseInt(input.getText().toString()) >= 700 && Integer.parseInt(input.getText().toString()) <= 1400)
										{
											VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, VoltageAdapter.this.getContext().getResources().getString(R.string.changing_voltage), true, false);
											new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleseek", input.getText().toString(), voltageFreqs.get(position)});
										}
										else
										{
											Toast.makeText(VoltageAdapter.this.getContext(), VoltageAdapter.this.getContext().getResources().getString(R.string.voltage_value_out_of_bounds), Toast.LENGTH_LONG).show();
										}
									}
								}
								else
								{
									Toast.makeText(VoltageAdapter.this.getContext(), VoltageAdapter.this.getContext().getResources().getString(R.string.voltage_value_empty), Toast.LENGTH_LONG).show();

								}
							}


						});

					builder.setNegativeButton(VoltageAdapter.this.getContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface dialog, int which)
							{

							}});
					builder.setView(input);

					AlertDialog alert = builder.create();

					alert.show();

				}

			});

		viewHolder.minusView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v)
				{
					VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, VoltageAdapter.this.getContext().getResources().getString(R.string.changing_voltage), true, false);
					new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleminus", String.valueOf(position)});

				}

			});

		viewHolder.plusView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v)
				{
					VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, VoltageAdapter.this.getContext().getResources().getString(R.string.changing_voltage), true, false);
					new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleplus", String.valueOf(position)});

				}

			});

		viewHolder.freqView.setText(entry.getFreq());
		/*viewHolder.percentView.setText(entry.getPercent());
		 viewHolder.progressView.setProgress(entry.getProgress());*/

		return view;
	}

	private View getWorkingView(final View convertView)
	{
		View workingView = null;

		if (null == convertView)
		{
			final Context context = getContext();
			final LayoutInflater inflater = (LayoutInflater)context.getSystemService
			(Context.LAYOUT_INFLATER_SERVICE);

			workingView = inflater.inflate(voltageItemLayoutResource, null);
		}
		else
		{
			workingView = convertView;
		}

		return workingView;
	}

	private ViewHolder getViewHolder(final View workingView)
	{
		final Object tag = workingView.getTag();
		ViewHolder viewHolder = null;


		if (null == tag || !(tag instanceof ViewHolder))
		{
			viewHolder = new ViewHolder();

			viewHolder.voltageSeekBarView = (SeekBar) workingView.findViewById(R.id.seekBar1);
			viewHolder.minusView = (ImageView) workingView.findViewById(R.id.minus);
			viewHolder.plusView = (ImageView) workingView.findViewById(R.id.plus);
			viewHolder.buttonView = (Button) workingView.findViewById(R.id.button1);
			viewHolder.freqView = (TextView)workingView.findViewById(R.id.textView1);

			workingView.setTag(viewHolder);

		}
		else
		{
			viewHolder = (ViewHolder) tag;
		}

		return viewHolder;
	}

	private class ViewHolder
	{
		public SeekBar voltageSeekBarView;
		public ImageView minusView;
		public ImageView plusView;
		public Button buttonView;
		public TextView freqView;

	}


}
