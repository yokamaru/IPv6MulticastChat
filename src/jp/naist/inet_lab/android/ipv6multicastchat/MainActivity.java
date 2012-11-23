package jp.naist.inet_lab.android.ipv6multicastchat;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

    private Button buttonJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                // Start the ChatAcrivity

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
