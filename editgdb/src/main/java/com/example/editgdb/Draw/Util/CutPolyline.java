package com.example.editgdb.Draw.Util;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;

import java.util.ArrayList;

/**
 * 线分割
 * Created by lq on 2015/7/18.
 */
public class CutPolyline {

    /**
     * 判断两条线之间的关系
     * @param aP1 线1端点1
     * @param aP2 线1端点2
     * @param bP1 线2端点1
     * @param bP2 线2端点2
     * @return 交点 否则返回空
     */
    public static Point SegmentInterect(Point aP1, Point aP2, Point bP1, Point bP2)
    {
        // 如果分母为0 则平行或共线, 不相交
        double denominator = (aP2.getY() - aP1.getY())*(bP2.getX() - bP1.getX()) - (aP1.getX() - aP2.getX())*(bP1.getY() - bP2.getY());
        if (denominator==0) {
            return null ;
        }

        // 线段所在直线的交点坐标 (X , y)
        double  x = ( (aP2.getX() - aP1.getX()) * (bP2.getX() - bP1.getX()) * (bP1.getY() - aP1.getY())
                + (aP2.getY() - aP1.getY()) * (bP2.getX() - bP1.getX()) * aP1.getX()
                - (bP2.getY() - bP1.getY()) * (aP2.getX() - aP1.getX()) * bP1.getX() ) / denominator ;
        double y = -( (aP2.getY() - aP1.getY()) * (bP2.getY() - bP1.getY()) * (bP1.getX() - aP1.getX())
                + (aP2.getX() - aP1.getX()) * (bP2.getY() - bP1.getY()) * aP1.getY()
                - (bP2.getX() - bP1.getX()) * (aP2.getY() - aP1.getY()) * bP1.getY() ) / denominator;

        /** 2 判断交点是否在两条线段上 **/
        if (
            // 交点在线段1上x
                (x - aP1.getX()) * (x - aP2.getX()) <= 0 && (y - aP1.getY()) * (y - aP2.getY()) <= 0
                        // 且交点也在线段2上
                        && (x - bP1.getX()) * (x - bP2.getX()) <= 0 && (y - bP1.getY()) * (y - bP2.getY()) <= 0
                ){
            // 返回交点p
            return new Point(x,y);
        }
        //否则不相交
        return null;
    }

    /**
     * 计算线与线的交点
     * @param waitingSplitPolyline 待分割线要素
     * @param line 线
     * @return
     */
    public static Point IntersectionPoint(Polyline waitingSplitPolyline, Polyline line){
        Point intersectP=null;//交点
        int gon_index1 = -1, line_index1 = -1;
        for (int i = 0; i < waitingSplitPolyline.getPointCount() - 1; i++)
        {
            for (int j = 0; j < line.getPointCount() - 1; j++)
            {
                Point _intersectP = CutPolygonL.SegmentInterect(waitingSplitPolyline.getPoint(i), waitingSplitPolyline.getPoint(i + 1), line.getPoint(j),  line.getPoint(j+1));
                if (_intersectP != null)
                {
                    if (gon_index1 == -1 && line_index1 == -1 && intersectP == null) {
                        gon_index1 = i;
                        line_index1 = j;
                        intersectP = _intersectP;
                    }
                }
            }
        }
        return intersectP;
    }

    /**
     * 线裁剪面
     * @param waitingSplitPolyline 带分割线
     * @param line 分割线
     * @return 分割后多边形列表
     */
    public static ArrayList<Polyline> Cut(Polyline waitingSplitPolyline, Polyline line)
    {
        ArrayList<Polyline> _result = new ArrayList<>();

        int gon_index1 = -1, line_index1 = -1;
        Point intersectP1=null;//交点

        for (int i = 0; i < waitingSplitPolyline.getPointCount() - 1; i++)
        {
            for (int j = 0; j < line.getPointCount() - 1; j++)
            {
                Point _intersectP = CutPolygonL.SegmentInterect(waitingSplitPolyline.getPoint(i), waitingSplitPolyline.getPoint(i + 1), line.getPoint(j),  line.getPoint(j+1));
                if (_intersectP != null)
                {
                    if (gon_index1 == -1 && line_index1 == -1 && intersectP1 == null)
                    {
                        gon_index1 = i;
                        line_index1 = j;
                        intersectP1 = _intersectP;
                    }
                }
            }
        }

        //相交点在waitingSplitPolyline的第gon_index1个和第gon_index1+1个之间

        Polyline _PC1 = new Polyline();
        Polyline _PC2 = new Polyline();
        int _PC1_num=0,_PC2_num=0;
        int plnum = waitingSplitPolyline.getPointCount();//待分割线的节点数
        for(int i=0;i<plnum;i++){
            Point pt = waitingSplitPolyline.getPoint(i);
            if(i<gon_index1+1){
                if(_PC1_num==0){
                    _PC1.startPath(pt);
                    _PC1_num++;
                }else{
                    _PC1.lineTo(pt);
                    _PC1_num++;
                }
            }else{
                if(_PC2_num==0){
                    _PC2.startPath(intersectP1);
                    _PC2_num++;
                    _PC2.lineTo(pt);
                    _PC2_num++;
                }else{
                    _PC2.lineTo(pt);
                    _PC2_num++;
                }
            }
        }
        _PC1.lineTo(intersectP1);
        _PC1_num++;

        _result.add(_PC1);
        _result.add(_PC2);
        return _result;
    }


}
