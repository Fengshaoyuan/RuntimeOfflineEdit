package com.example.editgdb.Draw;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.FillSymbol;
import com.esri.core.symbol.LineSymbol;
import com.esri.core.symbol.MarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;

import java.util.ArrayList;

/**
 *
 * @author ropp gispace@yeah.net
 * update by gisluq
 * 画图实现类，支持画点、矩形、线、多边形、圆、手画线、手画多边形，可设置各种图形的symbol。
 */
public class DrawTool extends Subject {

    private static String TAG = "DrawTool";

    private MapView mapView;
    private GraphicsLayer mGraphicsLayerEditing;//绘制要素图层
    private GraphicsLayer tempLayer;//零时图层，用于存储要素各个节点信息
    private MarkerSymbol markerSymbol;
    private LineSymbol lineSymbol;
    private FillSymbol fillSymbol;
    private int drawType;//当前要素绘制类型
    private boolean active;
    private boolean isAllowDoubleTouchToEnd=true;//是否允许双击结束绘制
    private Point point;
    private Envelope envelope;
    private Polyline polyline;
    private Polygon polygon;
    private DrawTouchListener drawListener;//绘图事件
    private MapOnTouchListener defaultListener;//默认点击事件
    private Graphic drawGraphic;//当前绘制要素
    private Point startPoint;//当前绘制要素起点信息
    private int graphicID;//当前绘制要素graphicID

    private boolean isCompleteDraw=true;//是否完成要素绘制

    //=======================================================================================================
    ArrayList<Point> mPoints = new ArrayList<Point>(); //节点
    ArrayList<Point> mMidPoints = new ArrayList<Point>(); //中间点
    boolean mMidPointSelected = false; //线段中间点
    boolean mVertexSelected = false;//顶点是否选择
    int mInsertingIndex;//插入位置信息
    ArrayList<EditingStates> mEditingStates = new ArrayList<EditingStates>();//编辑状态信息

    public static final int POINT = 1;//点
    public static final int ENVELOPE = 2;//矩形
    public static final int POLYLINE = 3;//线
    public static final int POLYGON = 4;//面
    public static final int CIRCLE = 5;//圆
    public static final int ELLIPSE = 6;//椭圆
    public static final int FREEHAND_POLYGON = 7; //流状面
    public static final int FREEHAND_POLYLINE = 8; //流状线

    public DrawTool(MapView mapView) {
        this.mapView = mapView;
        this.mapView.setShowMagnifierOnLongPress(true);//允许使用放大镜
        this.mGraphicsLayerEditing = new GraphicsLayer();
        this.mapView.addLayer(this.mGraphicsLayerEditing);
        this.tempLayer = new GraphicsLayer();
        this.mapView.addLayer(this.tempLayer);

        drawListener = new DrawTouchListener(this.mapView.getContext(),
                this.mapView);
        defaultListener = new MapOnTouchListener(this.mapView.getContext(),
                this.mapView);
        this.markerSymbol = DrawSymbol.markerSymbol;
        this.lineSymbol = DrawSymbol.mLineSymbol;
        this.fillSymbol = DrawSymbol.mFillSymbol;
        this.fillSymbol.setAlpha(90);
    }

    public void activate(int drawType) {
        if (this.mapView == null)
            return;

        this.deactivate();

        this.mapView.setOnTouchListener(drawListener);
        this.drawType = drawType;
        this.active = true;
        switch (this.drawType) {
            case DrawTool.POINT:
                this.point = new Point();
                drawGraphic = new Graphic(this.point, this.markerSymbol);
                break;
            case DrawTool.ENVELOPE:
                this.envelope = new Envelope();
                drawGraphic = new Graphic(this.envelope, this.fillSymbol);
                break;
            case DrawTool.POLYGON:
            case DrawTool.CIRCLE:
            case DrawTool.FREEHAND_POLYGON:
                this.polygon = new Polygon();
                drawGraphic = new Graphic(this.polygon, this.fillSymbol);
                clear();//清除顶点和节点信息
                break;
            case DrawTool.POLYLINE:
            case DrawTool.FREEHAND_POLYLINE:
                this.polyline = new Polyline();
                drawGraphic = new Graphic(this.polyline, this.lineSymbol);
                clear();//清除顶点和节点信息
                break;
        }
        graphicID = this.mGraphicsLayerEditing.addGraphic(drawGraphic);

        isCompleteDraw=false;//开始绘制
    }

    public void deactivate() {
        this.mapView.setOnTouchListener(defaultListener);
        this.mGraphicsLayerEditing.removeAll();
        this.tempLayer.removeAll();
        this.active = false;
        this.isCompleteDraw=true;//完成绘制
        this.drawType = -1;
        this.point = null;
        this.envelope = null;
        this.polygon = null;
        this.polyline = null;
        this.drawGraphic = null;
        this.startPoint=null;
    }

    public boolean isActive(){
        return this.active;
    }

    public  boolean isCompleteDraw(){
       return this.isCompleteDraw;
    }

    public MarkerSymbol getMarkerSymbol() {
        return markerSymbol;
    }

    public void setMarkerSymbol(MarkerSymbol markerSymbol) {
        this.markerSymbol = markerSymbol;
    }

    //设置是否允许双击结束
    public void setAllowDoubleTouchToEnd(Boolean isAllowDoubleTouchToEnd){
        this.isAllowDoubleTouchToEnd = isAllowDoubleTouchToEnd;
    }

    public LineSymbol getLineSymbol() {
        return lineSymbol;
    }

    public void setLineSymbol(LineSymbol lineSymbol) {
        this.lineSymbol = lineSymbol;
    }

    public FillSymbol getFillSymbol() {
        return fillSymbol;
    }

    public void setFillSymbol(FillSymbol fillSymbol) {
        this.fillSymbol = fillSymbol;
    }

    public void sendDrawEndEvent() {
        DrawEvent e = new DrawEvent(this, DrawEvent.DRAW_END, DrawTool.this.drawGraphic);
        DrawTool.this.notifyEvent(e);
        int type = this.drawType;
        this.deactivate();
        if(type==POINT) {
            this.activate(type);
        }
    }

    /**
     * 设置当前待编辑
     * 该编辑目前仅经针对线、面编辑情况
     */
    public void setEditGraphic(Graphic graphic){
        this.mGraphicsLayerEditing.removeAll();
        Geometry geometry = graphic.getGeometry();
        if(geometry!=null){
            Geometry.Type type = geometry.getType();
           if(type== Geometry.Type.POINT||type== Geometry.Type.MULTIPOINT){//点
                //获取要素并显示
//               Graphic graphic0 = new Graphic(geometry,DrawSymbol.markerSymbol);
//               graphicID =this.mGraphicsLayerEditing.addGraphic(graphic0);
//               this.drawGraphic = graphic0;//保存当前

           }else if(type== Geometry.Type.LINE||type== Geometry.Type.POLYLINE){//线

               this.activate(DrawTool.POLYLINE);
               //获取点并按指定显示在地图上
               Graphic graphic1 = new Graphic(geometry,DrawSymbol.mLineSymbol);
               graphicID =this.mGraphicsLayerEditing.addGraphic(graphic1);
               this.drawGraphic = graphic1;//保存当前
               MultiPath polyline = (MultiPath)graphic1.getGeometry();
               this.mPoints.clear();
               for (int i =0;i<polyline.getPointCount();i++){
                   Point pt = polyline.getPoint(i);
                   this.mPoints.add(pt);
               }
               refresh();

           }else if(type== Geometry.Type.ENVELOPE||type== Geometry.Type.POLYGON){//面

               this.activate(DrawTool.POLYGON);
               //获取点并按指定显示在地图上
               Graphic graphic1 = new Graphic(geometry,DrawSymbol.mFillSymbol);
               graphicID =this.mGraphicsLayerEditing.addGraphic(graphic1);
               this.drawGraphic = graphic1;//保存当前
               MultiPath polygon = (MultiPath)graphic1.getGeometry();
               this.mPoints.clear();
               for(int i =0;i<polygon.getPointCount();i++){
                    Point pt = polygon.getPoint(i);
                    this.mPoints.add(pt);
               }
               refresh();

           }else if(type== Geometry.Type.UNKNOWN){//未知

           }else{//type不在区间内

           }
        }

    }

    /**
     * 刷新要素信息
     */
    private void refresh() {
        if (tempLayer != null) {
            tempLayer.removeAll();
        }
        drawPolylineOrPolygon();
        drawMidPoints(); //绘制要素中心点
        drawVertices(); //绘制要素节点
    }

    /**
     * 绘制节点的中心点
     */
    private void drawMidPoints() {
        int index;
        Graphic graphic;

        mMidPoints.clear();
        if (mPoints.size() > 1) {

            // Build new list of mid-points
            for (int i = 1; i < mPoints.size(); i++) {
                Point p1 = mPoints.get(i - 1);
                Point p2 = mPoints.get(i);
                mMidPoints.add(new Point((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2));
            }
            if (drawType == POLYGON && mPoints.size() > 2) {
                // Complete the circle
                Point p1 = mPoints.get(0);
                Point p2 = mPoints.get(mPoints.size() - 1);
                mMidPoints.add(new Point((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2));
            }

            // Draw the mid-points
            index = 0;
            for (Point pt : mMidPoints) {
                if (mMidPointSelected && mInsertingIndex == index) {
                    graphic = new Graphic(pt, DrawSymbol.mRedMarkerSymbol);
                } else {
                    graphic = new Graphic(pt, DrawSymbol.mGreenMarkerSymbol);
                }
                this.tempLayer.addGraphic(graphic);
                index++;
            }
        }
    }

    /**
     * 绘制要素的节点信息在mPoints中.
     */
    private void drawVertices() {
        int index = 0;
        SimpleMarkerSymbol symbol;

        for (Point pt : mPoints) {
            if (mVertexSelected && index == mInsertingIndex) {
                // This vertex is currently selected so make it red
                symbol = DrawSymbol.mRedMarkerSymbol;
            } else if (index == mPoints.size() - 1 && !mMidPointSelected && !mVertexSelected) {
                // Last vertex and none currently selected so make it red
                symbol = DrawSymbol.mRedMarkerSymbol;
            } else {
                // Otherwise make it black
                symbol = DrawSymbol.mBlackMarkerSymbol;
            }
            Graphic graphic = new Graphic(pt, symbol);
            tempLayer.addGraphic(graphic);//添加节点信息到零时图层
            index++;
        }
    }

    /**
     * 清除节点和终点信息
     */
    private void clear(){
        // 清除要素编辑状态数据
        mPoints.clear();
        mMidPoints.clear();
        mEditingStates.clear();

        mMidPointSelected = false;
        mVertexSelected = false;
        mInsertingIndex = 0;

        if (tempLayer != null) {
            tempLayer.removeAll();
        }
    }

    /**
     * 通过mPoints中的顶点绘制折线和多边形.
     */
    private void drawPolylineOrPolygon() {
        MultiPath multipath;
        if (mPoints.size() >= 1) {
            //利用节点信息创建MultiPath信息
            if (drawType == POLYLINE) {
                multipath = new Polyline();
            } else {
                multipath = new Polygon();
            }
            multipath.startPath(mPoints.get(0));
            for (int i = 1; i < mPoints.size(); i++) {
                multipath.lineTo(mPoints.get(i));
            }
            //创建多段线或者面
            if (drawType == POLYLINE) {
                this.drawGraphic = new Graphic(multipath, DrawSymbol.mLineSymbol);
                polyline = (Polyline) multipath;//保存线数据到全局变量
            } else {
                this.drawGraphic = new Graphic(multipath,DrawSymbol.mFillSymbol);
                polygon = (Polygon) multipath;//保存面数据到全局变量
            }
            this.mGraphicsLayerEditing.updateGraphic(graphicID, this.drawGraphic);
        }
    }


    /**
     * 节点回退操作.
     */
    public boolean actionUndo() {
        if (active &&(drawType==POLYGON || drawType==POLYLINE)) {
            if(mPoints.size()>=1) {//删除时至少保留一个节点
                if(mEditingStates.size()>=1){
                    if (mEditingStates.size() == 0) {
                        mMidPointSelected = false;
                        mVertexSelected = false;
                        mInsertingIndex = 0;
                    } else {
                        mPoints.clear();//清空节点要素
                        EditingStates state = mEditingStates.get(mEditingStates.size() - 1);
                        mPoints.addAll(state.points);
                        //Log.d(TAG, "# of points = " + mPoints.size());
                        mMidPointSelected = state.midPointSelected;
                        mVertexSelected = state.vertexSelected;
                        mInsertingIndex = state.insertingIndex;
                        refresh();
                        mEditingStates.remove(mEditingStates.size() - 1);
                        Toast.makeText(mapView.getContext(), "撤销操作", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }else{
                    Toast.makeText(mapView.getContext(), "当前状态无法撤销", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return false;
    }

    /**
     * 节点删除操作.
     */
    public boolean actionDelete() {
        if (active &&(drawType==POLYGON || drawType==POLYLINE)) {
            if(mPoints.size()>=2){//删除时至少保留一个节点
                mMidPointSelected = false;
                mVertexSelected = false;
                mEditingStates.add(new EditingStates(mPoints, mMidPointSelected, mVertexSelected, mInsertingIndex));
                if (!mVertexSelected) {
                    mPoints.remove(mPoints.size() - 1); // 删除最后一个顶点
                } else {
                    mPoints.remove(mInsertingIndex); // 删除当前选择的点
                }
                refresh();
                Toast.makeText(mapView.getContext(), "删除节点", Toast.LENGTH_SHORT).show();
                return true;
            }else{
                Toast.makeText(mapView.getContext(), "当前已无法删除节点", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    /**
     * 当一个新的点被添加/移动/删除时该实例被创建
     * 用于记录编辑的状态信息，用于允许编辑的回退操作
     */
    private class EditingStates {
        ArrayList<Point> points = new ArrayList<Point>();

        boolean midPointSelected = false;
        boolean vertexSelected = false;
        int insertingIndex;

        public EditingStates(ArrayList<Point> points, boolean midpointselected, boolean vertexselected, int insertingindex) {
            this.points.addAll(points);
            this.midPointSelected = midpointselected;
            this.vertexSelected = vertexselected;
            this.insertingIndex = insertingindex;
        }
    }

    /**
     * 扩展MapOnTouchListener，实现画图功能
     */
    class DrawTouchListener extends MapOnTouchListener {

        public DrawTouchListener(Context context, MapView view) {
            super(context, view);
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (active && ( drawType == ENVELOPE
                    || drawType == CIRCLE
                    || drawType == FREEHAND_POLYLINE || drawType == FREEHAND_POLYGON)
                    && event.getAction() == MotionEvent.ACTION_DOWN) {
                //检查是否为空
                if(event == null) return false;
                Point point = mapView.toMapPoint(event.getX(), event.getY());
                switch (drawType) {
//                    case DrawTool.POINT:
//                        DrawTool.this.point.setXY(point.getX(), point.getY());
//                        sendDrawEndEvent();
//                        break;
                    case DrawTool.ENVELOPE:
                        startPoint = point;
                        envelope.setCoords(point.getX(), point.getY(),
                                point.getX(), point.getY());
                        break;
                    case DrawTool.CIRCLE:
                        startPoint = point;
                        break;
                    case DrawTool.FREEHAND_POLYGON:
                        polygon.startPath(point);
                        break;
                    case DrawTool.FREEHAND_POLYLINE:
                        polyline.startPath(point);
                        break;
                }
                return true;
            }
            try{
                //多次执行面分割会异常
                return super.onTouch(view, event);
            }catch (Exception e){
                Log.e(TAG,e.getMessage());
                return true;
            }
        }

        @Override
        public boolean onDragPointerMove(MotionEvent from, MotionEvent to) {
            if (active
                    && (drawType == ENVELOPE || drawType == FREEHAND_POLYGON
                    || drawType == FREEHAND_POLYLINE || drawType == CIRCLE)) {
                //检查是否为空
                if(from==null||to==null) return  false;
                Point point = mapView.toMapPoint(to.getX(), to.getY());
                switch (drawType) {
                    case DrawTool.ENVELOPE:
                        envelope.setXMin(startPoint.getX() > point.getX() ? point
                                .getX() : startPoint.getX());
                        envelope.setYMin(startPoint.getY() > point.getY() ? point
                                .getY() : startPoint.getY());
                        envelope.setXMax(startPoint.getX() < point.getX() ? point
                                .getX() : startPoint.getX());
                        envelope.setYMax(startPoint.getY() < point.getY() ? point
                                .getY() : startPoint.getY());
                        DrawTool.this.mGraphicsLayerEditing.updateGraphic(graphicID, envelope.copy());
                        break;
                    case DrawTool.FREEHAND_POLYGON:
                        polygon.lineTo(point);
                        DrawTool.this.mGraphicsLayerEditing.updateGraphic(graphicID, polygon);

                        break;
                    case DrawTool.FREEHAND_POLYLINE:
                        polyline.lineTo(point);
                        DrawTool.this.mGraphicsLayerEditing.updateGraphic(graphicID, polyline);

                        break;
                    case DrawTool.CIRCLE:
                        double radius = Math.sqrt(Math.pow(startPoint.getX()
                                - point.getX(), 2)
                                + Math.pow(startPoint.getY() - point.getY(), 2));
                        getCircle(startPoint, radius, polygon);
                        DrawTool.this.mGraphicsLayerEditing.updateGraphic(graphicID, polygon);
                        break;
                }
                return true;
            }
            return super.onDragPointerMove(from, to);
        }

        @Override
        public boolean onDragPointerUp(MotionEvent from, MotionEvent to) {
            if (active && (drawType == ENVELOPE || drawType == FREEHAND_POLYGON
                    || drawType == FREEHAND_POLYLINE || drawType == CIRCLE)) {
                //检查是否为空
                if(from==null||to==null) return  false;
                Point point = mapView.toMapPoint(to.getX(), to.getY());
                switch (drawType) {
                    case DrawTool.ENVELOPE:
                        envelope.setXMin(startPoint.getX() > point.getX() ? point
                                .getX() : startPoint.getX());
                        envelope.setYMin(startPoint.getY() > point.getY() ? point
                                .getY() : startPoint.getY());
                        envelope.setXMax(startPoint.getX() < point.getX() ? point
                                .getX() : startPoint.getX());
                        envelope.setYMax(startPoint.getY() < point.getY() ? point
                                .getY() : startPoint.getY());
                        break;
                    case DrawTool.FREEHAND_POLYGON://流状面
                        polygon.lineTo(point);
                        break;
                    case DrawTool.FREEHAND_POLYLINE://流状线
                        polyline.lineTo(point);
                        break;
                    case DrawTool.CIRCLE:
                        double radius = Math.sqrt(Math.pow(startPoint.getX()
                                - point.getX(), 2)
                                + Math.pow(startPoint.getY() - point.getY(), 2));
                        getCircle(startPoint, radius, polygon);
                        break;
                }
                sendDrawEndEvent();
                startPoint = null;
                isCompleteDraw=true;//开始绘制
                return true;
            }
            return super.onDragPointerUp(from, to);
        }

        @Override
        public boolean onSingleTap(MotionEvent event) {
            if(active&&(drawType == POINT || drawType == POLYLINE || drawType == POLYGON)){
                return handleTap(event);
            }
             return super.onSingleTap(event);
        }

        @Override
        public boolean onLongPressUp(MotionEvent event) {
            if(active&&(drawType == POINT || drawType == POLYLINE || drawType == POLYGON)){
                return handleTap(event);
            }
            return super.onLongPressUp(event);
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (active &&(drawType==POLYGON || drawType==POLYLINE)) {
//                Point point = mapView.toMapPoint(event.getX(), event.getY());
//                //双击最后一个点不算在内
//                switch (drawType) {
//                    case DrawTool.POLYGON:
//                        polygon.lineTo(point);
//                        break;
//                    case DrawTool.POLYLINE:
//                        polyline.lineTo(point);
//                        break;
//                }
                int ptNum = mPoints.size();
                if(drawType==POLYLINE){
                    if(ptNum<2){
                        Toast.makeText(mapView.getContext(), "线段至少两个节点", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }else if (drawType==POLYGON){
                    if(ptNum<3){
                        Toast.makeText(mapView.getContext(), "面至少三个节点", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                isCompleteDraw=true;//开始绘制
                if(isAllowDoubleTouchToEnd){
                    sendDrawEndEvent();
                    startPoint = null;
                }
                return true;
            }
            return super.onDoubleTap(event);
        }

        /***
         * Handle a tap on the map (or the end of a magnifier long-press event).
         * @param event The point that was tapped.
         */
        private boolean handleTap(MotionEvent event) {
            Point point = mapView.toMapPoint(event.getX(), event.getY());
            if (active && (drawType==POLYGON || drawType==POLYLINE)) {
                // If a point is currently selected, move that point to tap point
                if (mMidPointSelected || mVertexSelected) {
                    movePoint(point);
                } else {
                    // If tap coincides with a mid-point, select that mid-point
                    int idx1 = getSelectedIndex(event.getX(), event.getY(), mMidPoints, mapView);
                    if (idx1 != -1) {
                        mMidPointSelected = true;
                        mInsertingIndex = idx1;
                    } else {
                        // If tap coincides with a vertex, select that vertex
                        int idx2 = getSelectedIndex(event.getX(), event.getY(), mPoints, mapView);
                        if (idx2 != -1) {
                            mVertexSelected = true;
                            mInsertingIndex = idx2;
                        } else {
                            // No matching point above, add new vertex at tap point
                            mEditingStates.add(new EditingStates(mPoints, mMidPointSelected, mVertexSelected, mInsertingIndex));
                            mPoints.add(point);
                        }
                    }
                }
                refresh();
                return true;
            }else if(drawType==POINT ){
                DrawTool.this.point.setXY(point.getX(), point.getY());
                sendDrawEndEvent();
                isCompleteDraw=true;//开始绘制
                return true;
            }
            return false;//默认为false
        }


        /**
         * Checks if a given location coincides (within a tolerance) with a point in a given array.
         *
         * @param x Screen coordinate of location to check.
         * @param y Screen coordinate of location to check.
         * @param points Array of points to check.
         * @param map MapView containing the points.
         * @return Index within points of matching point, or -1 if none.
         */
        private int getSelectedIndex(double x, double y, ArrayList<Point> points, MapView map) {
            final int TOLERANCE = 40; // Tolerance in pixels

            if (points == null || points.size() == 0) {
                return -1;
            }

            // Find closest point
            int index = -1;
            double distSQ_Small = Double.MAX_VALUE;
            for (int i = 0; i < points.size(); i++) {
                Point p = map.toScreenPoint(points.get(i));
                double diffx = p.getX() - x;
                double diffy = p.getY() - y;
                double distSQ = diffx * diffx + diffy * diffy;
                if (distSQ < distSQ_Small) {
                    index = i;
                    distSQ_Small = distSQ;
                }
            }

            // Check if it's close enough
            if (distSQ_Small < (TOLERANCE * TOLERANCE)) {
                return index;
            }
            return -1;
        }

        /**
         * 移动当前选择点
         * @param point Location to move the point to.
         */
        private void movePoint(Point point) {
            if (mMidPointSelected) {
                // Move mid-point to the new location and make it a vertex
                mPoints.add(mInsertingIndex + 1, point);
            } else {
                // Must be a vertex: move it to the new location
                ArrayList<Point> temp = new ArrayList<Point>();
                for (int i = 0; i < mPoints.size(); i++) {
                    if (i == mInsertingIndex) {
                        temp.add(point);
                    } else {
                        temp.add(mPoints.get(i));
                    }
                }
                mPoints.clear();
                mPoints.addAll(temp);
            }
            // Go back to the normal drawing mode and save the new editing state
            mMidPointSelected = false;
            mVertexSelected = false;
            mEditingStates.add(new EditingStates(mPoints, mMidPointSelected, mVertexSelected, mInsertingIndex));
        }

    }

    /**
     * 绘制圆
     * @param center
     * @param radius
     * @param circle
     */
    private void getCircle(Point center, double radius, Polygon circle) {
        circle.setEmpty();
        Point[] points = getPoints(center, radius);
        circle.startPath(points[0]);
        for (int i = 1; i < points.length; i++)
            circle.lineTo(points[i]);
    }

    private Point[] getPoints(Point center, double radius) {
        Point[] points = new Point[50];
        double sin;
        double cos;
        double x;
        double y;
        for (double i = 0; i < 50; i++) {
            sin = Math.sin(Math.PI * 2 * i / 50);
            cos = Math.cos(Math.PI * 2 * i / 50);
            x = center.getX() + radius * sin;
            y = center.getY() + radius * cos;
            points[(int) i] = new Point(x, y);
        }
        return points;
    }

}
