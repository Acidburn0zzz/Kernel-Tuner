package rs.pedjaapps.KernelTuner.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.google.ads.c;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import rs.pedjaapps.KernelTuner.Constants;
import rs.pedjaapps.KernelTuner.R;
import rs.pedjaapps.KernelTuner.constants.TempUnit;
import rs.pedjaapps.KernelTuner.fragments.CpuFragment;
import rs.pedjaapps.KernelTuner.fragments.MainFragment;
import rs.pedjaapps.KernelTuner.helpers.IOHelper;
import rs.pedjaapps.KernelTuner.model.FrequencyCollection;
import rs.pedjaapps.KernelTuner.services.NotificationService;
import rs.pedjaapps.KernelTuner.utility.PrefsManager;
import rs.pedjaapps.KernelTuner.utility.Tools;

/**
 * Created by pedja on 17.4.14..
 */
public class MainActivity extends AbsActivity implements Runnable, View.OnClickListener
{
    private long cpuRefreshInterval;
    final int cpuTempPath = IOHelper.getCpuTempPath();
    boolean hideUnsupportedItems;

    private long mLastBackPressTime = 0;
    private Toast mToast;

    private TextView tvCpu0prog;
    private TextView tvCpu1prog;
    private TextView tvCpu2prog;
    private TextView tvCpu3prog;

    private TextView tvBatteryTemp;
    private TextView tvCputemptxt;

    private LinearLayout llCpuTemp;

    Button cpu1toggle;
    Button cpu2toggle;
    Button cpu3toggle;

    private TextView tvCpuLoad;

    private TempUnit tempUnit;
    Handler cpuRefreshHandler;
    Handler uiHandler = new Handler();
    Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setTheme(getThemeRes());
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());

        cpuRefreshInterval = PrefsManager.getCpuRefreshInterval();
        boolean firstLaunch = PrefsManager.isFirstLaunch();
        tempUnit = PrefsManager.getTempUnit();
        hideUnsupportedItems = PrefsManager.hideUnsupportedItems();

        mountDebugFileSystem();
        enableTmemperatureMonitor();
        if (firstLaunch)
        {
            logKernelInfo();
        }

        setupView();

        showChangelog();

        /*if (PrefsManager.getNotificationService())
        {
            startService(new Intent(this, NotificationService.class));
        }
        else
        {
            stopService(new Intent(this, NotificationService.class));
        }*/
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause()
    {
        super.onStop();
        if (mBatInfoReceiver != null)
        {
            unregisterReceiver(mBatInfoReceiver);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (cpuRefreshHandler != null)
        {
            cpuRefreshHandler.removeCallbacks(this);
        }
    }

    private void showChangelog()
    {
        int appVersion = PrefsManager.getAppVersion();

        try
        {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int version = pInfo.versionCode;
            if (appVersion != version)
            {
                Intent myIntent = new Intent(this, Changelog.class);
                startActivity(myIntent);
            }

            PrefsManager.setAppVersion(version);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e(Constants.LOG_TAG, e.getMessage());
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    private void logKernelInfo()
    {
        PrefsManager.setKernelInfo();
    }

    private void enableTmemperatureMonitor()
    {
        if (!IOHelper.isTempEnabled())
        {
            new ATEnableTempMon().execute();
        }
    }

    private void mountDebugFileSystem()
    {
        File file = new File("/sys/kernel/debug");

        if (!file.exists() && file.list().length >= 0)
        {
            new ATMountDebugFS().execute();
        }
    }

    @Override
    public int getThemeRes()
    {
        return R.style.Theme_Kerneltuner;
    }

    public int getLayoutRes()
    {
        return R.layout.activity_main;
    }

    public void setupView()
    {
        //LinearLayout tempPanel = (LinearLayout) findViewById(R.id.temperature_layout);
        //LinearLayout mainPanel = (LinearLayout) findViewById(R.id.buttons_layout);
        //LinearLayout cpuPanel = (LinearLayout) findViewById(R.id.cpu_info_layout);
        //LinearLayout togglesPanel = (LinearLayout) findViewById(R.id.toggles_layout);

        /*if (!PrefsManager.getMainShowTemp())
        {
            tempPanel.setVisibility(View.GONE);
        }
        if (!PrefsManager.getMainShowCpu())
        {
            cpuPanel.setVisibility(View.GONE);
        }
        if (!PrefsManager.getMainShowToggles())
        {
            togglesPanel.setVisibility(View.GONE);
        }
        if (!PrefsManager.getMainShowButtons())
        {
            mainPanel.setVisibility(View.GONE);
        }*/

        tvCpu0prog = (TextView) findViewById(R.id.txtCpu0);
        tvCpu1prog = (TextView) findViewById(R.id.txtCpu1);
        tvCpu2prog = (TextView) findViewById(R.id.txtCpu2);
        tvCpu3prog = (TextView) findViewById(R.id.txtCpu3);

        tvBatteryTemp = (TextView) findViewById(R.id.txtBatteryTemp);
        llCpuTemp = (LinearLayout) findViewById(R.id.temp_cpu_layout);

        ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle("Various kernel and system tuning");
        actionBar.setHomeButtonEnabled(false);

        HandlerThread cpuRefreshThread = new HandlerThread("cpu_refresh_thread");
        cpuRefreshThread.start();
        cpuRefreshHandler = new Handler(cpuRefreshThread.getLooper());
        cpuRefreshHandler.postDelayed(this, cpuRefreshInterval);

        cpu1toggle = (Button) findViewById(R.id.btn_cpu1_toggle);
        cpu1toggle.setOnClickListener(this);

        cpu2toggle = (Button) findViewById(R.id.btn_cpu2_toggle);
        cpu2toggle.setOnClickListener(this);

        cpu3toggle = (Button) findViewById(R.id.btn_cpu3_toggle);
        cpu3toggle.setOnClickListener(this);

        tvCputemptxt = (TextView) findViewById(R.id.txtCpuTemp);
        tvCpuLoad = (TextView) findViewById(R.id.txtCpuLoadText);


        if (IOHelper.cpu1Exists())
        {
            if (hideUnsupportedItems)
                cpu1toggle.setVisibility(View.VISIBLE);
            else
                cpu1toggle.setEnabled(true);

            tvCpu1prog.setVisibility(View.VISIBLE);
        }
        else
        {
            if (hideUnsupportedItems)
                cpu1toggle.setVisibility(View.GONE);
            else
                cpu1toggle.setEnabled(false);

            tvCpu1prog.setVisibility(View.GONE);
        }
        if (IOHelper.cpu2Exists())
        {
            if (hideUnsupportedItems)
                cpu2toggle.setVisibility(View.VISIBLE);
            else
                cpu2toggle.setEnabled(true);

            tvCpu2prog.setVisibility(View.VISIBLE);
        }
        else
        {
            if (hideUnsupportedItems)
                cpu2toggle.setVisibility(View.GONE);
            else
                cpu2toggle.setEnabled(false);

            tvCpu2prog.setVisibility(View.GONE);
        }
        if (IOHelper.cpu3Exists())
        {
            if (hideUnsupportedItems)
                cpu3toggle.setVisibility(View.VISIBLE);
            else
                cpu3toggle.setEnabled(true);

            tvCpu3prog.setVisibility(View.VISIBLE);
        }
        else
        {
            if (hideUnsupportedItems)
                cpu3toggle.setVisibility(View.GONE);
            else
                cpu3toggle.setEnabled(false);

            tvCpu3prog.setVisibility(View.GONE);
        }
        if (PrefsManager.showAds())
        ((AdView) findViewById(R.id.ad)).loadAd(new AdRequest());
        currentFragment = MainFragment.newInstance();
        getFragmentManager().beginTransaction().replace(R.id.flFragmentContainer, currentFragment).commit();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_cpu1_toggle:
                getProgressDialog().show();
                new ATToggleCPU().execute("1");
                break;
            case R.id.btn_cpu2_toggle:
                getProgressDialog().show();
                new ATToggleCPU().execute("2");
                break;
            case R.id.btn_cpu3_toggle:
                getProgressDialog().show();
                new ATToggleCPU().execute("3");
                break;
        }
    }

    private class ATMountDebugFS extends AsyncTask<String, Void, Object>
    {
        @Override
        protected Object doInBackground(String... args)
        {
            CommandCapture command = new CommandCapture(0,
                    "mount -t debugfs debugfs /sys/kernel/debug");
            try
            {
                RootTools.getShell(true).add(command).waitForFinish();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
            return null;
        }
    }

    private class ATEnableTempMon extends AsyncTask<String, Void, Object>
    {
        @Override
        protected Object doInBackground(String... args)
        {
            CommandCapture command = new CommandCapture(
                    0,
                    "chmod 777 /sys/devices/virtual/thermal/thermal_zone1/mode",
                    "chmod 777 /sys/devices/virtual/thermal/thermal_zone0/mode",
                    "echo -n enabled > /sys/devices/virtual/thermal/thermal_zone1/mode",
                    "echo -n enabled > /sys/devices/virtual/thermal/thermal_zone0/mode");
            try
            {
                RootTools.getShell(true).add(command).waitForFinish();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
            return null;
        }
    }

    private class ATToggleCPU extends AsyncTask<String, Void, Object>
    {
        @Override
        protected Object doInBackground(String... args)
        {
            File file = new File("/sys/devices/system/cpu/cpu" + args[0]
                    + "/cpufreq/scaling_governor");
            if (file.exists())
            {
                CommandCapture command = new CommandCapture(0,
                        "echo 1 > /sys/kernel/msm_mpdecision/conf/enabled",
                        "chmod 777 /sys/devices/system/cpu/cpu" + args[0]
                                + "/online",
                        "echo 0 > /sys/devices/system/cpu/cpu" + args[0]
                                + "/online",
                        "chown system /sys/devices/system/cpu/cpu" + args[0]
                                + "/online");
                try
                {
                    RootTools.getShell(true).add(command).waitForFinish();
                }
                catch (Exception e)
                {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }

            else
            {
                CommandCapture command = new CommandCapture(0,
                        "echo 0 > /sys/kernel/msm_mpdecision/conf/enabled",
                        "chmod 666 /sys/devices/system/cpu/cpu" + args[0]
                                + "/online",
                        "echo 1 > /sys/devices/system/cpu/cpu" + args[0]
                                + "/online",
                        "chmod 444 /sys/devices/system/cpu/cpu" + args[0]
                                + "/online",
                        "chown system /sys/devices/system/cpu/cpu" + args[0]
                                + "/online",
                        "chmod 777 /sys/devices/system/cpu/cpu" + args[0]
                                + "/cpufreq/scaling_max_freq",
                        "chmod 777 /sys/devices/system/cpu/cpu" + args[0]
                                + "/cpufreq/scaling_min_freq",
                        "chmod 777 /sys/devices/system/cpu/cpu" + args[0]
                                + "/cpufreq/scaling_cur_freq",
                        "chmod 777 /sys/devices/system/cpu/cpu" + args[0]
                                + "/cpufreq/scaling_governor");
                try
                {
                    RootTools.getShell(true).add(command).waitForFinish();
                }
                catch (Exception e)
                {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }

            return "";
        }

        @Override
        protected void onPostExecute(Object result)
        {
            if(progressDialog != null)progressDialog.dismiss();
        }

    }

    @Override
    public void run()
    {
        String tmp = IOHelper.cpuTemp(cpuTempPath);
        cpuTemp(tmp);
        cpu0update(IOHelper.cpu0CurFreq());
        if(currentFragment instanceof CpuFragment)
        {
            ((CpuFragment)currentFragment).updateCpu(0, Tools.parseInt(IOHelper.cpu0MinFreq(), Constants.CPU_OFFLINE_CODE),
                    Tools.parseInt(IOHelper.cpu0MaxFreq(), Constants.CPU_OFFLINE_CODE), true);
        }

        if (IOHelper.cpu1Exists())
        {
            cpu1update(IOHelper.cpu1CurFreq());
            if(currentFragment instanceof CpuFragment)
            {
                ((CpuFragment)currentFragment).updateCpu(1, Tools.parseInt(IOHelper.cpu1MinFreq(), Constants.CPU_OFFLINE_CODE),
                        Tools.parseInt(IOHelper.cpu1MaxFreq(), Constants.CPU_OFFLINE_CODE), true);
            }
        }
        if (IOHelper.cpu2Exists())
        {
            cpu2update(IOHelper.cpu2CurFreq());
            if(currentFragment instanceof CpuFragment)
            {
                ((CpuFragment)currentFragment).updateCpu(2, Tools.parseInt(IOHelper.cpu2MinFreq(), Constants.CPU_OFFLINE_CODE),
                        Tools.parseInt(IOHelper.cpu2MaxFreq(), Constants.CPU_OFFLINE_CODE), true);
            }
        }
        if (IOHelper.cpu3Exists())
        {
            cpu3update(IOHelper.cpu3CurFreq());
            if(currentFragment instanceof CpuFragment)
            {
                ((CpuFragment)currentFragment).updateCpu(3, Tools.parseInt(IOHelper.cpu3MinFreq(), Constants.CPU_OFFLINE_CODE),
                        Tools.parseInt(IOHelper.cpu3MaxFreq(), Constants.CPU_OFFLINE_CODE), true);
            }
        }
        try
        {
            setCpuLoad(getCpuLoad());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        cpuRefreshHandler.postDelayed(this, cpuRefreshInterval);
    }

    private int getCpuLoad() throws IOException
    {
        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
        String sLoad = reader.readLine();

        String[] toks = sLoad.split(" ");

        long idle1 = Long.parseLong(toks[5]);
        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
                + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
                + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

        try
        {
            Thread.sleep(360);
        }
        catch (Exception ignored)
        {
        }

        reader.seek(0);
        sLoad = reader.readLine();
        reader.close();

        toks = sLoad.split(" ");

        long idle2 = Long.parseLong(toks[5]);
        long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
                + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
                + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

        float fLoad = (float) (cpu2 - cpu1)
                / ((cpu2 + idle2) - (cpu1 + idle1));
        return (int) (fLoad * 100);
    }

    private void cpu0update(final String freq)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (!freq.equals("offline") && freq.length() > 3)
                {
                    tvCpu0prog.setText(getString(R.string.txt_cpu0, freq.trim().substring(0, freq.length() - 3) + "MHz"));
                }
                else
                {
                    tvCpu0prog.setText(getString(R.string.txt_cpu0, "offline"));
                }
            }
        });
    }

    private void cpu1update(final String freq)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (!freq.equals("offline") && freq.length() > 3)
                {
                    tvCpu1prog.setText(getString(R.string.txtCpu1, freq.trim().substring(0, freq.length() - 3) + "MHz"));
                }
                else
                {
                    tvCpu1prog.setText(getString(R.string.txtCpu1, "offline"));
                }
            }
        });
    }

    private void cpu2update(final String freq)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (!freq.equals("offline") && freq.length() > 3)
                {
                    tvCpu2prog.setText(getString(R.string.txtCpu2, freq.trim().substring(0, freq.length() - 3) + "MHz"));
                }
                else
                {
                    tvCpu2prog.setText(getString(R.string.txtCpu2, "offline"));
                }
            }
        });
    }

    private void cpu3update(final String freq)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (!freq.equals("offline") && freq.length() > 3)
                {
                    tvCpu3prog.setText(getString(R.string.txtCpu3, freq.trim().substring(0, freq.length() - 3) + "MHz"));
                }
                else
                {
                    tvCpu3prog.setText(getString(R.string.txtCpu3, "offline"));
                }
            }
        });
    }

    private void setCpuLoad(final int load)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                tvCpuLoad.setText(getString(R.string.txt_cpu_load, load + "%"));
            }
        });
    }

    /**
     * CPU Temperature
     */

    private void cpuTemp(final String cputemp)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                String tmpCputemp = cputemp;
                llCpuTemp.setVisibility(View.VISIBLE);

                if (!cputemp.equals("") || cputemp.length() != 0)
                {
                    if (tempUnit == TempUnit.fahrenheit)
                    {
                        tmpCputemp = String.valueOf((int) (Double.parseDouble(tmpCputemp) * 1.8) + 32);
                        tvCputemptxt.setText(tmpCputemp + "°F");
                        int temp = Integer.parseInt(tmpCputemp);

                        if (temp < 113)
                        {
                            tvCputemptxt.setTextColor(Color.GREEN);
                        }
                        else if (temp >= 113 && temp < 138)
                        {
                            tvCputemptxt.setTextColor(Color.YELLOW);
                        }
                        else if (temp >= 138)
                        {
                            tvCputemptxt.setTextColor(Color.RED);
                        }
                    }

                    else if (tempUnit == TempUnit.celsius)
                    {
                        tvCputemptxt.setText(tmpCputemp + "°C");
                        int temp = Integer.parseInt(tmpCputemp);
                        if (temp < 45)
                        {
                            tvCputemptxt.setTextColor(Color.GREEN);
                        }
                        else if (temp >= 45 && temp <= 59)
                        {
                            tvCputemptxt.setTextColor(Color.YELLOW);
                        }
                        else if (temp > 59)
                        {
                            tvCputemptxt.setTextColor(Color.RED);
                        }
                    }

                    else if (tempUnit == TempUnit.kelvin)
                    {
                        tmpCputemp = String.valueOf((int) (Double.parseDouble(tmpCputemp) + 273.15));

                        tvCputemptxt.setText(tmpCputemp + "°K");
                        int temp = Integer.parseInt(tmpCputemp);
                        if (temp < 318)
                        {
                            tvCputemptxt.setTextColor(Color.GREEN);
                        }
                        else if (temp >= 318 && temp <= 332)
                        {
                            tvCputemptxt.setTextColor(Color.YELLOW);
                        }
                        else if (temp > 332)
                        {
                            tvCputemptxt.setTextColor(Color.RED);
                        }
                    }
                }
                else
                {
                    llCpuTemp.setVisibility(View.GONE);
                }
            }
        });

    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context arg0, Intent intent)
        {
            double temperature = intent.getIntExtra(
                    BatteryManager.EXTRA_TEMPERATURE, 0) / 10;

            if (tempUnit == TempUnit.fahrenheit)
            {
                temperature = (temperature * 1.8) + 32;
                tvBatteryTemp.setText(((int) temperature) + "°F");
                if (temperature <= 104)
                {
                    tvBatteryTemp.setTextColor(Color.GREEN);
                }
                else if (temperature > 104 && temperature < 131)
                {
                    tvBatteryTemp.setTextColor(Color.YELLOW);
                }
                else if (temperature >= 131 && temperature < 140)
                {
                    tvBatteryTemp.setTextColor(Color.RED);
                }
                else if (temperature >= 140)
                {
                    tvBatteryTemp.setTextColor(Color.RED);
                }
            }
            else if (tempUnit == TempUnit.celsius)
            {
                tvBatteryTemp.setText(temperature + "°C");
                if (temperature < 45)
                {
                    tvBatteryTemp.setTextColor(Color.GREEN);

                }
                else if (temperature > 45 && temperature < 55)
                {
                    tvBatteryTemp.setTextColor(Color.YELLOW);
                }
                else if (temperature >= 55 && temperature < 60)
                {
                    tvBatteryTemp.setTextColor(Color.RED);
                }
                else if (temperature >= 60)
                {
                    tvBatteryTemp.setTextColor(Color.RED);
                }
            }
            else if (tempUnit == TempUnit.kelvin)
            {
                temperature = temperature + 273.15;
                tvBatteryTemp.setText(temperature + "°K");
                if (temperature < 318.15)
                {
                    tvBatteryTemp.setTextColor(Color.GREEN);
                }
                else if (temperature > 318.15 && temperature < 328.15)
                {
                    tvBatteryTemp.setTextColor(Color.YELLOW);
                }
                else if (temperature >= 328.15 && temperature < 333.15)
                {
                    tvBatteryTemp.setTextColor(Color.RED);
                }
                else if (temperature >= 333.15)
                {
                    tvBatteryTemp.setTextColor(Color.RED);
                }
            }
            // /F = (C x 1.8) + 32
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(1, 1, 1, getString(R.string.settings))
                .setIcon(R.drawable.settings_dark)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(2, 2, 2, getString(R.string.compatibility_check)).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(3, 3, 3, getString(R.string.swap))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == 1)
        {
            startActivity(new Intent(this, Preferences.class));
        }
        else if (item.getItemId() == 2)
        {
            startActivity(new Intent(this, CompatibilityCheck.class));
        }
        else if (item.getItemId() == 3)
        {
            Intent myIntent = new Intent(this, Swap.class);
            startActivity(myIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        if (mLastBackPressTime < java.lang.System.currentTimeMillis() - 4000)
        {
            mToast = Toast.makeText(this,
                    getResources().getString(R.string.press_again_to_exit),
                    Toast.LENGTH_SHORT);
            mToast.show();
            mLastBackPressTime = java.lang.System.currentTimeMillis();
        }
        else
        {
            if (mToast != null)
                mToast.cancel();
            finish();
            mLastBackPressTime = 0;
        }
    }

    public void setCurrentFragment(Fragment currentFragment)
    {
        this.currentFragment = currentFragment;
    }

    public Fragment getCurrentFragment()
    {
        return currentFragment;
    }
}
