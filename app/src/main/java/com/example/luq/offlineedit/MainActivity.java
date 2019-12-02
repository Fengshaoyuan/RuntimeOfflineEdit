package com.example.luq.offlineedit;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.esri.android.map.MapView;
import com.example.luq.offlineedit.Widget.DownLoadWidget;
import com.example.luq.offlineedit.Widget.FeaturTempleteWidget;
import com.example.luq.offlineedit.Widget.FeatureToolsWidget;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity" ;

    private Context context;
    private MapView mMapView;

    private DownLoadWidget downLoadWidget;//下载组件
    private FeaturTempleteWidget featurTempleteWidget;//要素模板组件
    private FeatureToolsWidget featureToolsWidget;//要素编辑工具

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 默认软键盘不弹出
        getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        this.context = this;
        this.mMapView = (MapView)findViewById(R.id.map);

        //初始化模块组件
        View downloadView = findViewById(R.id.activity_main_DownLoadView);
        this.downLoadWidget = new DownLoadWidget(context,downloadView,mMapView);
        View feeturTempletView =  findViewById(R.id.activity_main_featureTempleteView);
        this.featurTempleteWidget = new FeaturTempleteWidget(context,feeturTempletView,mMapView);
        View featureToolsView = findViewById(R.id.activity_main_featureToolsView);
        this.featureToolsWidget = new FeatureToolsWidget(context,featureToolsView,mMapView);

    }
}
