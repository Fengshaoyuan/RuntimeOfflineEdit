package com.example.editgdb.Draw.Util;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;

import java.util.ArrayList;

/**
 * 分割面
 */
public class CutPolygonL {

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
     * 线裁剪面
     * @param gon 面
     * @param line 分割线
     * @return 分割后多边形列表
     */
    public static ArrayList<Polygon> Cut(Polygon gon, Polyline line)
    {
        ArrayList<Polygon> _result = new ArrayList<Polygon>();

        int gon_index1 = -1, gon_index2 = -1, line_index1 = -1, line_index2 = -1, index_swap = -1 ;
        Point intersectP1=null, intersectP2=null;

        for (int i = 0; i < gon.getPointCount() - 1; i++)
        {
            for (int j = 0; j < line.getPointCount() - 1; j++)
            {
                Point _intersectP = CutPolygonL.SegmentInterect(gon.getPoint(i), gon.getPoint(i + 1), line.getPoint(j),  line.getPoint(j+1));
                if (_intersectP != null)
                {
                    if (gon_index1 == -1 && line_index1 == -1 && intersectP1 == null)
                    {
                        gon_index1 = i;
                        line_index1 = j;
                        intersectP1 = _intersectP;
                    }
                    else if (gon_index2 == -1 && line_index2 == -1 && intersectP2 == null)
                    {
                        gon_index2 = i;
                        line_index2 = j;
                        intersectP2 = _intersectP;
                    }
                }
            }
        }

        Polygon _PC1 = new Polygon();
        Polygon _PC2 = new Polygon();
        Boolean _swap = false;
        if (line_index1 > line_index2)
        {
            index_swap = line_index1;
            line_index1 = line_index2;
            line_index2 = index_swap;
            _swap = true;
            _PC1.startPath(intersectP2);
            _PC2.startPath(intersectP2);
        }
        else {
            _PC1.startPath(intersectP1);
            _PC2.startPath(intersectP1);
        }
        for (int i = line_index1+1; i < line_index2+1; i++)
        {
            _PC1.lineTo(line.getPoint(i));
            _PC2.lineTo(line.getPoint(i));
        }

        if (_swap)
        {
            _PC1.lineTo(intersectP1);
            _PC2.lineTo(intersectP1);
        }
        else {
            _PC1.lineTo(intersectP2);
            _PC2.lineTo(intersectP2);
        }

        if (_swap)
        {
            for (int i = gon_index1 + 1; i < gon_index2 + 1; i++)
            {
                _PC1.lineTo(gon.getPoint(i));
            }
            _PC1.lineTo(intersectP2);


            for (int i = gon_index1; i >= 0; i--)
            {
                _PC2.lineTo(gon.getPoint(i));
            }
            for (int i = gon.getPointCount()-1; i >= gon_index2+1; i--)
            {
                _PC2.lineTo(gon.getPoint(i));
            }
            _PC2.lineTo(intersectP2);
        }
        else {
            for (int i = gon_index2; i > gon_index1; i--)
            {
                _PC1.lineTo(gon.getPoint(i));
            }
            _PC1.lineTo(intersectP1);


            for (int i = gon_index2+1; i <gon.getPointCount(); i++)
            {
                _PC2.lineTo(gon.getPoint(i));
            }
            for (int i = 0; i <= gon_index1; i++)
            {
                _PC2.lineTo(gon.getPoint(i));
            }
            _PC2.lineTo(intersectP1);
        }

        _result.add(_PC1);
        _result.add(_PC2);
        return _result;
    }
}
