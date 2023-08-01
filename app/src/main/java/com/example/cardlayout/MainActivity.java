package com.example.cardlayout;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SeekBar sbOutOffset=findViewById(R.id.sb_out_offset);
        StackLayout myLayout=findViewById(R.id.my_layout);
        myLayout.setAdapter(new StackAdapter() {
            final int[] bgColors=new int[]{Color.RED,Color.GREEN,Color.BLUE};
            @Override
            public View getView(int position, LayoutInflater inflater, ViewGroup viewGroup) {

                return inflater.inflate(R.layout.demo_item,viewGroup,false);
            }

            @Override
            public int getVisibleCount() {
                return 3;
            }

            @Override
            public int getCount() {
                return 10;
            }

            @Override
            public void bindData(View view, int position) {

                TextView tv= (TextView) view;
                tv.setText("数据:"+position);
                tv.setBackgroundColor(bgColors[position%bgColors.length]);
            }
        });
        sbOutOffset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float offset=progress*1.0f/100;
                myLayout.setOutOffset(offset);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}