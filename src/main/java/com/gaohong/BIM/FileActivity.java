package com.gaohong.BIM;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.R;

/**
 * @version 1.00.00
 * @description: 浏览文件
 * @author: archko 12-6-1
 */
public class FileActivity extends Activity implements OnItemClickListener {

    private final static String TAG="FileActivity";
    private final static String PREF_TAG="FileActivity";
    private final static String PREF_HOME="Home";

    private String currentPath;

    private TextView pathTextView=null;
    private ListView filesListView=null;
    private FileFilter fileFilter=null;
    private ArrayAdapter<FileListEntry> fileListAdapter=null;
    private ArrayList<FileListEntry> fileList=null;

    private MenuItem aboutMenuItem=null;
    private MenuItem setAsHomeMenuItem=null;
    private MenuItem optionsMenuItem=null;
    private MenuItem deleteContextMenuItem=null;
    private MenuItem removeContextMenuItem=null;
    private MenuItem setAsHomeContextMenuItem=null;

    private Boolean dirsFirst=true;
    private Boolean showExtension=true;
    private Boolean history=true;

    private Boolean light=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (light)
            this.setTheme(android.R.style.Theme_Light);

        currentPath=getHome();

        this.fileFilter=new FileFilter() {
            public boolean accept(File f) {
                //return (f.isDirectory()||f.getName().toLowerCase().endsWith(".pdf"));
                return true;
            }
        };

        this.setContentView(R.layout.filechooser);

        this.pathTextView=(TextView) this.findViewById(R.id.path);
        this.filesListView=(ListView) this.findViewById(R.id.files);
        final Activity activity=this;
        this.fileList=new ArrayList<FileListEntry>();
        this.fileListAdapter=new ArrayAdapter<FileListEntry>(this,
            R.layout.serverbrowseritem, fileList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View v;

                if (convertView==null) {
                    v=View.inflate(activity, R.layout.serverbrowseritem, null);
                } else {
                    v=convertView;
                }

                FileListEntry entry=fileList.get(position);

                v.findViewById(R.id.home).setVisibility(
                    entry.type==FileListEntry.HOME?View.VISIBLE:View.GONE);
                v.findViewById(R.id.upfolder).setVisibility(
                    entry.isUpFolder()?View.VISIBLE:View.GONE);
                v.findViewById(R.id.folder).setVisibility(
                    (entry.type==FileListEntry.NORMAL&&entry.isDirectory&&
                        !entry.isUpFolder())?View.VISIBLE:View.GONE);

                TextView tv=(TextView) v.findViewById(R.id.text);

                tv.setText(entry.label);
                tv.setTypeface(tv.getTypeface(),
                    entry.type==FileListEntry.RECENT?Typeface.ITALIC:Typeface.NORMAL);
                TextView bookSize=(TextView) v.findViewById(R.id.file_size);
                bookSize.setText(entry.size);

                return v;
            }
        };
        this.filesListView.setAdapter(this.fileListAdapter);
        this.filesListView.setOnItemClickListener(this);

        registerForContextMenu(this.filesListView);
    }

    /**
     * Reset list view and list adapter to reflect change to currentPath.
     */
    private void update() {
        this.pathTextView.setText(this.currentPath);
        this.fileListAdapter.clear();

        FileListEntry entry;

        entry=new FileListEntry(FileListEntry.HOME, getResources().getString(R.string.go_home));
        this.fileListAdapter.add(entry);

        if (!this.currentPath.equals("/")) {
            File upFolder=new File(this.currentPath).getParentFile();
            entry=new FileListEntry(FileListEntry.NORMAL, -1, upFolder, "..");
            this.fileListAdapter.add(entry);
        }

        File files[]=new File(this.currentPath).listFiles(this.fileFilter);

        if (files!=null) {
            try {
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        if (f1==null) throw new RuntimeException("f1 is null inside sort");
                        if (f2==null) throw new RuntimeException("f2 is null inside sort");
                        try {
                            if (dirsFirst&&f1.isDirectory()!=f2.isDirectory()) {
                                if (f1.isDirectory())
                                    return -1;
                                else
                                    return 1;
                            }
                            return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
                        } catch (NullPointerException e) {
                            throw new RuntimeException("failed to compare "+f1+" and "+f2, e);
                        }
                    }
                });
            } catch (NullPointerException e) {
                throw new RuntimeException("failed to sort file list "+files+" for path "+this.currentPath, e);
            }

            float size=0;
            for (File file : files) {
                entry=new FileListEntry(FileListEntry.NORMAL, -1, file, file.getName());
                if (file.isFile()) {
                    size=file.length()/(1024.0f);
                    if (size>1024) {
                        entry.size=size/1024.0f+"MB";
                    } else {
                        entry.size=size+"KB";
                    }
                }
                this.fileListAdapter.add(entry);
            }
        }

        this.filesListView.setSelection(0);
    }

    public void ftpView(File f) {
        Log.i(TAG, "post intent to open file "+f);
        Intent intent=new Intent();
        intent.putExtra("file_path", f.getAbsolutePath());
        setResult(RESULT_OK, intent);
        this.finish();
    }

    private String getHome() {
        String defaultHome=Environment.getExternalStorageDirectory().getAbsolutePath();
        String path=getSharedPreferences(PREF_TAG, 0).getString(PREF_HOME,
            defaultHome);
        if (path.length()>1&&path.endsWith("/")) {
            path=path.substring(0, path.length()-2);
        }

        File pathFile=new File(path);

        if (pathFile.exists()&&pathFile.isDirectory())
            return path;
        else
            return defaultHome;
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        FileListEntry clickedEntry=this.fileList.get(position);
        File clickedFile;

        if (clickedEntry.type==FileListEntry.HOME) {
            clickedFile=new File(getHome());
        } else {
            clickedFile=clickedEntry.file;
        }

        if (!clickedFile.exists())
            return;

        if (clickedFile.isDirectory()) {
            this.currentPath=clickedFile.getAbsolutePath();
            this.update();
        } else {
            ftpView(clickedFile);
        }
    }

    public void setAsHome() {
        SharedPreferences.Editor edit=getSharedPreferences(PREF_TAG, 0).edit();
        edit.putString(PREF_HOME, currentPath);
        edit.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem==this.aboutMenuItem) {
            return true;
        } else if (menuItem==this.setAsHomeMenuItem) {
            setAsHome();
            return true;
        } else if (menuItem==this.optionsMenuItem) {
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.setAsHomeMenuItem=menu.add(R.string.set_as_home);
        this.optionsMenuItem=menu.add(R.string.options);
        this.aboutMenuItem=menu.add("About");
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        /*Options.setOrientation(this); *//* TODO: Handle Harder-to-change orientation *//*
        SharedPreferences options=PreferenceManager.getDefaultSharedPreferences(this);
        dirsFirst=options.getBoolean(Options.PREF_DIRS_FIRST, true);
        showExtension=options.getBoolean(Options.PREF_SHOW_EXTENSION, false);
        history=options.getBoolean(Options.PREF_HISTORY, true);*/

        this.update();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position=
            ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        if (item==deleteContextMenuItem) {
            FileListEntry entry=this.fileList.get(position);
            if (entry.type==FileListEntry.NORMAL&&!entry.isDirectory) {
                entry.file.delete();
                update();
            }
            return true;
        } else if (item==removeContextMenuItem) {
            FileListEntry entry=this.fileList.get(position);
        } else if (item==setAsHomeContextMenuItem) {
            setAsHome();
        }
        return false;
    }

    private boolean isHome(String path) {
        File pathFile=new File(path);
        File homeFile=new File(getHome());
        try {
            return pathFile.getCanonicalPath().equals(homeFile.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v==this.filesListView) {
            AdapterView.AdapterContextMenuInfo info=(AdapterView.AdapterContextMenuInfo) menuInfo;

            if (info.position<0)
                return;

            FileListEntry entry=this.fileList.get(info.position);

            if (entry.type==FileListEntry.HOME) {
                setAsHomeContextMenuItem=menu.add(R.string.set_as_home);
            } else if (entry.type==FileListEntry.RECENT) {
                //removeContextMenuItem=menu.add(R.string.remove_from_recent);
            } else if (!entry.isDirectory) {
                deleteContextMenuItem=menu.add(R.string.delete);
            }
        }
    }
}
