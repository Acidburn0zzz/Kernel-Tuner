package rs.pedjaapps.kerneltuner.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rs.pedjaapps.kerneltuner.R;
import rs.pedjaapps.kerneltuner.model.LogEntry;
import rs.pedjaapps.kerneltuner.helpers.LogEntryAdapter;
import rs.pedjaapps.kerneltuner.helpers.Logcat;
import rs.pedjaapps.kerneltuner.helpers.LogcatLevel;
import rs.pedjaapps.kerneltuner.utility.Format;
import rs.pedjaapps.kerneltuner.utility.LogSaver;
import rs.pedjaapps.kerneltuner.utility.Prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

public class LogCat extends ListActivity
{
    public static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy HH:mm:ss ZZZZ");
    private static final Executor EX = Executors.newCachedThreadPool();

    static final int FILTER_DIALOG = 1;

    private static final int PREFS_REQUEST = 1;

    private static final int MENU_FILTER = 1;
    private static final int MENU_PLAY = 6;
    private static final int MENU_CLEAR = 8;
    private static final int MENU_SAVE = 9;
    private static final int MENU_JUMP_TOP = 11;
    private static final int MENU_JUMP_BOTTOM = 12;

    static final int WINDOW_SIZE = 1000;

    public static final int CAT_WHAT = 0;
    public static final int CLEAR_WHAT = 2;

    private AlertDialog mFilterDialog;

    private ListView mLogList;
    private LogEntryAdapter mLogEntryAdapter;
    private MenuItem mPlayItem;
    private MenuItem mFilterItem;

    private LogcatLevel mLastLevel = LogcatLevel.V;
    private Logcat mLogcat;
    private Prefs mPrefs;
    private LogCat mThis;
    private boolean mPlay = true;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case CAT_WHAT:
                    final List<String> lines = (List<String>) msg.obj;
                    cat(lines);
                    break;
                case CLEAR_WHAT:
                    mLogEntryAdapter.clear();
                    break;
            }
        }
    };

    private void jumpTop()
    {
        pauseLog();
        mLogList.post(new Runnable()
        {
            public void run()
            {
                mLogList.setSelection(0);
            }
        });
    }

    private void jumpBottom()
    {
        playLog();
        mLogList.setSelection(mLogEntryAdapter.getCount() - 1);
    }

    private void cat(final String s)
    {
        if (mLogEntryAdapter.getCount() > WINDOW_SIZE)
        {
            mLogEntryAdapter.remove(0);
        }

        Format format = mLogcat.mFormat;
        LogcatLevel level = format.getLevel(s);
        if (level == null)
        {
            level = mLastLevel;
        }
        else
        {
            mLastLevel = level;
        }

        final LogEntry entry = new LogEntry(s, level);
        mLogEntryAdapter.add(entry);
    }

    private void cat(List<String> lines)
    {
        for (String line : lines)
        {
            cat(line);
        }
        jumpBottom();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logcat);

        //getWindow().setTitle(getResources().getString(R.string.app_name));

        getActionBar().setSubtitle(getString(R.string.running));
        mThis = this;
        mPrefs = new Prefs(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mLogList = (ListView) findViewById(android.R.id.list);
        mLogList.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener()
        {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
            {
                MenuItem jumpTopItem = menu.add(0, MENU_JUMP_TOP, 0, R.string.jump_start_menu);
                jumpTopItem.setIcon(android.R.drawable.ic_media_previous);

               MenuItem jumpBottomItem = menu.add(0, MENU_JUMP_BOTTOM, 0, R.string.jump_end_menu);
                jumpBottomItem.setIcon(android.R.drawable.ic_media_next);
            }
        });
        mLogList.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
                pauseLog();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
            }
        });

        // Log.v("alogcat", "created");
    }


    @Override
    public void onStart()
    {
        super.onStart();
        // Log.v("alogcat", "started");
    }

    private void init()
    {
        mLogList.setBackgroundColor(Color.WHITE);
        mLogList.setCacheColorHint(Color.WHITE);

        mLogEntryAdapter = new LogEntryAdapter(this, R.layout.logcat_row, new ArrayList<LogEntry>(WINDOW_SIZE));
        setListAdapter(mLogEntryAdapter);
        reset();
        setKeepScreenOn();
    }

    @Override
    public void onResume()
    {
        //Debug.startMethodTracing("alogcat");
        super.onResume();
        //	onNewIntent(getIntent());
        init();
        // Log.v("alogcat", "resumed");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // Log.v("alogcat", "paused");

        //Debug.stopMethodTracing();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mLogcat != null)
        {
            mLogcat.stop();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // Log.v("alogcat", "destroyed");
    }

    @Override
    protected void onSaveInstanceState(Bundle b)
    {
        // Log.v("alogcat", "save instance");
    }

    @Override
    protected void onRestoreInstanceState(Bundle b)
    {
        // Log.v("alogcat", "restore instance");
    }

    public void reset()
    {
        //Toast.makeText(this, R.string.reading_logs, Toast.LENGTH_SHORT).show();
        mLastLevel = LogcatLevel.V;

        if (mLogcat != null)
        {
            mLogcat.stop();
        }

        mPlay = true;

        EX.execute(new Runnable()
        {
            public void run()
            {
                mLogcat = new Logcat(mThis, mHandler);
                mLogcat.start();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        mPlayItem = menu.add(0, MENU_PLAY, 0, R.string.pause_menu);
        mPlayItem.setIcon(android.R.drawable.ic_media_pause).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        setPlayMenu();

        mFilterItem = menu.add(
                0,
                MENU_FILTER,
                0,
                getResources().getString(R.string.filter_menu, mPrefs.getFilter()));
        mFilterItem.setIcon(android.R.drawable.ic_menu_search)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        setFilterMenu();

        MenuItem clearItem = menu.add(0, MENU_CLEAR, 0, R.string.clear_menu);
        clearItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        MenuItem saveItem = menu.add(0, MENU_SAVE, 0, R.string.save_menu);
        saveItem.setIcon(android.R.drawable.ic_menu_save).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return true;
    }

    public void setPlayMenu()
    {
        if (mPlayItem == null)
        {
            return;
        }
        if (mPlay)
        {
            mPlayItem.setTitle(R.string.pause_menu);
            mPlayItem.setIcon(android.R.drawable.ic_media_pause);
        }
        else
        {
            mPlayItem.setTitle(R.string.play_menu);
            mPlayItem.setIcon(android.R.drawable.ic_media_play);
        }
    }

    void setFilterMenu()
    {
        if (mFilterItem == null)
        {
            return;
        }
        int filterMenuId;
        String filter = mPrefs.getFilter();
        if (filter == null || filter.length() == 0)
        {
            filterMenuId = R.string.filter_menu_empty;
        }
        else
        {
            filterMenuId = R.string.filter_menu;
        }
        mFilterItem.setTitle(getResources().getString(filterMenuId, filter));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_FILTER:
                showDialog(FILTER_DIALOG);
                return true;
            case MENU_SAVE:
                File f = save();
                String msg = getResources().getString(R.string.saving_log, f.toString());
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return true;
            case MENU_PLAY:
                if (mPlay)
                {
                    pauseLog();
                }
                else
                {
                    jumpBottom();
                }
                return true;
            case MENU_CLEAR:
                clear();
                reset();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case PREFS_REQUEST:
                setKeepScreenOn();
                break;
        }
    }

    private void setKeepScreenOn()
    {
        if (mPrefs.isKeepScreenOn())
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else
        {
            getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_JUMP_TOP:
                Toast.makeText(this, getString(R.string.logcat_jumping_to_top), Toast.LENGTH_SHORT).show();
                jumpTop();
                return true;
            case MENU_JUMP_BOTTOM:
                Toast.makeText(this, getString(R.string.logcat_jumping_to_bottom), Toast.LENGTH_SHORT).show();
                jumpBottom();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void clear()
    {
        try
        {
            Runtime.getRuntime().exec(new String[]{"logcat", "-c"});
        }
        catch (IOException e)
        {
            Log.e("alogcat", "error clearing log", e);
        }
        finally
        {
        }
    }

    private String dump(boolean html)
    {
        StringBuilder sb = new StringBuilder();
        LogcatLevel lastLevel = LogcatLevel.V;

        // make copy to avoid CME
        List<LogEntry> entries = new ArrayList<>(mLogEntryAdapter.getEntries());

        for (LogEntry le : entries)
        {
            if (!html)
            {
                sb.append(le.getText());
                sb.append('\n');
            }
            else
            {
                LogcatLevel level = le.getLevel();
                if (level == null)
                {
                    level = lastLevel;
                }
                else
                {
                    lastLevel = level;
                }
                sb.append("<font color=\"");
                sb.append(level.getHexColor());
                sb.append("\" face=\"sans-serif\"><b>");
                sb.append(TextUtils.htmlEncode(le.getText()));
                sb.append("</b></font><br/>\n");
            }
        }

        return sb.toString();
    }

    private File save()
    {
        final File path = new File(Environment.getExternalStorageDirectory(), "logcat");
        final File file = new File(path + File.separator + "logcat."
                + LogSaver.LOG_FILE_FORMAT.format(new Date()) + ".txt");

        // String msg = "saving log to: " + file.toString();
        // Log.d("alogcat", msg);

        EX.execute(new Runnable()
        {
            public void run()
            {
                String content = dump(false);

                if (!path.exists())
                {
                    path.mkdir();
                }

                BufferedWriter bw = null;
                try
                {
                    file.createNewFile();
                    bw = new BufferedWriter(new FileWriter(file), 1024);
                    bw.write(content);
                }
                catch (IOException e)
                {
                    Log.e("logcat", "error saving log", e);
                }
                finally
                {
                    if (bw != null)
                    {
                        try
                        {
                            bw.close();
                        }
                        catch (IOException e)
                        {
                            Log.e("logcat", "error closing log", e);
                        }
                    }
                }
            }
        });

        return file;
    }

    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case FILTER_DIALOG:
                mFilterDialog = new FilterDialog(this);
                return mFilterDialog;
        }
        return null;
    }

    private void pauseLog()
    {
        if (!mPlay)
        {
            return;
        }
        getActionBar().setSubtitle(getString(R.string.paused));
        if (mLogcat != null)
        {
            mLogcat.setPlay(false);
            mPlay = false;
        }
        setPlayMenu();
    }

    private void playLog()
    {
        if (mPlay)
        {
            return;
        }
        getActionBar().setSubtitle(R.string.running);
        if (mLogcat != null)
        {
            mLogcat.setPlay(true);
            mPlay = true;
        }
        else
        {
            reset();
        }
        setPlayMenu();
    }
}
