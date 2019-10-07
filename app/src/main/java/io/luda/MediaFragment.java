package io.luda;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
/*
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
*/
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import io.luda.model.Content;


public class MediaFragment extends Fragment {
    // Store instance variables
    private String title;
    private int page;
    private Content content;
    private Picasso picasso;

    // newInstance constructor for creating fragment with arguments
    public static MediaFragment newInstance() {
        MediaFragment fragmentFirst = new MediaFragment();
        return fragmentFirst;
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        picasso = new Picasso.Builder(this.getContext())
                .downloader(new OkHttp3Downloader(this.getContext(), Integer.MAX_VALUE))
                .build();
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = null;
        if(content == null) {
            return null;
        }
        if(content.type.equals(Content.Type.NEWS)) {
            view = inflater.inflate(R.layout.news_fragment, container, false);
            TextView tvLabel =  view.findViewById(R.id.title_text);
            TextView txtPubDate = view.findViewById(R.id.pub_date);
            TextView tvDescription = view.findViewById(R.id.description_text);
            ImageView ivThumb = view.findViewById(R.id.thumb_img);
            tvLabel.setText(content.title);
            tvDescription.setText(content.description);
            txtPubDate.setText(content.pubDate);
            picasso.load(content.urlImage).into(ivThumb);

        } else if(content.type.equals(Content.Type.AD_IMG)) {
            view = inflater.inflate(R.layout.ad_img_fragment, container, false);
            ImageView ivThumb = view.findViewById(R.id.img_content);
            Picasso.get().load(content.urlImage).into(ivThumb);
        } else if(content.type.equals(Content.Type.AD_VIDEO)) {
            view = inflater.inflate(R.layout.ad_video_fragment, container, false);
            /*PlayerView playerView = view.findViewById(R.id.video_view);
            SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(
                    new DefaultRenderersFactory(this.getContext()),
                    new DefaultTrackSelector(), new DefaultLoadControl());

            playerView.setPlayer(player);


            ExtractorMediaSource videoSource =
                    new ExtractorMediaSource.Factory(
                            new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                            createMediaSource(Uri.parse(content.urlVideo));


            player.prepare(videoSource);
            player.setPlayWhenReady(true);*/
        }



        return view;
    }
/*
    private MediaSource buildMediaSource(Uri uri) {
        // these are reused for both media sources we create below
        DefaultExtractorsFactory extractorsFactory =
                new DefaultExtractorsFactory();
        DefaultHttpDataSourceFactory dataSourceFactory =
                new DefaultHttpDataSourceFactory( "user-agent");

        ExtractorMediaSource videoSource =
                new ExtractorMediaSource.Factory(
                        new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                        createMediaSource(uri);


        ExtractorMediaSource audioSource =
                new ExtractorMediaSource.Factory(
                        new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                        createMediaSource(uri);

        return new ConcatenatingMediaSource(audioSource, videoSource);
    }*/

    public void setContent(Content content) {
        this.content = content;
    }

    public Content getContent() {
        return this.content;
    }
}
