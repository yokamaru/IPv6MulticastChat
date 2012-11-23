package jp.naist.inet_lab.android.ipv6multicastchat;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

    private Button buttonJoin;
    private EditText editName;
    private EditText editGroupAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // A form for input the handle-name
        this.editName = (EditText) this.findViewById(R.id.editName);

        // A form for input the multicast group address
        this.editGroupAddress = (EditText) this
                .findViewById(R.id.editGroupAddress);

        // A button for join the multicast group
        this.buttonJoin = (Button) this.findViewById(R.id.buttonJoin);
        this.buttonJoin.setOnClickListener(new OnClickListener() {

            @Override
            /*
             * Start the ChatActivity
             */
            public void onClick(View v) {
                // FIXME: Validate the input(check the name and the address are
                // valid)
                String name = editName.getText().toString();
                String groupAddress = editGroupAddress.getText().toString();

                // Start the ChatAcrivity
                Intent intentToChatActivity = new Intent(
                        getApplicationContext(), ChatActivity.class);
                intentToChatActivity.putExtra("name", name);
                intentToChatActivity.putExtra("group_address", groupAddress);
                startActivity(intentToChatActivity);
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

}
