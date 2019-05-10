package com.cugkuan.flowlayout;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cugkuan.flow.KFlowLayout;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KFlowLayout layout  = findViewById(R.id.flow);

        ViewPager viewPager = new ViewPager();
        layout.setAdapter(viewPager);





    }

    class ViewPager  extends KFlowLayout.KFAdapter{


        private String[] strings = new String[6];
        {
            strings[0] = "你好吗";
            strings[1] = "你好吗好的哈较好的";
            strings[2] = "dddgghh";
            strings[3] = "测试的的";
            strings[4] = "你好不好啊，好不好啊好不好啊";
            strings[5] = "你不哦啊吗，巴哈卡机的和交互打工行大动干戈交换机会加大";
        }

        @Override
        protected View getView(Context context, View parent, int position) {

            TextView textView =  (TextView) LayoutInflater.from(context).inflate(R.layout.textview,null);

            textView.setText(strings[position]);

            return textView;
        }

        @Override
        public int getCount() {
            return strings.length;
        }
    }


}
