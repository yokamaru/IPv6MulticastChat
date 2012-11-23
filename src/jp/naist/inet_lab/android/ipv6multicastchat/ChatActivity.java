package jp.naist.inet_lab.android.ipv6multicastchat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jp.naist.inet_lab.android.ipv6multicast.MulticastException;
import jp.naist.inet_lab.android.ipv6multicast.MulticastManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity {

    /**
     * Port number which bind to/send to
     */
    protected int PORT_NUMBER = 32100;

    /**
     * A handle-name
     */
    protected String name;
    /**
     * An address of multicast group
     */
    protected String groupAddress;

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
                }
            }

        });

        Intent intent = getIntent();
        if (intent != null) {
            this.name = intent.getStringExtra("name");
            this.groupAddress = intent.getStringExtra("group_address");

            this.joinGroup();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_chat, menu);
        return true;
    }

    /**
     * Join the multicast group
     */
    protected void joinGroup() {
        multicastManager = new MulticastManager();

        try {
            multicastManager.join(this.groupAddress, this.PORT_NUMBER);
        } catch (MulticastException e) {
            Toast.makeText(this, "Faild to join the group.", Toast.LENGTH_LONG)
                    .show();
            this.finish();
        }
    }

    /**
     * Leave the multicast group
     */
    protected void leaveGroup() {
        try {
            multicastManager.leave();
        } catch (MulticastException e) {
            Toast.makeText(this, "Faild to leave the group.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Send the message to the joined multicast group
     * 
     * @param message
     *            A message to be sent
     */
    protected void sendMessage(String message) {

        try {
            this.multicastManager
                    .sendData(message.getBytes(), this.PORT_NUMBER);
        } catch (MulticastException e) {
            Toast.makeText(this, "Faild to send the group.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    protected void startReceiveMessage() {
        receiver = new Thread(new Runnable() {

            @Override
            public void run() {
                while (multicastManager.isJoined()) {
                    try {
                        final String message = new String(
                        // FIXME: Hard-coded buffer size
                                multicastManager.receiveData(1024), "UTF-8");

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                textChatLog.append(message + "\n");
                            }
                        });
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (MulticastException e) {
                        Toast.makeText(getApplicationContext(),
                                "Faild to receive the message.", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }

        });
        receiver.start();
    }
}