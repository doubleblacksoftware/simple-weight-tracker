package com.doubleblacksoftware.tracker;

/**
 * Created by x on 5/22/15.
 */

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ScrollDirectionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class list extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private String export_filename="weight_tracker.csv";
    public static String format = "M/dd/yy h:mm a";

    // Identifies a particular Loader being used in this component
    private static final int loaderID;

    static {
        loaderID = 0;
    }

    private MyAdapter adapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(loaderID, null, this);

        adapter =
                new MyAdapter(
                        getActivity(),                // Current context
                        null                   // No flags
                );

        setListAdapter(adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.list, container, false);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEntry();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            fab.setScaleX(0);
            fab.setScaleY(0);
            fab.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setStartDelay(500)
                    .start();
        }

        fab.attachToListView(getListView());

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
                final String _id = l + "";
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Action")
                        .setItems(R.array.row_actions, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                switch (which) {
                                    case 0: // edit
                                        dialog.dismiss();
                                        TextView valueLabel = (TextView) view.findViewById(R.id.textView2);
                                        String value = valueLabel.getText().toString();
                                        final EditText input = new EditText(getActivity());
                                        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                        input.setText(value);
                                        input.setSelection(value.length());
                                        AlertDialog editDialog = new AlertDialog.Builder(getActivity())
                                                .setTitle("Edit Value")
                                                .setView(input)
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        // Add a new record
                                                        Editable value = input.getText();
                                                        ContentValues values = new ContentValues();
                                                        values.put(ValueProvider.VALUE,
                                                                value.toString());
                                                        int numEdited = getActivity().getContentResolver().update(
                                                                ValueProvider.CONTENT_URI, values, ValueProvider.ID + "=?", new String[]{_id});
                                                        Toast.makeText(getActivity(),
                                                                numEdited + "  rows edited!", Toast.LENGTH_LONG).show();
                                                    }
                                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        // Do nothing.
                                                    }
                                                }).create();
                                        editDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                                        editDialog.show();
                                        break;
                                    case 1: // delete
                                        dialog.dismiss();
                                        new AlertDialog.Builder(getActivity())
                                                .setTitle("Delete Record?")
                                                .setMessage("The record will be permanently deleted.")
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        // Add a new record
                                                        int numDeleted = getActivity().getContentResolver().delete(
                                                                ValueProvider.CONTENT_URI, ValueProvider.ID + "=?", new String[]{_id});
                                                        Toast.makeText(getActivity(),
                                                                numDeleted + "  rows deleted!", Toast.LENGTH_LONG).show();
                                                    }
                                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                // Do nothing.
                                            }
                                        }).show();
                                        break;
                                }

                            }
                        });
                builder.create().show();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_export) {
            exportToSdCard();
            Toast.makeText(getActivity(),
                    "Exported to " + export_filename, Toast.LENGTH_LONG).show();
            return true;
        } else if (id == R.id.action_import) {
            importFromSdCard();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addEntry() {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        //LayoutInflater inflater = getActivity().getLayoutInflater();
        //final View dialogView = inflater.inflate(R.layout.dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("New Weight")
                .setView(input)
                //.setView(dialogView)
                .setNeutralButton(getResources().getString(R.string.add_with_current_timestamp), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Add a new record
                        Editable value = input.getText();
                        //Editable value = ((EditText)dialogView.findViewById(R.id.editText)).getText();
                        storeEntry(value.toString(), System.currentTimeMillis() / 1000L);
                    }
                })
                .setPositiveButton(getResources().getString(R.string.add_with_custom_date), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Add a new record
                        final Editable value = input.getText();
                        //Editable value = ((EditText)dialogView.findViewById(R.id.editText)).getText();
                        dialog.dismiss();
                        DatePickerFragment newFragment = DatePickerFragment.newInstance(new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, final int year, final int month, final int day) {
                                TimePickerFragment newFragment = TimePickerFragment.newInstance(new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                                        Calendar cal = Calendar.getInstance(Locale.US);
                                        cal.set(year, month, day, hourOfDay, minute);
                                        storeEntry(value.toString(), cal.getTimeInMillis() / 1000L);
                                    }
                                });
                                newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
                            }
                        });
                        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
                        //storeEntry(value.toString(), System.currentTimeMillis() / 1000L);
                    }
                })/*.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                })*/.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void storeEntry(String value, Long timestamp) {
        ContentValues values = new ContentValues();
        values.put(ValueProvider.VALUE, value);
        values.put(ValueProvider.TIMESTAMP, timestamp);
        getActivity().getContentResolver().insert(
                ValueProvider.CONTENT_URI, values);
    }

    public class MyAdapter extends CursorAdapter {
        private final LayoutInflater mInflater;

        public MyAdapter(Context context, Cursor cursor) {
            super(context, cursor, false);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.row_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // get the fields from the row
            TextView timestampLabel = (TextView) view.findViewById(R.id.textView3);
            TextView valueLabel = (TextView) view.findViewById(R.id.textView2);
            //ImageView edit = (ImageView) view.findViewById(R.id.edit);
            //ImageView delete= (ImageView) view.findViewById(R.id.delete);

            // set value label
            final String value = cursor.getString(cursor.getColumnIndex("value"));
            valueLabel.setText(value);

            // set the timestamp label
            long time = cursor.getLong(cursor.getColumnIndex("timestamp")) * 1000L;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String dateString = sdf.format(cal.getTime());
            timestampLabel.setText(dateString);

            // this is implemented in OnViewCreated now, as a dialog when a row is selected
            // instead of as ugly buttons
            // set up the buttons
            /*final String _id = cursor.getString(cursor.getColumnIndex("_id"));
            edit.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    input.setText(value);
                    input.setSelection(value.length());
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle("Edit Value")
                            .setView(input)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Add a new record
                                    Editable value = input.getText();
                                    ContentValues values = new ContentValues();
                                    values.put(ValueProvider.VALUE,
                                            value.toString());
                                    int numEdited = getActivity().getContentResolver().update(
                                            ValueProvider.CONTENT_URI, values, ValueProvider.ID + "=?", new String[]{_id});
                                    Toast.makeText(getActivity(),
                                            numEdited + "  rows edited!", Toast.LENGTH_LONG).show();
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).create();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    dialog.show();
                }
            });
            delete.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Delete Record?")
                            .setMessage("The record will be permanently deleted.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Add a new record
                                    int numDeleted = getActivity().getContentResolver().delete(
                                            ValueProvider.CONTENT_URI, ValueProvider.ID + "=?", new String[]{_id});
                                    Toast.makeText(getActivity(),
                                            numDeleted + "  rows deleted!", Toast.LENGTH_LONG).show();
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                }
            });*/
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle)
    {
        String[] projection = { "_id", "value", "timestamp" };

        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                ValueProvider.CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    /*
     * Moves the query results into the adapter, causing the
     * ListView fronting this adapter to re-display
     */
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    /*
     * Clears out the adapter's reference to the Cursor.
     * This prevents memory leaks.
     */
        adapter.swapCursor(null);
    }

    public void exportToSdCard() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath());
        File file = new File(dir, export_filename);
        FileOutputStream f;
        try {
            file.createNewFile();
            f = new FileOutputStream(file);
            Cursor cursor = getActivity().getContentResolver().query(
                    ValueProvider.CONTENT_URI, new String[] {
                            "value", "timestamp"
                    }, null, null, "timestamp");
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            cursor.moveToFirst();
            while (cursor.isAfterLast() == false) {
                long time = cursor.getLong(cursor.getColumnIndex("timestamp")) * 1000L;
                cal.setTimeInMillis(time);
                String timestamp = sdf.format(cal.getTime());
                String value = cursor.getString(cursor.getColumnIndex("value"));
                String line = timestamp + "," + value + "\n";
                f.write(line.getBytes());
                cursor.moveToNext();
            }
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importFromSdCard() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath());
        File file = new File(dir, export_filename);
        if (file.exists()) {
            FileInputStream f;
            try {
                f = new FileInputStream(file);
                final List<String[]> resultList = new ArrayList();
                BufferedReader reader = new BufferedReader(new InputStreamReader(f));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    resultList.add(row);
                }
                f.close();
                new AlertDialog.Builder(getActivity())
                        .setTitle("Import records?")
                        .setMessage("Found " + resultList.size() + " records to import.  This will delete any records already in the app.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all records in app
                                getActivity().getContentResolver().delete(
                                        ValueProvider.CONTENT_URI, null, null);
                                // Add all the new records
                                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                                for (String[] row : resultList) {
                                    try {
                                        storeEntry(row[1], dateFormat.parse(row[0]).getTime() / 1000);
                                    } catch (Exception e) {
                                        Toast.makeText(getActivity(),
                                                "Error: couldn't parse " + row[0], Toast.LENGTH_LONG).show();
                                    }
                                }
                                Toast.makeText(getActivity(),
                                        "Import succeeded!", Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Import Instructions")
                    .setMessage("Create a CSV file in the root of the SD card named " + export_filename + " containing a column with the date and time in format " + format + ", and a column with the weight.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing
                        }
                    }).show();
        }
    }

    public static class DatePickerFragment extends DialogFragment {
        private DatePickerDialog.OnDateSetListener onDateSetListener;

        static DatePickerFragment newInstance(DatePickerDialog.OnDateSetListener onDateSetListener) {
            DatePickerFragment pickerFragment = new DatePickerFragment();
            pickerFragment.setOnDateSetListener(onDateSetListener);
            return pickerFragment;
        }

        private void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
            this.onDateSetListener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), onDateSetListener, year, month, day);
        }
    }

    public static class TimePickerFragment extends DialogFragment {
        private TimePickerDialog.OnTimeSetListener onTimeSetListener;

        static TimePickerFragment newInstance(TimePickerDialog.OnTimeSetListener onTimeSetListener) {
            TimePickerFragment pickerFragment = new TimePickerFragment();
            pickerFragment.setOnTimeSetListener(onTimeSetListener);
            return pickerFragment;
        }

        private void setOnTimeSetListener(TimePickerDialog.OnTimeSetListener listener) {
            this.onTimeSetListener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), onTimeSetListener, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }
    }
}
