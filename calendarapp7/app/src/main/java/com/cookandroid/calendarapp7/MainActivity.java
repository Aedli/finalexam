package com.cookandroid.calendarapp7;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private EditText editTextTask;
    private Button buttonAdd;
    private ListView listViewTasks;
    private ListView listViewSelectedDateTasks;
    private HashMap<String, ArrayList<String>> taskMap;
    private ArrayList<String> datesWithTasks;
    private String selectedDate;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendarView = findViewById(R.id.calendarView);
        editTextTask = findViewById(R.id.editTextTask);
        buttonAdd = findViewById(R.id.buttonAdd);
        listViewTasks = findViewById(R.id.listViewTasks);
        listViewSelectedDateTasks = findViewById(R.id.listViewSelectedDateTasks);

        sharedPreferences = getSharedPreferences("ToDoListApp", Context.MODE_PRIVATE);

        loadTaskMap();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(calendarView.getDate());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            loadTasksForSelectedDate(selectedDate);
        });

        buttonAdd.setOnClickListener(v -> {
            String task = editTextTask.getText().toString();
            if (!task.isEmpty()) {
                if (!taskMap.containsKey(selectedDate)) {
                    taskMap.put(selectedDate, new ArrayList<>());
                }
                taskMap.get(selectedDate).add(task);
                saveTaskMap();
                loadTasksForSelectedDate(selectedDate);
                editTextTask.setText("");
                updateDatesWithTasks();
            }
        });

        listViewTasks.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>()));
        ArrayAdapter<String> datesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, datesWithTasks);
        listViewSelectedDateTasks.setAdapter(datesAdapter);

        listViewSelectedDateTasks.setOnItemClickListener((parent, view, position, id) -> {
            String date = datesWithTasks.get(position);
            loadTasksForSelectedDate(date);
        });
    }

    private void loadTasksForSelectedDate(String selectedDate) {
        ArrayList<String> tasksForSelectedDate = taskMap.get(selectedDate);
        if (tasksForSelectedDate == null) {
            tasksForSelectedDate = new ArrayList<>();
        }
        TaskAdapter adapter = new TaskAdapter(this, android.R.layout.simple_list_item_1, tasksForSelectedDate);
        listViewTasks.setAdapter(adapter);
    }

    private void saveTaskMap() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        for (Map.Entry<String, ArrayList<String>> entry : taskMap.entrySet()) {
            String key = entry.getKey();
            Set<String> taskSet = new HashSet<>(entry.getValue());
            editor.putStringSet(key, taskSet);
        }
        editor.apply();
    }

    private void loadTaskMap() {
        taskMap = new HashMap<>();
        datesWithTasks = new ArrayList<>();
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Set<String> taskSet = sharedPreferences.getStringSet(key, null);
            if (taskSet != null) {
                ArrayList<String> tasks = new ArrayList<>(taskSet);
                taskMap.put(key, tasks);
                if (!tasks.isEmpty()) {
                    datesWithTasks.add(key);
                }
            }
        }
    }

    private void updateDatesWithTasks() {
        datesWithTasks.clear();
        for (String date : taskMap.keySet()) {
            if (!taskMap.get(date).isEmpty()) {
                datesWithTasks.add(date);
            }
        }
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) listViewSelectedDateTasks.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showEditDeleteDialog(String selectedDate, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("수정 또는 삭제");

        String[] options = {"수정", "삭제"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showEditTaskDialog(selectedDate, position);
            } else if (which == 1) {
                ArrayList<String> tasks = taskMap.get(selectedDate);
                if (tasks != null) {
                    tasks.remove(position);
                    saveTaskMap();
                    loadTasksForSelectedDate(selectedDate);
                    updateDatesWithTasks();
                }
            }
        });

        builder.show();
    }

    private void showEditTaskDialog(String selectedDate, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("할 일 수정");

        final EditText input = new EditText(this);
        input.setText(taskMap.get(selectedDate).get(position));
        builder.setView(input);

        builder.setPositiveButton("저장", (dialog, which) -> {
            String updatedTask = input.getText().toString();
            if (!updatedTask.isEmpty()) {
                taskMap.get(selectedDate).set(position, updatedTask);
                saveTaskMap();
                loadTasksForSelectedDate(selectedDate);
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private class TaskAdapter extends ArrayAdapter<String> {
        private final Pattern urlPattern = Patterns.WEB_URL;

        public TaskAdapter(Context context, int resource, ArrayList<String> tasks) {
            super(context, resource, tasks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = (TextView) super.getView(position, convertView, parent);
            String task = getItem(position);
            SpannableString spannableString = new SpannableString(task);

            Matcher matcher = urlPattern.matcher(task);
            while (matcher.find()) {
                String url = matcher.group();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
                spannableString.setSpan(new URLSpan(url), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            textView.setText(spannableString);
            textView.setMovementMethod(LinkMovementMethod.getInstance());

            textView.setOnLongClickListener(v -> {
                showEditDeleteDialog(selectedDate, position);
                return true;
            });

            return textView;
        }
    }
}