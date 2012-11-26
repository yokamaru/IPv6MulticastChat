package jp.naist.inet_lab.android.ipv6multicastchat;

import java.io.UnsupportedEncodingException;

import jp.naist.inet_lab.android.ipv6multicast.MulticastException;
import jp.naist.inet_lab.android.ipv6multicast.MulticastManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity {

    /**
     * A handle-name
     */
    protected String name;
    /**
     * An address of multicast group
     */
    protected String groupAddress;
    /**
     * A port number which bind to/send to
     */
    protected int portNumber;

    /**
     * Manage communication over multicast
     */
    protected MulticastManager multicastManager;

    /**
     * Thread to receive the message
     */
    protected Thread receiver;

    protected Handler handler;

    protected Button buttonSend;
    protected EditText editMessage;
    protected TextView textChatLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Handler for touching GUI from inner thread
        handler = new Handler();

        editMessage = (EditText) this.findViewById(R.id.editMessage);
        textChatLog = (TextView) this.findViewById(R.id.textChatLog);

        buttonSend = (Button) this.findViewById(R.id.buttonSend);
        buttonSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String message = editMessage.getText().toString();
                if (!message.isEmpty()) {
                    message = name + " > " + message;
                    sendMessage(message);
                    editMessage.setText("");
                }
            }

        });

        Intent intent = getIntent();
        if (intent != null) {
            this.name = intent.getStringExtra("name");
            this.groupAddress = intent.getStringExtra("group_address");
            this.portNumber = intent.getIntExtra("port_number", 54321);
        }

        multicastManager = new MulticastManager();
    }

    protected void onResume() {
        super.onResume();

        this.joinGroup();
        this.startReceiveMessage();
    }

    protected void onPause() {
        super.onPause();

        this.leaveGroup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_chat, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuLeave:
                this.leaveGroup();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ChatActivity.this.finish();
                    }
                });
        }

        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.leaveGroup();
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Join the multicast group
     */
    protected void joinGroup() {
        // Enable the multicast, and then join the multicast group.
        Thread join = new Thread(new Runnable() {
            @Override
            public void run() {
                multicastManager.enableMulticastOnWifi(getApplicationContext(),
                        getString(R.string.app_name));

                try {
                    multicastManager.join(groupAddress, portNumber);
                } catch (MulticastException e) {
                    // When an error is occured, toast error and finish this
                    // activity.
                    showToastFromThread("Faild to join the group.",
                            Toast.LENGTH_LONG);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ChatActivity.this.finish();
                        }
                    });
                }
            }
        });
        join.start();
    }

    /**
     * Leave the multicast group
     */
    protected void leaveGroup() {
        // Leave the multicast group, and then disable the mulricast on WiFi
        // interface.
        Thread leave = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    multicastManager.leave();
                    multicastManager.disableMulticastOnWifi();
                } catch (MulticastException e) {
                    // When an error is occured, toast a message.
                    showToastFromThread("Faild to leave the group.",
                            Toast.LENGTH_LONG);
                }
            }
        });
        leave.start();
    }

    /**
     * Send the message to the joined multicast group
     * 
     * @param message
     *            A message to be sent
     */
    protected void sendMessage(final String message) {
        Thread send = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    multicastManager.sendData(message.getBytes(), portNumber);
                } catch (MulticastException e) {
                    showToastFromThread("Faild to send the message.",
                            Toast.LENGTH_LONG);
                }
            }
        });
        send.start();
    }

    protected void startReceiveMessage() {
        receiver = new Thread(new Runnable() {

            @Override
            public void run() {
                while (multicastManager.isJoined()) {
                    try {
                        // FIXME: Hard-coded buffer size
                        final String message = new String(
                                multicastManager.receiveData(1024), "UTF-8");
                        appendChatLog(message);
                    } catch (UnsupportedEncodingException e) {
                        showToastFromThread("Faild to decode the message.",
                                Toast.LENGTH_LONG);
                    } catch (MulticastException e) {
                        showToastFromThread("Faild to receive the message.",
                                Toast.LENGTH_LONG);
                    }
                }
            }

        });
        receiver.start();
    }

    /**
     * Append specified message to the chat log area
     * 
     * @param message
     */
    protected void appendChatLog(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                textChatLog.append(message + "\n");
            }
        });
    }

    /**
     * Show toast message from outside of the UI thread
     * 
     * @param text
     * @param duration
     */
    protected void showToastFromThread(final String text, final int duration) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, duration).show();
            }
        });
    }
}