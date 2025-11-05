package com.example.lotteryevent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RunLotteryActivity extends AppCompatActivity {
    private LotteryManager lotteryManager;
    private EditText numSelectedEntrants;
    private String eventId = "temporary filler for event ID";

    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_event);

        this.lotteryManager  = new LotteryManager();
        this.numSelectedEntrants = findViewById(R.id.numSelectedEntrants);

        Button runDrawButton = this.findViewById(R.id.runDrawButton);
        Button cancelButton = this.findViewById(R.id.cancelButton);

        runDrawButton.setOnClickListener(v -> {
            String inputText = this.numSelectedEntrants.getText().toString().trim();

            if (inputText.isEmpty()) {
                Toast.makeText(this, "Please enter a number", Toast.LENGTH_SHORT).show();
                return;
            }
            int numToSelect = Integer.parseInt(inputText);

            //Fetch real eventId
            this.lotteryManager.selectWinners(this.eventId, numToSelect);

            Toast.makeText(this, "Draw initalized!", Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> this.finish());
    }

}
