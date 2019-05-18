package com.cugkuan.flowlayout;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.cugkuan.flow.KFlowLayout;

public class MainActivity extends AppCompatActivity {




    private String[] a = {"你是是hidjhj","xxxxddag","dagjhuhhdhgah","你海马还记得哈","米好吗"};

    private String[] b = {"你好吗","你码号","我去"};

    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KFlowLayout layout = findViewById(R.id.flow);

         viewPager = new ViewPager();
        layout.setAdapter(viewPager);


        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CheckedTextView checkedTextView = (CheckedTextView)v;

                if (checkedTextView.isChecked()){
                    checkedTextView.setChecked(false);
                    viewPager.setData(a);
                }else {
                    checkedTextView.setChecked(true);
                    viewPager.setData(b);
                }
            }
        });

        viewPager.setData(a);


    }

    class ViewPager extends KFlowLayout.KFAdapter {



        String[] strings;
        public void setData(String[] data){
            strings = data;

            notifyDataSetChanged();
        }

        @Override
        protected View getView(Context context, View parent, int position) {

            TextView textView = (TextView) LayoutInflater.from(context).inflate(R.layout.textview, null);


            textView.setText(strings[position]);

            return textView;
        }

        @Override
        public int getCount() {
            return strings ==null ? 0:strings.length;
        }
    }


}
