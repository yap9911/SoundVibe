package com.example.soundvibe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;

public class SearchActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SongsListAdapter adapter;
    private ImageButton backButton;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize RecyclerView and adapter
        recyclerView = findViewById(R.id.SongsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = MainActivity.adapter;
        recyclerView.setAdapter(adapter);

        // Initialize views
        backButton = findViewById(R.id.backButton);
        searchView = findViewById(R.id.searchView);

        // Set up click listener for the back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed(); // This will trigger the onBackPressed method
                adapter.resetData();
            }
        });

        // Set up search view listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // We don't need to handle this
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter the adapter based on the new text
                adapter.getFilter().filter(newText);

                // If the query is empty, reset the data
                if (TextUtils.isEmpty(newText)) {
                    adapter.resetData();
                }
                return true;
            }
        });
    }
}