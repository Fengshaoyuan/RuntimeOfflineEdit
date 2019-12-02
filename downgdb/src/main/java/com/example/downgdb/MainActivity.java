package com.example.downgdb;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.MapView;
import com.esri.core.ags.FeatureServiceInfo;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.map.CallbackListener;
import com.esri.core.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusCallback;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusInfo;
import com.esri.core.tasks.geodatabase.GeodatabaseSyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    protected static final String TAG = "downGDB";
    private Context context;

    private MapView mMapView;//地图容器

    private EditText editTextDownGDBUrl;//GDB地址
    private Button btnDownGDB;//下载GDB

    private static String onlineFeatureLayerUrl;//在线FeatureLayer地址
    private static String localGdbFilePath;//离线GDB地址

    private GeodatabaseSyncTask gdbSyncTask;//离线地理数据库下载Task
    private ProgressDialog mProgressDialog;//状态框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        // 默认软键盘不弹出
        getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        this.mMapView = (MapView)findViewById(R.id.map);
        this.editTextDownGDBUrl = (EditText)findViewById(R.id.editTextGDBUrl);
        //获取并设置在线服务地址
        this.onlineFeatureLayerUrl = this.editTextDownGDBUrl.getText().toString();

        mProgressDialog = new ProgressDialog(context);
        //设置点击进度对话框外的区域对话框不消失
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle("正在创建离线地理数据库副本");

        //绑定按钮设置下载事件
        btnDownGDB = (Button)this.findViewById(R.id.btnDownGDB);
        btnDownGDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadData(onlineFeatureLayerUrl);//下载离线数据
            }
        });

//        //设置离线地理数据库存储路径
//        String localGdbFilePath = createGeodatabaseFilePath();
//        //加载离线地理数据库
//        addFeatureLayer(localGdbFilePath);

    }

    /**
	 * Geodatabase文件存储路径
	 */
    static String createGeodatabaseFilePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/RuntimeOfflineEdit"
                + File.separator + "demo.geodatabase";
    }

    /**
     * 下载离线地理数据库
     * @param url FeatureService服务地址
     * 例如：http://192.168.1.212:6080/arcgis/rest/services/testdata/FeatureServer
     * 支持ArcGIS for Server 10.2.1以上版本，必须开启FeatureServer要素同步功能
     */
    private void downloadData(String url) {
        Log.i(TAG, "Create GeoDatabase");
        // create a dialog to update user on progress
        mProgressDialog.show();

        gdbSyncTask = new GeodatabaseSyncTask(url, null);
        gdbSyncTask.fetchFeatureServiceInfo(new CallbackListener<FeatureServiceInfo>() {

            @Override
            public void onError(Throwable arg0) {
                Log.e(TAG, "获取FeatureServiceInfo失败");
            }

            @Override
            public void onCallback(FeatureServiceInfo fsInfo) {
                if (fsInfo.isSyncEnabled()) {
                    createGeodatabase(fsInfo);
                }
            }
        });
    }

    /**
     * 根据FeatureServiceInfo信息创建离线地理数据库文件
     * @param featureServerInfo 服务参数信息
     */
    private void createGeodatabase(FeatureServiceInfo featureServerInfo) {
        // 生成一个geodatabase设置参数
        GenerateGeodatabaseParameters params = new GenerateGeodatabaseParameters(
                featureServerInfo, mMapView.getMaxExtent(), mMapView.getSpatialReference());

        // 下载结果回调函数
        CallbackListener<String> gdbResponseCallback = new CallbackListener<String>() {
            @Override
            public void onError(final Throwable e) {
                Log.e(TAG, "创建geodatabase失败");
                mProgressDialog.dismiss();
            }

            @Override
            public void onCallback(String path) {
                Log.i(TAG, "Geodatabase 路径: " + path);
                mProgressDialog.dismiss();


                // 创建一个geodatabase数据库
                Geodatabase localGdb = null;
                try {
                    localGdb = new Geodatabase(path);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                // 添加FeatureLayer到MapView中
                if (localGdb != null) {
                    for (GeodatabaseFeatureTable gdbFeatureTable : localGdb.getGeodatabaseTables()) {
                        if (gdbFeatureTable.hasGeometry()){
                            mMapView.addLayer(new FeatureLayer(gdbFeatureTable));
                        }
                    }
                }
            }
        };

        // 下载状态回调函数
        GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {
            @Override
            public void statusUpdated(final GeodatabaseStatusInfo status) {
                final String progress = status.getStatus().toString();
                //在UI线程更新下载状态
                ((Activity)context).runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        mProgressDialog.setMessage("数据下载中，请稍后……");
                    }
                });

            }
        };

        //设置离线地理数据库存储路径
        localGdbFilePath = createGeodatabaseFilePath();

        //执行下载Geodatabase数据库
        gdbSyncTask.generateGeodatabase(params, localGdbFilePath, false, statusCallback, gdbResponseCallback);
    }

//    /**
//     * 读取Geodatabase中离线地图信息
//     * @param featureLayerPath 离线Geodatabase文件路径
//     */
//    private void addFeatureLayer(String featureLayerPath) {
//
//        Geodatabase localGdb = null;
//        try {
//            localGdb = new Geodatabase(featureLayerPath);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        // 添加FeatureLayer到MapView中
//        if (localGdb != null) {
//            for (GeodatabaseFeatureTable gdbFeatureTable : localGdb.getGeodatabaseTables()) {
//                if (gdbFeatureTable.hasGeometry()){
//                    FeatureLayer layer = new FeatureLayer(gdbFeatureTable);
//                    mMapView.addLayer(layer);
//                }
//            }
//        }
//    }

}
