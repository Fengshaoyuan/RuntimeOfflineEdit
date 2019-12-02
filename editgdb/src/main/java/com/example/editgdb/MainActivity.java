package com.example.editgdb;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeature;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geometry.Geometry;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureTemplate;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.Renderer;
import com.esri.core.symbol.Symbol;
import com.esri.core.symbol.SymbolHelper;
import com.esri.core.table.TableException;
import com.example.editgdb.Draw.DrawEvent;
import com.example.editgdb.Draw.DrawEventListener;
import com.example.editgdb.Draw.DrawSymbol;
import com.example.editgdb.Draw.DrawTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DrawEventListener {

    protected static final String TAG = "editGDB";
    private Context context;

    private MapView mMapView;//地图容器
    private View featureTempleteView;//要素模板容器
    private List<FeatureLayer> layerList;//矢量图层列表

    private GraphicsLayer graphicsLayer = null;//零时图层
    private FeatureLayer selectFeatureLayer;//当前选中图层
    private Graphic selectGraphic = null;//当前选中要素（零时图层）
    private Feature selectFeature = null;//当前选中要素
    private DrawTool drawTool = null;//要素绘制工具

    public MapOnTouchListener mapDefaultOnTouchListener;//无状态事件
    public DrawEventListener drawEventListener;//要素绘制点击事件
    public MapSelectFeatureOnTouchListener mapSelectFeatureOnTouchListener;//要素选择事件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;
        this.mMapView = (MapView)findViewById(R.id.map);
        this.featureTempleteView = findViewById(R.id.featureTempleteView);

        //设置要素操作事件
        BtnOnClickListener btnOnClickListener = new BtnOnClickListener();
        Button btnStartEdit = (Button)findViewById(R.id.btnStartEdit);
        btnStartEdit.setOnClickListener(btnOnClickListener);
        Button btnDeleteNote = (Button)findViewById(R.id.btnDeleteNode);
        btnDeleteNote.setOnClickListener(btnOnClickListener);
        Button btnGoBack = (Button)findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(btnOnClickListener);
        Button btnSave = (Button)findViewById(R.id.btnSave);
        btnSave.setOnClickListener(btnOnClickListener);
        Button btnClear = (Button)findViewById(R.id.btnClear);
        btnClear.setOnClickListener(btnOnClickListener);
        Button btnDelete = (Button)findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(btnOnClickListener);

        //设置离线地理数据库存储路径
        String localGdbFilePath = getGeodatabaseFilePath();
        //加载离线地理数据库
        addFeatureLayer(localGdbFilePath);

        //添加零时图层
        this.graphicsLayer = new GraphicsLayer();
        this.mMapView.addLayer(graphicsLayer);

        // 初始化DrawTool实例
        this.drawTool = new DrawTool(this.mMapView);
        // 将本Activity设置为DrawTool实例的Listener
        this.drawTool.addEventListener(this);

        //设置地图事件
        mapDefaultOnTouchListener = new MapOnTouchListener(this.mMapView.getContext(), this.mMapView);
        drawEventListener = this;
        mapSelectFeatureOnTouchListener = new MapSelectFeatureOnTouchListener(this.mMapView.getContext(), this.mMapView);

        //设置默认地图Touch事件
        this.mMapView.setOnTouchListener(mapSelectFeatureOnTouchListener);
    }


    @Override
    public void handleDrawEvent(DrawEvent event) throws TableException, FileNotFoundException {
        //添加要素前先清空要素
        graphicsLayer.removeAll();
        // 将画好的图形（已经实例化了Graphic），添加到drawLayer中并刷新显示
        this.graphicsLayer.addGraphic(event.getDrawGraphic());
        //保存绘制过的Graphic要素
        this.selectGraphic = event.getDrawGraphic();
        // 修改点击事件为默认
        this.mMapView.setOnTouchListener(mapSelectFeatureOnTouchListener);
    }

    /**
     * Geodatabase文件存储路径
     */
    static String getGeodatabaseFilePath() {
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

        layerList = new ArrayList<>();
        // 添加FeatureLayer到MapView中
        if (localGdb != null) {
            for (GeodatabaseFeatureTable gdbFeatureTable : localGdb.getGeodatabaseTables()) {
                if (gdbFeatureTable.hasGeometry()){
                    FeatureLayer layer = new FeatureLayer(gdbFeatureTable);
                    layerList.add(layer);//保存矢量要素信息列表
                    mMapView.addLayer(layer);
                    addFeatureTemplate(layer);//添加要素绘制模板
                }
            }
        }
    }

    /**
     * 清空要素选择状态
     */
    private void clearFeatureSelection(){
         if(layerList!=null) {
             for (int i=0;i<layerList.size();i++){
                 FeatureLayer featurelayer = layerList.get(i);
                 featurelayer.clearSelection();//清空选择
             }
         }
    }

    /**
     * 添加要素绘制模板
     * @param layer
     */
    private void addFeatureTemplate(FeatureLayer layer) {
        //获取图层要素模板并添加到featureTempleteView
        List<FeatureTemplate> featureTemp = ((GeodatabaseFeatureTable) layer.getFeatureTable()).getFeatureTemplates();
        for (FeatureTemplate featureTemplate : featureTemp) {
            GeodatabaseFeature g = null;
            try {
                g = ((GeodatabaseFeatureTable) layer.getFeatureTable()).createFeatureWithTemplate(featureTemplate, null);
                Renderer renderer = layer.getRenderer();
                Symbol symbol = renderer.getSymbol(g);
                float scale = context.getResources().getDisplayMetrics().density;
                int widthInPixels = (int) (35 * scale + 0.5f);
                Bitmap bitmap = SymbolHelper.getLegendImage(symbol, widthInPixels, widthInPixels);

                ImageButton imageButton = new ImageButton(context);
                imageButton.setImageBitmap(bitmap);
                imageButton.setTag(layer);//保存当前待编辑图层
                ((LinearLayout)featureTempleteView).addView(imageButton);//添加到模板
                imageButton.setOnClickListener(new ImageButtonOnClickListener());
            } catch (TableException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error：" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 要素模板点击事件
     * 用于开始绘制要素
     */
    private class ImageButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            selectFeatureLayer = (FeatureLayer) v.getTag();//获取选中要素图层
            setEditingMode();// 设置编辑状态
        }
        /**
         * 设置编辑状态
         */
        private void setEditingMode() {
            selectFeature=null;//新建要素时设置selectFeature为空
            if (selectFeatureLayer != null) {
                if (selectFeatureLayer.getGeometryType().equals(Geometry.Type.POINT)
                        || selectFeatureLayer.getGeometryType().equals(Geometry.Type.MULTIPOINT)) {
                    //绘制点
                    drawTool.activate(DrawTool.POINT);
                } else if (selectFeatureLayer.getGeometryType().equals(Geometry.Type.POLYLINE)) {
                    //绘制线
                    drawTool.activate(DrawTool.POLYLINE);
                    Toast.makeText(MainActivity.this, "开始绘制线：双击屏幕结束绘制", Toast.LENGTH_SHORT).show();
                } else if (selectFeatureLayer.getGeometryType().equals(Geometry.Type.POLYGON)) {
                    //绘制面
                    drawTool.activate(DrawTool.POLYGON);
                    Toast.makeText(MainActivity.this, "开始绘制面：双击屏幕结束绘制", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    /**
     * 设置要素操作按钮点击事件
     */
    private class BtnOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            switch (v.getId()){
                case R.id.btnStartEdit:
                    if(selectGraphic!=null&&selectFeatureLayer!=null){
                        drawTool.setEditGraphic(selectGraphic);
                    }
                    break;
                case R.id.btnDeleteNode://删除节点
                    drawTool.actionDelete();
                    break;
                case R.id.btnGoBack://回退
                    drawTool.actionUndo();
                    break;
                case R.id.btnSave://保存要素
                    saveGraphic();
                    break;
                case R.id.btnClear:
                    for (int i=0;i<layerList.size();i++){
                        FeatureLayer featureLayer = layerList.get(i);
                        featureLayer.clearSelection();//清空选择
                    }
                    graphicsLayer.removeAll();
                    break;
                case R.id.btnDelete:
                    graphicsLayer.removeAll();
                    deleteGraphic();//删除要素
                    break;
            }

        }

        /**
         * 保存要素
         */
        private void saveGraphic() {
            if(selectGraphic!=null){
                if(selectFeature==null){//添加要素
                    try {
                        GeodatabaseFeatureTable geodatabaseFeatureTable = (GeodatabaseFeatureTable)selectFeatureLayer.getFeatureTable();
                        GeodatabaseFeature gdbFeature = new GeodatabaseFeature(null, selectGraphic.getGeometry(), geodatabaseFeatureTable);
                        long fid = geodatabaseFeatureTable.addFeature(gdbFeature);
                        Log.d(TAG,"要素添加成功，Feature ID："+fid);
                    } catch (TableException e) {
                        e.printStackTrace();
                    }
                }else {//更新要素
                    try {
                        GeodatabaseFeatureTable geodatabaseFeatureTable = (GeodatabaseFeatureTable)selectFeatureLayer.getFeatureTable();
                        geodatabaseFeatureTable.updateFeature(selectFeature.getId(),selectGraphic.getGeometry());
                        Log.d(TAG,"要素更新成功");
                    } catch (TableException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                Toast.makeText(context,"请双击保存后再试",Toast.LENGTH_SHORT).show();
            }
            graphicsLayer.removeAll();
        }

        /**
         * 删除要素
         */
        private void deleteGraphic() {

            AlertDialog.Builder  builder=new AlertDialog.Builder(context);
            builder.setTitle("系统提示");
            builder.setMessage("是否删除要素？");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    GeodatabaseFeatureTable geodatabaseFeatureTable = (GeodatabaseFeatureTable)selectFeatureLayer.getFeatureTable();
                    try {
                        if (selectFeature!=null){
                            geodatabaseFeatureTable.deleteFeature(selectFeature.getId());
                        }
                    } catch (TableException e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton("取消",null);

            //builder创建对话框对象AlertDialog
            AlertDialog simpledialog=builder.create();
            simpledialog.show();
        }
    }


    /**
     * 地图窗口默认Touch事件
     * 支持长按放大镜选种要素
     */
    public class MapSelectFeatureOnTouchListener extends MapOnTouchListener{

        public MapSelectFeatureOnTouchListener(Context context, MapView view) {
            super(context, view);
        }

        @Override
        public boolean onLongPressUp(MotionEvent point) {
            handleTap(point);
            //长按放大镜选择事件
            super.onLongPressUp(point);
            return true;
        }

        @Override
        public boolean onSingleTap(final MotionEvent e) {
            //单击响应事件
            return true;
        }

        /**
         * 获取当前选中要素
         * @param point
         */
        private void handleTap(MotionEvent point) {
            MotionEvent screePoint = point;

            //选中图层信息
            final List<SelectFeatureInfo> selectFeatureInfoList = new ArrayList<>();
            //记录当前选中要素信息，涉及多个图层情况
            for (int i=0;i<layerList.size();i++){
                FeatureLayer featureLayer = layerList.get(i);
                featureLayer.setSelectionColor(Color.YELLOW);
                featureLayer.setSelectionColorWidth(10);
                long[] selids = featureLayer.getFeatureIDs(screePoint.getX(), screePoint.getY(), 1);
                if (selids.length >= 1) {
                    SelectFeatureInfo selectFeatureInfo = new SelectFeatureInfo();
                    selectFeatureInfo.featureLayer = featureLayer;
                    selectFeatureInfo.selectFeatureID = selids[0];
                    selectFeatureInfoList.add(selectFeatureInfo);
                }
            }

            //根据待选图层确定是否弹窗选择
            if(selectFeatureInfoList.size()>1){
                //当前选中要素大于1个图层
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("请确认选择哪个图层要素？");
                //指定下拉列表的显示数据
                final String[] layerNamelist = getSelectFeatureInfoListName(selectFeatureInfoList);
                //设置一个下拉的列表选择项
                builder.setItems(layerNamelist, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Toast.makeText(MainActivity.this, "选择的图层为：" + layerNamelist[which], Toast.LENGTH_SHORT).show();
                        SelectFeatureInfo selectFeaInfo = getFeatureLayerbyName(selectFeatureInfoList,layerNamelist[which]);
                        setSelectFeature(selectFeaInfo);//根据选中图层信息选中当前要素
                    }
                });
                builder.show();
            }else if(selectFeatureInfoList.size()==1) {
                SelectFeatureInfo selectFeaInfo = selectFeatureInfoList.get(0);
                setSelectFeature(selectFeaInfo);//根据选中图层信息选中当前要素
            }

        }

        /**
         * 设置选中图层的要素选中信息
         * @param selectFeaInfo 待选图层信息
         */
        private void setSelectFeature(SelectFeatureInfo selectFeaInfo) {
            clearFeatureSelection();//设置选中状态前，清空已选择要素
            selectFeaInfo.featureLayer.selectFeature(selectFeaInfo.selectFeatureID);
            selectFeatureLayer = selectFeaInfo.featureLayer;
            selectFeature = selectFeatureLayer.getFeature(selectFeaInfo.selectFeatureID);
            switch (selectFeature.getGeometry().getType()){
                case POINT:
                case MULTIPOINT:
                    selectGraphic = new Graphic(selectFeature.getGeometry(), DrawSymbol.markerSymbol, null);
                    break;
                case LINE:
                case POLYLINE:
                    selectGraphic = new Graphic(selectFeature.getGeometry(), DrawSymbol.mLineSymbol,null);
                    break;
                case ENVELOPE:
                case POLYGON:
                    selectGraphic = new Graphic(selectFeature.getGeometry(), DrawSymbol.mFillSymbol, null);
                    break;
                default:
                    break;
            }
        }

        /**
         * 通过图层名称获取要素
         * @param name 图层名称
         * @return 选中图层信息
         */
        private SelectFeatureInfo getFeatureLayerbyName(List<SelectFeatureInfo> selectFeatureInfoList,String name){
            SelectFeatureInfo selectinfo= null;
            for (int i=0;i<selectFeatureInfoList.size();i++){
                FeatureLayer tmplayer = selectFeatureInfoList.get(i).featureLayer;
               if (tmplayer.getName().equals(name)){
                   selectinfo = selectFeatureInfoList.get(i);//选中图层信息
               }
            }
            return selectinfo;
        }

        /**
         * 获取待选择要素列表名称
         * @param selectFeatureInfoList 待选中要素列表信息
         * @return
         */
        private String[] getSelectFeatureInfoListName(List<SelectFeatureInfo> selectFeatureInfoList) {
            List<String> lsname = new ArrayList<>();
            for (int i=0;i<selectFeatureInfoList.size();i++){
                lsname.add(selectFeatureInfoList.get(i).featureLayer.getName());
            }
            return lsname.toArray(new String[lsname.size()]);
        }

        /**
         * 记录当前选中要素信息
         */
        public class SelectFeatureInfo{
            public FeatureLayer featureLayer ;//当前选中要素
            public long selectFeatureID ;//当前选中要素ID
        }
    }

}
