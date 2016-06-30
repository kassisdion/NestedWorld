package com.nestedworld.nestedworld.fragments.fight;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nestedworld.nestedworld.R;
import com.nestedworld.nestedworld.customView.drawingGestureView.DrawingGestureView;
import com.nestedworld.nestedworld.customView.drawingGestureView.listener.DrawingGestureListener;
import com.nestedworld.nestedworld.customView.drawingGestureView.listener.OnFinishMoveListener;
import com.nestedworld.nestedworld.fragments.base.BaseFragment;
import com.nestedworld.nestedworld.network.socket.implementation.NestedWorldSocketAPI;
import com.nestedworld.nestedworld.network.socket.listener.ConnectionListener;
import com.nestedworld.nestedworld.network.socket.models.request.combat.SendAttackRequest;
import com.rey.material.widget.ProgressView;

import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import butterknife.Bind;

public class FightFragment extends BaseFragment {

    private final ArrayList<Integer> mPositions = new ArrayList<>();
    @Bind(R.id.progressView)
    ProgressView progressView;
    private NestedWorldSocketAPI mNestedWorldSocketAPI;

    /*
    ** Public method
     */
    public static void load(@NonNull final FragmentManager fragmentManager) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, new FightFragment());
        fragmentTransaction.commit();
    }

    /*
    ** Life cycle
     */
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_action_fight;
    }

    @Override
    protected void init(final View rootView, Bundle savedInstanceState) {
        /*start a loading animation*/
        progressView.start();

        /*Init the socket*/
        NestedWorldSocketAPI.getInstance(new ConnectionListener() {
            @Override
            public void onConnectionReady(@NonNull NestedWorldSocketAPI nestedWorldSocketAPI) {
                /*Socket successfully init*/
                mNestedWorldSocketAPI = nestedWorldSocketAPI;

                /*Need to auth*/
                mNestedWorldSocketAPI.authRequest("AUTH_REQUEST");
            }

            @Override
            public void onConnectionLost() {
                //Check if fragment hasn't been detach
                if (mContext == null) {
                    return;
                }

                /*Stop the loading animation and display an error message*/
                if (progressView != null) {
                    progressView.stop();
                }

                /*Display an error message*/
                //TODO use string from xml
                Toast.makeText(mContext, "Connexion impossible", Toast.LENGTH_LONG).show();

                /*Stop the activity (can't run without connection)*/
                getActivity().finish();
            }

            @Override
            public void onMessageReceived(@NonNull String requestId, @NonNull Map<Value, Value> content) {
                //Check if fragment hasn't been detach
                if (mContext == null) {
                    return;
                }

                if (requestId.equals("AUTH_REQUEST")) {
                    if (content.get(ValueFactory.newString("result")).asStringValue().asString().equals("success")) {
                        /*Stop the loading animation*/
                        progressView.stop();

                        /*Init the custom view (for sending attack)*/
                        initDrawingGestureView(rootView);
                    }

                }

            }
        });
    }

    private void initDrawingGestureView(View rootView) {
        if (mContext == null) {
            return;
        }

        /*We create a list with every tile*/
        final List<ImageView> tiles = Arrays.asList(
                (ImageView) rootView.findViewById(R.id.imageView_top),
                (ImageView) rootView.findViewById(R.id.imageView_top_right),
                (ImageView) rootView.findViewById(R.id.imageView_right),
                (ImageView) rootView.findViewById(R.id.imageView_bottom_right),
                (ImageView) rootView.findViewById(R.id.imageView_bottom),
                (ImageView) rootView.findViewById(R.id.imageView_bottom_left),
                (ImageView) rootView.findViewById(R.id.imageView_left),
                (ImageView) rootView.findViewById(R.id.imageView_top_left));

        /*Create and init the custom view*/
        DrawingGestureView drawingGestureView = new DrawingGestureView(mContext);
        drawingGestureView.setTiles(tiles);
        drawingGestureView.setOnTileTouchListener(new DrawingGestureListener() {
            @Override
            public void onTouch(int tileId) {
                if (!mPositions.contains(tileId)) {
                    mPositions.add(tileId);
                }
            }
        });
        drawingGestureView.setOnFinishMoveListener(new OnFinishMoveListener() {
            @Override
            public void onFinish() {
                SendAttackRequest data = new SendAttackRequest();

                String buf = "0";
                for (int i : mPositions) {
                    buf += i;
                }

                data.target = 10;
                data.attack = Integer.parseInt(buf);

                mNestedWorldSocketAPI.combatRequest(data);
                mPositions.clear();
            }
        });

        /*Add the custom view under the rootView*/
        ((RelativeLayout) rootView.findViewById(R.id.relativeLayout_fight)).addView(drawingGestureView);
    }

}
