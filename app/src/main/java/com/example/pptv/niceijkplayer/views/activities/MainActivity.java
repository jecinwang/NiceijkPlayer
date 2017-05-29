package com.example.pptv.niceijkplayer.views.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.pptv.niceijkplayer.R;
import com.example.pptv.niceijkplayer.presenters.MainPresenter;

public class MainActivity extends AppCompatActivity implements IMainView {
    private MainPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter = new MainPresenter(this);
        presenter.attachView(this);
        presenter.play("http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-33-30.mp4");
    }


    public void playInMiniWindow(View view) {
        presenter.playInMiniWindow();
    }

    @Override
    public void onBackPressed() {
        presenter.release();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
    }
}
