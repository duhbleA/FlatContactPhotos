package duhblea.me.flatcontactphotos;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;


public class MainActivity extends AppCompatActivity {

    private int currentColor = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                getContactsFromUser();
            }
        });

        Button button = (Button) findViewById(R.id.startButton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thread.start();
            }
        });
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Subroutine to query and populate all the contacts from the user
     */
    private void getContactsFromUser() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor contactCursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);
            if (contactCursor != null) {
                if (contactCursor.getCount() > 0) {
                    while (contactCursor.moveToNext()) {

                        Long contactId = contactCursor.getLong(contactCursor.getColumnIndex(ContactsContract.Contacts._ID));
                        String contactName = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));

                        if (contactId != null && contactName != null) {
                            byte[] photoBytes = getContactPhoto(contactName);

                            if (photoBytes != null) {
                                ContentValues values = new ContentValues();
                                int photoRow = -1;
                                String where = ContactsContract.Data.CONTACT_ID + " = " + contactId + " AND " + ContactsContract.Data.MIMETYPE + "=='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
                                Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, where, null, null);
                                Integer idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
                                if (cursor.moveToFirst()) {
                                    photoRow = cursor.getInt(idIdx);
                                }
                                cursor.close();
                                values.put(ContactsContract.Data.RAW_CONTACT_ID, contactId);
                                values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
                                values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes);
                                values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

                                if (photoRow >= 0) {
                                    getContentResolver().update(ContactsContract.Data.CONTENT_URI, values, ContactsContract.Data._ID + " = " + photoRow, null);
                                } else {
                                    getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
                                }
                            }
                        }
                    }
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Contact photos successfully created", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
        catch (Exception e) {
            Log.e(MainActivity.class.getSimpleName(), "There was an error.");
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "There was an error. Check to make sure" +
                            "the app has Contacts permission.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     *
     * @param contactName Get the name of the user to determine what letter
     *                    the contact photo should be
     * @return bytesOfPhoto
     */
    private byte[] getContactPhoto(String contactName) {

        byte[] retVal = null;

        //Reset counter
        if (currentColor > 33) {
            currentColor = 1;
        }

        try {
            InputStream readPhoto = getAssets().open(Integer.toString(currentColor) + "/" + contactName.charAt(0) + ".jpg");

            retVal = IOUtils.toByteArray(readPhoto);

            currentColor++;
        }
        catch (Exception e)
        {
            Log.e(MainActivity.class.getSimpleName(), "There was an error");
        }


        return retVal;
    }
}

