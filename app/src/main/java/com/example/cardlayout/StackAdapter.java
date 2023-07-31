package com.example.cardlayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * <pre>
 * Created by zhuguohui
 * Date: 2023/7/28
 * Time: 17:37
 * Desc:
 * </pre>
 */
public abstract class StackAdapter {

    public  abstract View getView(int position, LayoutInflater inflater, ViewGroup viewGroup);

    public abstract int getVisibleCount();

    public abstract int getCount();

    public abstract void bindData(View view,int position);
}

