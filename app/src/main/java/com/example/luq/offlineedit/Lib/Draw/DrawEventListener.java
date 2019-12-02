package com.example.luq.offlineedit.Lib.Draw;

import com.esri.core.table.TableException;

import java.io.FileNotFoundException;
import java.util.EventListener;


/**
 * @author ropp gispace@yeah.net
 * update by gisluq
 *	定义画图事件监听接口
 */
public interface DrawEventListener extends EventListener {

	void handleDrawEvent(DrawEvent event) throws TableException, FileNotFoundException;
}
