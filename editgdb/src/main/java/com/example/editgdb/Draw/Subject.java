package com.example.editgdb.Draw;

import com.esri.core.table.TableException;

import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Vector;

/**
 *
 * @author ropp gispace@yeah.net
 * update by gisluq
 * 被监听类的父类
 * DrawTool继承了这个类，具备了添加监听的能力
 */
public class Subject {

    private Vector<DrawEventListener> repository = new Vector<DrawEventListener>();

    // 添加监听
    public void addEventListener(DrawEventListener listener) {
        this.repository.addElement(listener);
    }

    // 移除监听
    public void removeEventListener(DrawEventListener listener){
        this.repository.removeElement(listener);
    }

    // 向监听者派发消息
    public void notifyEvent(DrawEvent event) {
        Enumeration<DrawEventListener> en = this.repository.elements();
        while (en.hasMoreElements()) {
            DrawEventListener listener = en.nextElement();
            try {
                listener.handleDrawEvent(event);
            } catch (TableException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
