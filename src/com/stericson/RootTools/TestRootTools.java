package com.stericson.RootTools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class TestRootTools extends Activity
{
    private ScrollView mScrollView;
    private TextView mTextView;
    private ProgressDialog mPDialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mTextView = new TextView(this);
        mTextView.setText("");
        mScrollView = new ScrollView(this);
        mScrollView.addView(mTextView);
        setContentView(mScrollView);

        String version = "?";
        try {
            PackageInfo packageInfo =
                    this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            version = packageInfo.versionName;
        }
        catch(PackageManager.NameNotFoundException e) {}

        print("TestRootTools v " + version + "\n\n");
        if(false == RootTools.isAccessGiven()) {
            print("ERROR: No root access to this device.\n");
            return;
        }

        mPDialog = new ProgressDialog(this);
        mPDialog.setCancelable(false);
        mPDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        new TestThread(new TestHandler()).start();
    }

    protected void print(CharSequence text) {
        mTextView.append(text);
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private class TestThread extends Thread {
        private Handler handler;

        public TestThread(Handler handler) {
            this.handler = handler;
        }

        public void run() {
            visualUpdate(TestHandler.ACTION_SHOW, null);

            try {
                List<String> response = RootTools.sendShell("ls /");
                visualUpdate(TestHandler.ACTION_DISPLAY, "[ Listing of / (passing a List)]\n");
                for(String line:response) {
                    visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");
                }
            } catch (IOException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
                return;
            } catch (InterruptedException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
                return;
            } catch (RootTools.RootToolsException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
                return;
            }

            try {
                visualUpdate(TestHandler.ACTION_DISPLAY, "\n[ Listing of / (callback)]\n");
                RootTools.Result result = new RootTools.Result() {
                    @Override
                    public void process(String line) throws Exception {
                        visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + ex);
                        setError(1);
                    }

                    @Override
                    public void onComplete(int diag) {
                        visualUpdate(TestHandler.ACTION_DISPLAY, "------\nDone.\n");
                    }
                };
                RootTools.sendShell("ls /", result);
                if(0 != result.getError())
                    return;
            } catch (IOException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
                return;
            } catch (InterruptedException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
                return;
            } catch (RootTools.RootToolsException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
                return;
            }

            try {
                visualUpdate(TestHandler.ACTION_DISPLAY, "\n[ ps + ls + date / (callback)]\n");
                RootTools.Result result = new RootTools.Result() {
                    @Override
                    public void process(String line) throws Exception {
                        visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + ex);
                        setError(1);
                    }

                    @Override
                    public void onComplete(int diag) {
                        visualUpdate(TestHandler.ACTION_DISPLAY, "------\nDone.\n");
                    }

                };
                RootTools.sendShell(
                    new String[] {
                            "echo \"* PS:\"",
                            "ps",
                            "echo \"* LS:\"",
                            "ls",
                            "echo \"* DATE:\"",
                            "date" },
                    2000,
                    result
                );
                if(0 != result.getError())
                    return;
            } catch (IOException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
            } catch (InterruptedException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
            } catch (RootTools.RootToolsException e) {
                visualUpdate(TestHandler.ACTION_DISPLAY, "ERROR: " + e);
            }

            visualUpdate(TestHandler.ACTION_HIDE, null);
        }

        private void visualUpdate(int action, String text) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putInt(TestHandler.ACTION, action);
            bundle.putString(TestHandler.TEXT, text);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    private class TestHandler extends Handler {
        static final public String ACTION = "action";
            static final public int ACTION_SHOW 	= 0x01;
            static final public int ACTION_HIDE 	= 0x02;
            static final public int ACTION_DISPLAY 	= 0x03;
        static final public String TEXT   = "text";

        public void handleMessage(Message msg) {
            int action = msg.getData().getInt(ACTION);
            String text   = msg.getData().getString(TEXT);

            switch(action) {
                case ACTION_SHOW:
                    mPDialog.show();
                    mPDialog.setMessage("Running Root Library Tests...");
                    break;
                case ACTION_HIDE:
                    mPDialog.hide();
                    break;
                case ACTION_DISPLAY:
                    print(text);
                    break;
            }
        }
    }
}
