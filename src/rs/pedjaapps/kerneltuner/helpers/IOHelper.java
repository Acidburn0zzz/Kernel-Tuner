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
package rs.pedjaapps.kerneltuner.helpers;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rs.pedjaapps.kerneltuner.Constants;
import rs.pedjaapps.kerneltuner.model.Frequency;
import rs.pedjaapps.kerneltuner.model.TimesEntry;
import rs.pedjaapps.kerneltuner.model.Voltage;
import rs.pedjaapps.kerneltuner.root.RCommand;
import rs.pedjaapps.kerneltuner.utility.Tools;

public class IOHelper
{

    public static boolean freqsExists()
    {
        boolean i = false;
        if (new File(Constants.CPU0_FREQS).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean oomExists()
    {
        boolean i = false;
        if (new File(Constants.OOM).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean thermaldExists()
    {
        boolean i = false;
        if (new File(Constants.THERMALD).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean swapsExists()
    {
        boolean i = false;
        if (new File(Constants.SWAPS).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean cpu0Exists()
    {
        return new File(Constants.cpu0online).exists();
    }

    public static boolean cpu1Exists()
    {
        return new File(Constants.cpu1online).exists();
    }

    public static boolean cpu2Exists()
    {
        return new File(Constants.cpu2online).exists();
    }

    public static boolean cpu3Exists()
    {
        return new File(Constants.cpu3online).exists();
    }

    public static boolean cpuScreenOff()
    {
        return new File(Constants.cpuScreenOff).exists();
    }

    public static boolean cpuOnline(int cpu)
    {
        return new File("/sys/devices/system/cpu/cpu" + cpu + "/cpufreq/scaling_governor").exists();
    }

    public static boolean gpuExists()
    {
        File file1 = new File(Constants.GPU_3D);
        File file2 = new File(Constants.GPU_3D_2);
        return file1.exists() || file2.exists();
    }

    public static boolean cdExists()
    {
        boolean i = false;
        if (new File(Constants.CDEPTH).exists())
        {
            i = true;
        }
        return i;
    }

    public static boolean tcpCongestionControlAvailable()
    {
        return new File(Constants.TCP_CONGESTION).exists() && new File(Constants.TCP_AVAILABLE_CONGESTION).exists();
    }

    public static boolean voltageExists()
    {
        boolean i = false;
        if (new File(Constants.VOLTAGE_PATH).exists())
        {
            i = true;
        }
        else if (new File(Constants.VOLTAGE_PATH_TEGRA_3).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean otgExists()
    {
        boolean i = false;
        if (new File(Constants.OTG).exists())
        {
            i = true;
        }
        else if (new File(Constants.OTG_2).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean s2wExists()
    {
        boolean i = false;
        if (new File(Constants.S2W).exists())
        {
            i = true;
        }
        else if (new File(Constants.S2W_ALT).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean dt2wExists()
    {
        return new File(Constants.DT2W).exists();
    }

    public static boolean TISExists()
    {
        boolean i = false;
        if (new File(Constants.TIMES_IN_STATE_CPU0).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean mpdecisionExists()
    {
        boolean i = false;
        if (new File(Constants.MPDECISION).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean buttonsExists()
    {
        boolean i = false;
        if (new File(Constants.BUTTONS_LIGHT).exists())
        {
            i = true;
        }
        else if (new File(Constants.BUTTONS_LIGHT_2).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean sdcacheExists()
    {
        boolean i = false;
        if (new File(Constants.SD_CACHE).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean vsyncExists()
    {
        boolean i = false;
        if (new File(Constants.VSYNC).exists())
        {
            i = true;
        }
        return i;

    }

    public static boolean fchargeExists()
    {
        boolean i = false;
        if (new File(Constants.FCHARGE).exists())
        {
            i = true;
        }
        return i;

    }

    public static List<Frequency> frequencies()
    {
        List<Frequency> entries = new ArrayList<>();
        try
        {
            String data = RCommand.readFileContent(Constants.CPU0_FREQS);
            String[] freqs = data.split(" ");
            for (String s : freqs)
            {
                Frequency frequency = new Frequency();
                int value = Tools.parseInt(s, Constants.CPU_OFFLINE_CODE);
                if (value == Constants.CPU_OFFLINE_CODE) continue;
                String string = null;
                if (s.length() > 3)
                {
                    string = s.trim().substring(0, s.trim().length() - 3) + "MHz";
                }
                if (TextUtils.isEmpty(string)) continue;
                frequency.setFrequencyString(string);
                frequency.setFrequencyValue(value);
                entries.add(frequency);
            }
        }
        catch (Exception e)
        {
            try
            {
                FileInputStream fstream = new FileInputStream(Constants.TIMES_IN_STATE_CPU0);

                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;

                while ((strLine = br.readLine()) != null)
                {
                    String[] delims = strLine.split(" ");
                    String freq = delims[0];

                    Frequency frequency = new Frequency();
                    int value = Tools.parseInt(freq, Constants.CPU_OFFLINE_CODE);
                    if (value == Constants.CPU_OFFLINE_CODE) continue;
                    String string = null;
                    if (freq.length() > 3)
                    {
                        string = freq.trim().substring(0, freq.trim().length() - 3) + "MHz";
                    }
                    if (TextUtils.isEmpty(string)) continue;
                    frequency.setFrequencyString(string);
                    frequency.setFrequencyValue(value);
                    entries.add(frequency);
                }
                Collections.sort(entries, new MyComparator());
                in.close();
                fstream.close();
                br.close();
            }
            catch (Exception ee)
            {
                Crashlytics.logException(ee);
                e.printStackTrace();
            }
        }
        return entries;

    }

    public static String[] getTcpAvailableCongestion()
    {
        try
        {
            return RCommand.readFileContent(Constants.TCP_AVAILABLE_CONGESTION).trim().split(" ");
        }
        catch (Exception e)
        {
            return new String[0];
        }
    }

    public static List<String> getTcpAvailableCongestionAsList()
    {
        return Arrays.asList(getTcpAvailableCongestion());
    }

    public static String getTcpCongestion()
    {
        try
        {
            return RCommand.readFileContent(Constants.TCP_CONGESTION).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static List<String> oom()
    {
        try
        {
            return Arrays.asList(RCommand.readFileContent(Constants.OOM).split(","));
        }
        catch (Exception e)
        {
            return new ArrayList<String>();
        }

    }

    public static String leds()
    {
        try
        {
            return RCommand.readFileContent(Constants.BUTTONS_LIGHT_2).trim();
        }
        catch (Exception e)
        {
            try
            {
                return RCommand.readFileContent(Constants.BUTTONS_LIGHT).trim();
            }
            catch (Exception ee)
            {
                return "";
            }
        }

    }

    public static String[] governors()
    {
        try
        {
            //return RCommand.readFileContent(Constants.CPU0_GOVS)).split("\\s");
            return RCommand.readFileContent(Constants.CPU0_GOVS).trim().split("\\s");
        }
        catch (Exception e)
        {
            return new String[0];
        }

    }

    public static List<String> governorsAsList()
    {
        try
        {
            return Arrays.asList(RCommand.readFileContent(Constants.CPU0_GOVS).split("\\s"));
        }
        catch (Exception e)
        {
            return new ArrayList<>();
        }

    }

    public static int cpu0MinFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU0_MIN_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Constants.CPU_OFFLINE_CODE;
        }

    }

    public static String cpuMin()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU_MIN).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static String cpuMax()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU_MAX).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static int cpu0MaxFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU0_MAX_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }

    }

    public static int cpu1MinFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU1_MIN_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }
    }

    public static int cpu1MaxFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU1_MAX_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }
    }

    public static int cpu2MinFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU2_MIN_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }

    }

    public static int cpu2MaxFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU2_MAX_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }
    }

    public static int cpu3MinFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU3_MIN_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }

    }

    public static int cpu3MaxFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.CPU3_MAX_FREQ).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }

    }

    public static String cpu0CurFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU0_CURR_FREQ).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static String cpu1CurFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU1_CURR_FREQ).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static String cpu2CurFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU2_CURR_FREQ).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }

    }

    public static String cpu3CurFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU3_CURR_FREQ).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static int cpuScreenOffMaxFreq()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.cpuScreenOff).trim(), Constants.CPU_OFFLINE_CODE);
        }
        catch (Exception e)
        {
            return Constants.CPU_OFFLINE_CODE;
        }
    }

    public static String cpu0CurGov()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU0_CURR_GOV).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static String cpu1CurGov()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU1_CURR_GOV).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static String cpu2CurGov()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU2_CURR_GOV).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }

    public static String cpu3CurGov()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU3_CURR_GOV).trim();
        }
        catch (Exception e)
        {
            return "offline";
        }
    }


    public static List<TimesEntry> getTis()
    {
        List<TimesEntry> times = new ArrayList<>();

        try
        {
            String[] lines = RCommand.readFileContentAsLineArray(Constants.TIMES_IN_STATE_CPU0);

            for (String strLine : lines)
            {
                String[] delims = strLine.split(" ");
                times.add(new TimesEntry(Tools.parseInt(delims[0], 0), Long.parseLong(delims[1])));
                //System.out.println(strLine);
            }
        }
        catch (Exception e)
        {
            Log.e("Error: ", e.getMessage());
        }

        return times;

    }


    public static List<Voltage> voltages()
    {
        List<Voltage> voltages = new ArrayList<>();
        if (new File(Constants.VOLTAGE_PATH).exists())
        {
            parseVoltageM1(voltages, Constants.VOLTAGE_PATH);
        }
        if (new File(Constants.VOLTAGE_PATH_2).exists())
        {
            parseVoltageM1(voltages, Constants.VOLTAGE_PATH_2);
        }
        else if (new File(Constants.VOLTAGE_PATH_TEGRA_3).exists())
        {
            parseVoltageM2(voltages, Constants.VOLTAGE_PATH_TEGRA_3);
        }
        return voltages;
    }

    private static void parseVoltageM2(List<Voltage> voltages, String voltagePathTegra3)
    {
        /*
        300mhz: 775 mV
        422mhz: 775 mV
        652mhz: 775 mV
        729mhz: 780 mV
        883mhz: 800 mV
        960mhz: 810 mV
        1036mhz: 820 mV
        1190mhz: 840 mV
        1267mhz: 850 mV
        1497mhz: 880 mV
        1574mhz: 890 mV
        1728mhz: 920 mV
        1958mhz: 965 mV
        2265mhz: 1025 mV
        2457mhz: 1060 mV
        */
        try
        {
            String[] lines = RCommand.readFileContentAsLineArray(voltagePathTegra3);
            for (String strLine : lines)
            {
                strLine = strLine.replaceAll("\\s+", "");
                String[] delims = strLine.split(":");
                if (delims.length < 2) continue;
                Voltage voltage = new Voltage();
                String name, frequency = null;
                if (delims[0].length() > 4)
                {
                    frequency = delims[0].replaceAll("mhz", "") + "MHz";
                }
                name = delims[1].replaceAll("mV", "").trim();
                int value = Tools.parseInt(name, Constants.CPU_OFFLINE_CODE);
                if (frequency == null || value == Constants.CPU_OFFLINE_CODE)
                {
                    continue;
                }
                voltage.setFreq(frequency);
                voltage.setName(name + "mV");
                voltage.setValue(value);
                voltage.setDivider(1);
                voltage.setMultiplier(1000);
                voltages.add(voltage);
            }
        }
        catch (Exception ex)
        {

        }
    }

    private static void parseVoltageM1(List<Voltage> voltages, String path)
    {
        try
        {
            String[] lines = RCommand.readFileContentAsLineArray(path);
            for (String strLine : lines)
            {
                strLine = strLine.trim().replaceAll("\\s+", "");
                Voltage voltage = new Voltage();
                String name, frequency;
                String[] vf = strLine.split(":");
                if (vf.length != 2) continue;
                int frInt = Tools.parseInt(vf[0], -1);
                if (frInt < 0) continue;
                frequency = frInt / 1000 + "MHz";
                int value = Tools.parseInt(vf[1], -1);
                if (value < 0) continue;
                name = value / 1000 + "mV";

                voltage.setFreq(frequency);
                voltage.setName(name);
                voltage.setValue(value);
                voltage.setFreqValue(frInt);
                voltage.setDivider(1000);
                voltage.setMultiplier(1);
                voltages.add(voltage);
            }
        }
        catch (Exception e)
        {

        }
    }


    public static String uptime()
    {
        String uptime;

        int time = (int) SystemClock.elapsedRealtime();

        String s = (time / 1000) % 60 + "";
        String m = (time / (1000 * 60)) % 60 + "";
        String h = (time / (1000 * 3600)) % 24 + "";
        String d = time / (1000 * 60 * 60 * 24) + "";
        StringBuilder builder = new StringBuilder();
        if (!d.equals("0"))
        {
            builder.append(d).append("d:");
        }
        if (!h.equals("0"))
        {
            builder.append(h).append("h:");
        }
        if (!m.equals("0"))
        {
            builder.append(m).append("m:");
        }
        builder.append(s).append("s");
        uptime = builder.toString();

        return uptime;

    }

    public static String deepSleep()
    {
        String deepSleep;

        int time = (int) (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis());

        String s = (time / 1000) % 60 + "";
        String m = (time / (1000 * 60)) % 60 + "";
        String h = (time / (1000 * 3600)) % 24 + "";
        String d = time / (1000 * 60 * 60 * 24) + "";
        StringBuilder builder = new StringBuilder();
        if (!d.equals("0"))
        {
            builder.append(d).append("d:");
        }
        if (!h.equals("0"))
        {
            builder.append(h).append("h:");
        }
        if (!m.equals("0"))
        {
            builder.append(m).append("m:");
        }

        builder.append(s).append("s");
        deepSleep = builder.toString();

        return deepSleep;
    }

    public static int getCpuTempPath()
    {
        int ret = 10;
        for (int i = 0; i < Constants.CPU_TEMP_PATHS.length; i++)
        {
            if (new File(Constants.CPU_TEMP_PATHS[i]).exists())
            {
                ret = i;
                break;
            }
        }
        return ret;
    }

    public static String cpuTemp(int path)
    {
        try
        {
            String temp = RCommand.readFileContent(Constants.CPU_TEMP_PATHS[path]).trim();
            if (temp.length() > 2)
            {
                return temp.substring(0, temp.length() - (temp.length() - 2));
            }
            else
            {
                return temp;
            }
        }
        catch (Exception e2)
        {
            return "0";
        }
    }

    public static String cpuInfo()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU_INFO).trim();
        }
        catch (Exception e2)
        {
            return "";
        }
    }

    public static List<String> availableGovs()
    {
        File govs = new File("/sys/devices/system/cpu/cpufreq/");
        List<String> availableGovs = new ArrayList<>();

        if (govs.exists())
        {
            File[] files = govs.listFiles();

            for (File file : files)
            {
                availableGovs.add(file.getName());
            }
        }

        availableGovs.removeAll(Arrays.asList("vdd_table"));
        return availableGovs;

    }

    public static List<String> govSettings()
    {

        List<String> govSettings = new ArrayList<String>();

        for (String s : availableGovs())
        {
            File gov = new File("/sys/devices/system/cpu/cpufreq/" + s + "/");

            if (gov.exists())
            {
                File[] files = gov.listFiles();
                if (files != null)
                {
                    for (File file : files)
                    {
                        govSettings.add(file.getName());
                    }
                }
            }
        }
        return govSettings;
    }

    public static String[] schedulersAsArray()
    {
        List<String> schedulers = schedulers();
        return schedulers.toArray(new String[schedulers.size()]);
    }

    public static List<String> schedulers()
    {
        List<String> schedulers = new ArrayList<>();
        try
        {
            String schedulersTemp = RCommand.readFileContent(Constants.SCHEDULER);
            String[] temp = schedulersTemp.replace("[", "").replace("]", "").split("\\s");
            Collections.addAll(schedulers, temp);
        }
        catch (Exception e)
        {

        }

        return schedulers;
    }

    public static String mpup()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_THR_UP).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String mpdown()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_THR_DOWN).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static int gpu3d()
    {
        try
        {
            File file1 = new File(Constants.GPU_3D);
            File file2 = new File(Constants.GPU_3D_2);
            if (file1.exists())
            {
                return Tools.parseInt(RCommand.readFileContent(file1.getAbsolutePath()).trim(), Constants.CPU_OFFLINE_CODE);
            }
            else if (file2.exists())
            {
                return Tools.parseInt(RCommand.readFileContent(file2.getAbsolutePath()).trim(), Constants.GPU_OFFLINE_CODE);
            }
            else
            {
                return Constants.GPU_NOT_AVAILABLE;
            }
        }
        catch (Exception e)
        {
            return Constants.GPU_NOT_AVAILABLE;
        }
    }

    public static String gpu3dGovernor()
    {
        try
        {
            File file1 = new File(Constants.GPU_3D_2_GOV);
            if (file1.exists())
            {
                return RCommand.readFileContent(file1.getAbsolutePath()).trim();
            }
            else
            {
                return "n/a";
            }
        }
        catch (Exception e)
        {
            return "n/a";
        }
    }

    public static String gpu2d()
    {
        try
        {
            return RCommand.readFileContent(Constants.GPU_2D).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String getGpu3dFrequenciesAsString()
    {
        List<Frequency> frequencies = gpu3dFrequenciesAsList();
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Frequency fr : frequencies)
        {
            if (i != 0) builder.append(", ");
            builder.append(fr.getFrequencyString());
            i++;
        }
        return builder.toString();
    }

    public static List<Frequency> gpu3dFrequenciesAsList()
    {
        try
        {
            List<Frequency> frequencies = new ArrayList<>();
            File file1 = new File(Constants.GPU_3D_AVAILABLE_FREQUENCIES);
            String[] frqs = RCommand.readFileContent(file1.getAbsolutePath()).trim().split(" ");
            Set<Integer> values = new HashSet<>();
            for (String freq : frqs)
            {
                int frInt = Tools.parseInt(freq, -1);
                if (frInt == -1 || values.contains(frInt)) continue;
                values.add(frInt);
                Frequency frequency = new Frequency();
                frequency.setFrequencyValue(frInt);
                frequency.setFrequencyString(frInt / 1000000 + "MHz");
                frequencies.add(frequency);
            }
            return frequencies;
        }
        catch (Exception e)
        {
            return new ArrayList<Frequency>();
        }
    }


    public static int fcharge()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.FCHARGE).trim(), -1);
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    public static int vsync()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.VSYNC).trim(), -1);
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    public static String cDepth()
    {
        try
        {
            return RCommand.readFileContent(Constants.CDEPTH).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String scheduler()
    {
        String scheduler = "";
        try
        {
            String schedulers = RCommand.readFileContent(Constants.SCHEDULER);
            scheduler = schedulers.substring(schedulers.indexOf("[") + 1, schedulers.indexOf("]")).trim();
        }
        catch (Exception e)
        {

        }
        return scheduler;
    }

    public static int sdCache()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.SD_CACHE).trim(), 0);
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    public static int s2w()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.S2W).trim(), -1);
        }
        catch (Exception e)
        {
            try
            {
                return Tools.parseInt(RCommand.readFileContent(Constants.S2W_ALT).trim(), -1);
            }
            catch (Exception e2)
            {
                return -1;
            }
        }
    }

    public static int dt2w()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.DT2W).trim(), -1);
        }
        catch (IOException e)
        {
            return -1;
        }
    }

    public static int readOTG()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.OTG).trim(), -1);
        }
        catch (Exception e)
        {
            try
            {
                return Tools.parseInt(RCommand.readFileContent(Constants.OTG_2).trim(), -1);
            }
            catch (Exception e2)
            {
                return -1;
            }
        }
    }

    public static String kernel()
    {
        try
        {
            return RCommand.readFileContent(Constants.KERNEL).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    static class MyComparator implements Comparator<Frequency>
    {
        public int compare(Frequency ob1, Frequency ob2)
        {
            return ob1.getFrequencyValue() - ob2.getFrequencyValue();
        }
    }

    public static int batteryLevel()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.BATTERY_LEVEL).trim(), 0);
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    public static double batteryTemp()
    {
        try
        {
            File battTempFile1 = new File(Constants.BATTERY_TEMP);
            File battTempFile2 = new File(Constants.BATTERY_TEMP2);
            if (battTempFile1.exists())
            {
                return Double.parseDouble(RCommand.readFileContent(battTempFile1.getAbsolutePath()).trim());
            }
            else if (battTempFile2.exists())
            {
                return Double.parseDouble(RCommand.readFileContent(battTempFile2.getAbsolutePath()).trim()) / 10;
            }
            else
            {
                return 0.0;
            }
        }
        catch (Exception e)
        {
            return 0.0;
        }
    }

    public static String batteryDrain()
    {
        try
        {
            File file1 = new File(Constants.BATTERY_DRAIN);
            File file2 = new File(Constants.BATTERY_DRAIN2);
            if (file1.exists())
            {
                return RCommand.readFileContent(file1.getAbsolutePath()).trim() + "mA";
            }
            else if (file2.exists())
            {
                return Tools.parseInt(RCommand.readFileContent(file2.getAbsolutePath()).trim(), 1000) / 1000 + "mA";
            }
            else
            {
                return "n/a";
            }

        }
        catch (Exception e)
        {
            return "n/a";
        }
    }

    public static int batteryVoltage()
    {
        try
        {
            File file1 = new File(Constants.BATTERY_VOLTAGE);
            File file2 = new File(Constants.BATTERY_VOLTAGE2);
            if (file1.exists())
            {
                return Tools.parseInt(RCommand.readFileContent(file1.getAbsolutePath()).trim(), 0);
            }
            else if (file2.exists())
            {
                return Tools.parseInt(RCommand.readFileContent(file2.getAbsolutePath()).trim(), 0) / 1000;
            }
            else
            {
                return 0;
            }
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    public static String batteryTechnology()
    {
        try
        {
            return RCommand.readFileContent(Constants.BATTERY_TECH).trim();
        }
        catch (Exception e)
        {
            return "n/a";
        }
    }

    public static String batteryHealth()
    {
        try
        {
            return RCommand.readFileContent(Constants.BATTERY_HEALTH).trim();
        }
        catch (Exception e)
        {
            return "n/a";
        }
    }

    public static String batteryCapacity()
    {
        try
        {
            File file1 = new File(Constants.BATTERY_CAPACITY);
            File file2 = new File(Constants.BATTERY_CAPACITY2);
            if (file1.exists())
            {
                return RCommand.readFileContent(file1.getAbsolutePath()).trim() + "mAh";
            }
            else if (file2.exists())
            {
                return RCommand.readFileContent(file2.getAbsolutePath()).trim() + "mAh";
            }
            else
            {
                return "n/a";
            }
        }
        catch (Exception e)
        {
            return "n/a";
        }
    }

    /**
     * @return 2 if charging from AC
     */
    public static int batteryChargingSource()
    {
        try
        {
            return Tools.parseInt(RCommand.readFileContent(Constants.BATTERY_CHARGING_SOURCE).trim(), 0);
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    public static boolean isTempEnabled()
    {
        try
        {
            return RCommand.readFileContent(Constants.CPU_TEMP_ENABLED).equals("enabled");
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static String mpDelay()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_DELAY).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String mpPause()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_PAUSE).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String mpTimeUp()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_TIME_UP).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String mpTimeDown()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_TIME_DOWN).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String mpIdleFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_IDLE_FREQ).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String mpScroffFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_SCROFF_FREQ).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String mpScroffSingleCore()
    {
        try
        {
            return RCommand.readFileContent(Constants.MPDEC_SCROFF_SINGLE).trim();
        }
        catch (Exception e)
        {
            return "err";
        }
    }

    public static String thermalLowLow()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_LOW_LOW).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalLowHigh()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_LOW_HIGH).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalMidLow()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_MID_LOW).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalMidHigh()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_MID_HIGH).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalMaxLow()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_MAX_LOW).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalMaxHigh()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_MAX_HIGH).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalLowFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_LOW_FREQ).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalMidFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_MID_FREQ).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String thermalMaxFreq()
    {
        try
        {
            return RCommand.readFileContent(Constants.THERMAL_MAX_FREQ).trim();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
