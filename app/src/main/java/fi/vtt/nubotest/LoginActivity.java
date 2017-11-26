/*
 * (C) Copyright 2016 VTT (http://www.vtt.fi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package fi.vtt.nubotest;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.List;

import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubotest.util.Constants;

/**
 * Login Activity for the first time the app is opened, or when a user clicks the sign out button.
 * Saves the username in SharedPreferences.
 */
public class LoginActivity extends AppCompatActivity {
    private String TAG = "LoginActivity";
    private EditText mUsername, mRoomname;
    private Context context;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        context = this;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mUsername = (EditText) findViewById(R.id.username);
        mRoomname = (EditText) findViewById(R.id.roomname);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(context, PreferencesActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_encoder) {
            Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("Encoder")
                .setItems(new String[]{"VP8", "VP9", "H264"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PeerVideoActivity.CODEC = NBMMediaConfiguration.NBMVideoCodec.values()[which];
                }
            }).create();
            dialog.show();
            return true;
        }

        if (id == R.id.action_fps) {
            RelativeLayout linearLayout = new RelativeLayout(this);
            final NumberPicker aNumberPicker = new NumberPicker(this);
            aNumberPicker.setMaxValue(60);
            aNumberPicker.setMinValue(1);
            aNumberPicker.setValue(PeerVideoActivity.FPS);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
            RelativeLayout.LayoutParams numPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            numPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            linearLayout.setLayoutParams(params);
            linearLayout.addView(aNumberPicker,numPicerParams);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("FPS");
            alertDialogBuilder.setView(linearLayout);
            alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int id) {
                            PeerVideoActivity.FPS = aNumberPicker.getValue();

                        }
                    })
                .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return true;
        }

        if (id == R.id.action_resolution) {
            Camera camera = Camera.open(1);
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
            String[] names = new String[sizeList.size()];
            final int[] heights = new int[sizeList.size()];
            final int[] widths = new int[sizeList.size()];
            for (int i = 0; i < sizeList.size(); i++) {
                Camera.Size size = sizeList.get(i);
                heights[i] = size.height;
                widths[i] = size.width;
                names[i] = size.width + " x " + size.height;
            }
            Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("Resolution")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    PeerVideoActivity.HEIGHT = heights[which];
                    PeerVideoActivity.WIDTH = widths[which];
                    }
                }).create();
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    };

    /**
     * Takes the username from the EditText, check its validity and saves it if valid.
     *   Then, redirects to the MainActivity.
     * @param view Button clicked to trigger call to joinChat
     */
    public void joinRoom(View view){
        String username = mUsername.getText().toString();
        String roomname = mRoomname.getText().toString();
        if (!validUsername(username) || !validRoomname(roomname))
            return;

        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString(Constants.USER_NAME, username);
        edit.putString(Constants.ROOM_NAME, roomname);
        edit.apply();

        Intent intent = new Intent(context, MainActivity.class);
        startActivity(intent);
    }

    public void showToast(String string) {
        try {
            Toast toast = Toast.makeText(this, string, Toast.LENGTH_SHORT);
            toast.show();
        }
        catch (Exception e){e.printStackTrace();}
    }

    /**
     * Optional function to specify what a username in your chat app can look like.
     * @param username The name entered by a user.
     * @return is username valid
     */
    private boolean validUsername(String username) {
        if (username.length() == 0) {
            mUsername.setError("Username cannot be empty.");
            return false;
        }
        if (username.length() > 16) {
            mUsername.setError("Username too long.");
            return false;
        }
        return true;
    }

    /**
     * Optional function to specify what a username in your chat app can look like.
     * @param roomname The name entered by a user.
     * @return is username valid
     */
    private boolean validRoomname(String roomname) {
        if (roomname.length() == 0) {
            mRoomname.setError("Roomname cannot be empty.");
            return false;
        }
        if (roomname.length() > 16) {
            mRoomname.setError("Roomname too long.");
            return false;
        }
        return true;
    }
}
