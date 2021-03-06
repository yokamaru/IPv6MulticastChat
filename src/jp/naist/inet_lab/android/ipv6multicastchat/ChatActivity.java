package jp.naist.inet_lab.android.ipv6multicastchat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import jp.naist.inet_lab.android.ipv6multicast.MulticastException;
import jp.naist.inet_lab.android.ipv6multicast.MulticastManager;
import jp.naist.inet_lab.android.ipv6multicast.MulticastManager.ReceivedData;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
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

        // If the user expressly leave the group, it is unnecessary to leave the
        // group. So we should confirm that the application currently joined or
        // not.
        if (this.multicastManager.isJoined()) {
            this.leaveGroup();
        }
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
                // Leave the group expressly
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

    /**
     * Join the multicast group
     */
    protected void joinGroup() {
        multicastManager.enableMulticastOnWifi(getApplicationContext(),
                getString(R.string.app_name));
        try {
            multicastManager.join(groupAddress, portNumber);
            showToastFromThread(getString(R.string.alert_join_success) + " "
                    + groupAddress, Toast.LENGTH_SHORT);
        } catch (MulticastException e) {
            showToastFromThread(getString(R.string.alert_join_failed),
                    Toast.LENGTH_LONG);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ChatActivity.this.finish();
                }
            });
        }
    }

    /**
     * Leave the multicast group
     */
    protected void leaveGroup() {
        // Leave the multicast group, and then disable the mulricast on WiFi
        // interface.
        try {
            multicastManager.leave();
            multicastManager.disableMulticastOnWifi();
            showToastFromThread(getString(R.string.alert_leave_success) + " "
                    + groupAddress, Toast.LENGTH_SHORT);
        } catch (MulticastException e) {
            // When an error is occured, toast a message.
            showToastFromThread(getString(R.string.alert_leave_failed),
                    Toast.LENGTH_LONG);
        }
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
                    showToastFromThread(getString(R.string.alert_send_failed),
                            Toast.LENGTH_LONG);
                }
            }
        });
        send.start();
    }

    protected void startReceiveMessage() {
        InetAddress groupAddressAsInetAddr = null;

        try {
            groupAddressAsInetAddr = InetAddress.getByName(this.groupAddress);

            multicastManager.startReceiver(groupAddressAsInetAddr, 1024, false,
                    new MulticastManager.Receiver() {
                        @Override
                        public void run(ReceivedData receivedData) {
                            String message = new String(receivedData.buffer,
                                    Charset.forName("UTF-8"));
                            appendChatLog(message);
                        }
                    });
        } catch (UnknownHostException e) {
            showToastFromThread(getString(R.string.alert_join_failed),
                    Toast.LENGTH_LONG);
        } catch (MulticastException e) {
            showToastFromThread(getString(R.string.alert_join_failed),
                    Toast.LENGTH_LONG);
        }
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