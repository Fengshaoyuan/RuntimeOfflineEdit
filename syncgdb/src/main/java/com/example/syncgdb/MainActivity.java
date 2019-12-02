package com.example.syncgdb;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.MapView;
import com.esri.core.ags.FeatureServiceInfo;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geodatabase.GeodatabaseFeatureTableEditErrors;
import com.esri.core.map.CallbackListener;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusCallback;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusInfo;
import com.esri.core.tasks.geodatabase.GeodatabaseSyncTask;
import com.esri.core.tasks.geodatabase.SyncGeodatabaseParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    protected static final String TAG = "syncGDB";
    private Context context;

    private MapView mMapView;//地图容器

    private static String onlineFeatureLayerUrl = "http://192.168.1.212:6080/arcgis/rest/services/testdata/FeatureServer";//在线FeatureLayer地址
    private static String localGdbFilePath;//离线GDB地址

    private GeodatabaseSyncTask gdbSyncTask;//离线地理数据库下载Task
    private ProgressDialog mProgressDialog;//状态框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.context = this;
        this.mMapView = (MapView)findViewById(R.id.map);

        //设置离线地理数据库存储路径
        localGdbFilePath = createGeodatabaseFilePath();
        //加载离线地理数据库
        addFeatureLayer(localGdbFilePath);

        mProgressDialog = new ProgressDialog(context);
        //设置点击进度对话框外的区域对话框不消失
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle("正在同步离线地理数据库副本到服务器");


        //绑定按钮设置下载事件
        Button btnSyncGDB = (Button)this.findViewById(R.id.btnSync);
        btnSyncGDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncOfflineData();//同步离线地理数据库
            }
        });
    }

    /**
     * Geodatabase文件存储路径
     */
    static String createGeodatabaseFilePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/RuntimeOfflineEdit"
                + File.separator + "demo.geodatabase";
    }

    /**
     * 读取Geodatabase中离线地图信息
     * @param featureLayerPath 离线Geodatabase文件路径
     */
    private void addFeatureLayer(String featureLayerPath) {

        Geodatabase localGdb = null;
        try {
            localGdb = new Geodatabase(featureLayerPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // 添加FeatureLayer到MapView中
        if (localGdb != null) {
            for (GeodatabaseFeatureTable gdbFeatureTable : localGdb.getGeodatabaseTables()) {
                if (gdbFeatureTable.hasGeometry()){
                    FeatureLayer layer = new FeatureLayer(gdbFeatureTable);
                    mMapView.addLayer(layer);
                }
            }
        }
    }

    /**
     * 同步离线地理数据库
     */
    private void SyncOfflineData() {

        Log.i(TAG, "Sync GeoDatabase");
        // create a dialog to update user on progress
        mProgressDialog.show();

        gdbSyncTask = new GeodatabaseSyncTask(onlineFeatureLayerUrl, null);
        gdbSyncTask.fetchFeatureServiceInfo(new CallbackListener<FeatureServiceInfo>() {

            @Override
            public void onError(Throwable arg0) {
                Log.e(TAG, "获取FeatureServiceInfo失败");
            }

            @Override
            public void onCallback(FeatureServiceInfo fsInfo) {
                if (fsInfo.isSyncEnabled()) {
                    SyncGeodatabase(fsInfo);
                }
            }
        });
    }

    /**
     * 根据FeatureServiceInfo信息获取离线地理数据库同步信息
     * @param featureServerInfo 服务参数信息
     */
    private void SyncGeodatabase(FeatureServiceInfo featureServerInfo) {
        try {
            // 创建一个离线地理数据库
            Geodatabase gdb = new Geodatabase(localGdbFilePath);

            // 获取离线地理数据库同步参数
            final SyncGeodatabaseParameters syncParams = gdb.getSyncParameters();

            CallbackListener<Map<Integer, GeodatabaseFeatureTableEditErrors>> syncResponseCallback
                    = new CallbackListener<Map<Integer, GeodatabaseFeatureTableEditErrors>>() {

                @Override
                public void onCallback(Map<Integer, GeodatabaseFeatureTableEditErrors> objs) {
                    mProgressDialog.dismiss();
                    if (objs != null) {
                        if (objs.size() > 0) {
                            showMakeText("同步完成，但是发生错误");
                        } else {
                            showMakeText("同步完成：同步成功");
                        }
                    } else {
                        showMakeText("同步完成：同步成功");
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "", e);
                    mProgressDialog.dismiss();
                    Toast.makeText(context, "Error:"+e.toString(), Toast.LENGTH_SHORT).show();
                }

            };

            GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {

                @Override
                public void statusUpdated(GeodatabaseStatusInfo status) {
                    final String progress = status.getStatus().toString();
                    //在UI线程更新下载状态
                    ((Activity)context).runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            mProgressDialog.setMessage("数据同步中，请稍后……");
                        }
                    });
                }
            };

            // 执行同步
            gdbSyncTask.syncGeodatabase(syncParams, gdb, statusCallback, syncResponseCallback);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 在UI线程中执行状态提示
     * @param msg
     */
    private void showMakeText(final String msg) {
        //在UI线程更新下载状态
        ((Activity)context).runOnUiThread(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
