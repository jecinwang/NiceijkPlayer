package com.example.pptv.niceijkplayer.presenters;

/**
 * Created by wzhx on 2017/5/29.
 */

public interface IMainPresenter<V> {

    void attachView(V view);

    void detachView();

    void play(String url);

    void release();

    void playInMiniWindow();
}
