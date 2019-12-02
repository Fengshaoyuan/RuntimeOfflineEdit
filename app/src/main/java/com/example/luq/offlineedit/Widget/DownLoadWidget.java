package com.example.luq.offlineedit.Widget;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.esri.android.map.MapView;

/**
 * 下载组件
 * Created by luq on 2016/9/9.
 */
public class DownLoadWidget {

    private View downLoadView;//下载组件
    private Context context;
    private MapView mapView;

    private EditText editTextGdbUrl = null;
    private Button btnDownloadGDB = null;

    public DownLoadWidget(Context context, View downloadView, MapView mMapView) {
        this.context = context;
        this.downLoadView = downloadView;
        this.mapView = mMapView;
        initView();//初始化
    }

    /**
     * 初始化系统UI组件
     */
    private void initView() {
        
    }


}
