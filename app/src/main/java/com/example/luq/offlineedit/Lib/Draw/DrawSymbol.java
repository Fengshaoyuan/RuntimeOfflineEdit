package com.example.luq.offlineedit.Lib.Draw;

import android.graphics.Color;

import com.esri.core.symbol.FillSymbol;
import com.esri.core.symbol.LineSymbol;
import com.esri.core.symbol.MarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;

/**
 * 要素编辑状态符号化信息
 * Created by gis-luq on 15/5/21.
 */
public class DrawSymbol {

    private static int SIZE = 10;//节点大小

    public static MarkerSymbol markerSymbol  = new SimpleMarkerSymbol(Color.RED, SIZE, SimpleMarkerSymbol.STYLE.CIRCLE);
    public static SimpleMarkerSymbol mRedMarkerSymbol = new SimpleMarkerSymbol(Color.RED, SIZE, SimpleMarkerSymbol.STYLE.CIRCLE);
    public static SimpleMarkerSymbol mBlackMarkerSymbol = new SimpleMarkerSymbol(Color.BLACK, SIZE, SimpleMarkerSymbol.STYLE.CIRCLE);
    public static SimpleMarkerSymbol mGreenMarkerSymbol = new SimpleMarkerSymbol(Color.GREEN, SIZE, SimpleMarkerSymbol.STYLE.CIRCLE);
    public static LineSymbol mLineSymbol = new SimpleLineSymbol(Color.BLACK, 2);
    public static FillSymbol mFillSymbol =  new SimpleFillSymbol(Color.BLACK).setOutline(mLineSymbol).setAlpha(100);

}
