package coding.quizzaciously.nitpchanakyaunofficial;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import coding.quizzaciously.nitpchanakyaunofficial.datahandler.SubjectAttendanceValue;
import coding.quizzaciously.nitpchanakyaunofficial.datahandler.processors.AttendanceTableProcessor;
import coding.quizzaciously.nitpchanakyaunofficial.httprequesthelper.AttendanceLoader;
import coding.quizzaciously.nitpchanakyaunofficial.httprequesthelper.MarksError;
import coding.quizzaciously.nitpchanakyaunofficial.httprequesthelper.Progressable;
import nitpchankyaunofficial.R;

public class AttendanceActivity extends AppCompatActivity {


    volatile String cookie, value;
    @BindView(R.id.attendance_ad)
    AdView attendanceAd;
    private boolean expandForFirstTime = true;
    @BindView(R.id.getSession)
    Spinner session;
    @BindView(R.id.expandable_list)
    ExpandableListView expandableListView;
    @BindView(R.id.update_time)
    TextView timeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        ButterKnife.bind(this);
        MobileAds.initialize(this, "ca-app-pub-6905676924902031~3315860304");
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).addTestDevice("E13A0941A9BD375F6073D614426FC721").addTestDevice("7106C756201B730736FDD9855AB08A38").build();
        attendanceAd.loadAd(adRequest);
        cookie = getIntent().getStringExtra(getString(R.string.cookie));
        GetOptionsTask getOptionsTask = new GetOptionsTask();
        SharedPreferences sharedPreferences = getSharedPreferences("User Details", MODE_PRIVATE);
        value = sharedPreferences.getString("Attendance Session", null);

        final DisplayResultTask drs = new DisplayResultTask(null);
        try {
            if (value != null)
                drs.execute(getFileContents().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (getIntent().getBooleanExtra("isOnline", true)) {
            if (cookie == null) {
                final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).setTitle("Error").setMessage("Something unexpected has occured").create();
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        alertDialog.hide();

                    }
                });
                alertDialog.show();
            } else getOptionsTask.execute(value);
        }


    }

    private class GetOptionsTask extends AsyncTask<String, Integer, ArrayList> implements Progressable {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(AttendanceActivity.this);
            progressDialog.setMessage("Loading latest attendance from Chanakya");
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected ArrayList doInBackground(String... params) {
            AttendanceLoader attendanceLoader = new AttendanceLoader(AttendanceActivity.this, this);
            ArrayList session = new ArrayList();
            try {
                String[] sessions = attendanceLoader.accessOptions(cookie, params[0]);
                for (String s : sessions) {
                    session.add(s);
                }
                return session;
            } catch (MarksError marksError) {
                marksError.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressDialog.incrementProgressBy(values[0]);
        }

        @Override
        protected void onPostExecute(ArrayList s) {
            if(s!=null) {
                String[] output = new String[s.size()];
                for (int i = 0; i < s.size(); i++) {
                    output[i] = (String) s.get(i);
                }
                writeToOptions(output);
                DisplayResultTask displayResultTask = new DisplayResultTask(progressDialog);
                try {
                    StringBuilder text = getFileContents();
                    displayResultTask.execute(text.toString());
                } catch (IOException e) {
                    if (value == null)
                        return;
                    final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(AttendanceActivity.this).setTitle("Error").setMessage("Error in reading file. Make sure app can read its internal files properly.").create();
                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Retry", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            alertDialog.hide();
                            new GetOptionsTask().execute(cookie);

                        }
                    });
                    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            alertDialog.hide();
                        }
                    });
                    alertDialog.show();
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void progress() {
            publishProgress(15);
        }
    }

    @NonNull
    private StringBuilder getFileContents() throws IOException {
        File file = new File(AttendanceActivity.this.getFilesDir(), "attendance_temp.html");
        FileReader reader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String str;
        StringBuilder text = new StringBuilder();
        long longT = Long.parseLong(bufferedReader.readLine());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(longT);
        int sec = calendar.get(Calendar.SECOND);
        int min = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        mMonth++;
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        String timeStr = String.format(Locale.ENGLISH, "Last updated on %2d/%2d/%4d at %02d:%02d:%02d", mDay, mMonth, mYear, hour, min, sec);
        timeView.setText(timeStr);

        while ((str = bufferedReader.readLine()) != null) {
            text.append(str);
        }
        return text;
    }

    private class DisplayResultTask extends AsyncTask<String, Integer, ArrayList> {
        ProgressDialog pd;
        AttendanceExpandableListAdapter myAdapter = null;

        public DisplayResultTask(ProgressDialog progressDialog) {
            pd = progressDialog;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList doInBackground(String... params) {
            AttendanceTableProcessor mtp = new AttendanceTableProcessor();
            ArrayList<SubjectAttendanceValue> subjectAttendanceValues = new ArrayList<SubjectAttendanceValue>();
            try {
                subjectAttendanceValues = mtp.fetchFromTable(params[0]);
            } catch (MarksError marksError) {
                if (marksError.getType() == MarksError.WRONG_SESSION_ERROR)
                    return null;
            }
            publishProgress(15);
            myAdapter = new AttendanceExpandableListAdapter(AttendanceActivity.this, subjectAttendanceValues);
            publishProgress(10);
            return new ArrayList();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (pd != null)
                pd.incrementProgressBy(values[0]);
        }

        @Override
        protected void onPostExecute(ArrayList arrayList) {
            if (arrayList == null) {
                if (pd != null) {
                    final AlertDialog alertDialog = new AlertDialog.Builder(AttendanceActivity.this).setTitle("Error").setMessage("Attendance for the required session " + value + " not found.").create();

                    alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            alertDialog.hide();

                        }
                    });
                    alertDialog.show();
                    pd.hide();
                }
                return;
            }
            super.onPostExecute(arrayList);
            if (pd != null)
                pd.hide();
            expandableListView.setAdapter(myAdapter);
            expandableListView.refreshDrawableState();
        }
    }

    private void writeToOptions(final String[] sessions) {
        if (expandForFirstTime) {
            final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sessions);
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
            session.setAdapter(spinnerArrayAdapter);
            spinnerArrayAdapter.notifyDataSetChanged();
        }
        for (int i = 0; i < session.getAdapter().getCount(); i++) {
            if (value == null) {
                session.setSelection(0);
                break;
            }
            if (expandForFirstTime && value.equals(session.getAdapter().getItem(i).toString())) {
                session.setSelection(i);
                break;
            }
        }
        session.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences sharedPreferences = getSharedPreferences("User Details", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (value == null) {
                    editor.putString("Attendance Session", session.getAdapter().getItem(position).toString());
                    editor.commit();
                }
                value = session.getAdapter().getItem(position).toString();
                if (!value.equals(sharedPreferences.getString("Attendance Session", null))) {
                    editor.putString("Attendance Session", value);
                    String value = (String) session.getAdapter().getItem(position);
                    GetOptionsTask getOptionsTask = new GetOptionsTask();
                    getOptionsTask.execute(value);
                }
                editor.commit();
                expandForFirstTime = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        session.refreshDrawableState();
        // set selection to the 0th or the position where what we have passed stands session.setSelection(2);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        attendanceAd.destroy();
    }

    @Override
    public void onBackPressed() {
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        if (getIntent().getBooleanExtra("isOnline", true))
            mainActivityIntent.putExtra(getString(R.string.cookie), cookie);
        startActivity(mainActivityIntent);
        finish();
    }
}