package com.nestedworld.nestedworld.ui.fight;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nestedworld.nestedworld.R;
import com.nestedworld.nestedworld.customView.drawingGestureView.DrawingGestureView;
import com.nestedworld.nestedworld.customView.drawingGestureView.listener.DrawingGestureListener;
import com.nestedworld.nestedworld.customView.drawingGestureView.listener.OnFinishMoveListener;
import com.nestedworld.nestedworld.event.socket.combat.OnAttackReceiveEvent;
import com.nestedworld.nestedworld.event.socket.combat.OnMonsterKoEvent;
import com.nestedworld.nestedworld.helpers.service.ServiceHelper;
import com.nestedworld.nestedworld.network.socket.implementation.NestedWorldSocketAPI;
import com.nestedworld.nestedworld.network.socket.implementation.SocketMessageType;
import com.nestedworld.nestedworld.network.socket.models.message.combat.AttackReceiveMessage;
import com.nestedworld.nestedworld.network.socket.models.message.combat.MonsterKoMessage;
import com.nestedworld.nestedworld.network.socket.models.message.combat.StartMessage;
import com.nestedworld.nestedworld.network.socket.models.request.combat.SendAttackRequest;
import com.nestedworld.nestedworld.service.SocketService;
import com.nestedworld.nestedworld.ui.base.BaseFragment;
import com.rey.material.widget.ProgressView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;

public class FightFragment extends BaseFragment {

    private final ArrayList<Integer> mPositions = new ArrayList<>();
    @Bind(R.id.progressView)
    ProgressView progressView;
    @Bind(R.id.layout_player)
    LinearLayout layoutPlayer;
    @Bind(R.id.layout_opponent)
    LinearLayout layoutOpponent;

    private DrawingGestureView mDrawingGestureView;
    private StartMessage mStartMessage;

    /*
    ** Public method
     */
    public static void load(@NonNull final FragmentManager fragmentManager, @NonNull final StartMessage startMessage) {
        FightFragment fightFragment = new FightFragment();
        fightFragment.setStartMessage(startMessage);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, fightFragment);
        fragmentTransaction.commit();
    }

    public void setStartMessage(@NonNull final StartMessage startMessage) {
        mStartMessage = startMessage;
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
        //start loading animation
        progressView.start();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        setupActionBar();

        /*populate the view*/
        setupEnvironment();
        initMonsterLayout(layoutOpponent, mStartMessage.opponent.monster);
        initMonsterLayout(layoutPlayer, mStartMessage.user.monster);

        /*Init the gestureListener*/
        initDrawingGestureView(rootView);
    }

    /*
    ** EventBus
     */
    @Subscribe
    public void onAttackReceive(OnAttackReceiveEvent event) {
        AttackReceiveMessage message = event.getMessage();

        //TODO parse message
    }

    @Subscribe
    public void onMonsterKo(OnMonsterKoEvent event) {
        MonsterKoMessage monsterKoMessage = event.getMessage();

        //TODO parse message
    }


    /*
    ** Private method
     */
    private void setupActionBar() {
        //Check if fragment hasn't been detach
        if (mContext != null) {
            /*Update toolbar title*/
            ActionBar actionBar = ((AppCompatActivity) mContext).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getResources().getString(R.string.combat_title));
            }
        }
    }

    private void setupEnvironment() {
        //TODO parse mStartMessage.env and set background

        switch (mStartMessage.env) {
            case "city":
                break;
            default:
                break;
        }
    }

    private void initDrawingGestureView(View rootView) {
        if (mContext == null) {
            return;
        }

        /*We create a list with every tile*/
        final List<ImageView> tiles = Arrays.asList(
                (ImageView) rootView.findViewById(R.id.imageView_top),
                (ImageView) rootView.findViewById(R.id.imageView_top_right),
                (ImageView) rootView.findViewById(R.id.imageView_bottom_right),
                (ImageView) rootView.findViewById(R.id.imageView_bottom),
                (ImageView) rootView.findViewById(R.id.imageView_bottom_left),
                (ImageView) rootView.findViewById(R.id.imageView_top_left));

        /*Create and init the custom view*/
        mDrawingGestureView = new DrawingGestureView(mContext);
        mDrawingGestureView.setEnabled(false);
        mDrawingGestureView.setTiles(tiles);
        mDrawingGestureView.setOnTileTouchListener(new DrawingGestureListener() {
            @Override
            public void onTouch(int tileId) {
                if (!mPositions.contains(tileId)) {
                    mPositions.add(tileId);
                }
            }
        });
        mDrawingGestureView.setOnFinishMoveListener(new OnFinishMoveListener() {
            @Override
            public void onFinish() {
                sendAttack();
            }
        });

        /*Add the custom view under the rootView*/
        ((RelativeLayout) rootView.findViewById(R.id.layout_fight_body)).addView(mDrawingGestureView);
    }

    private void initMonsterLayout(@NonNull final LinearLayout layout, @NonNull final StartMessage.PlayerMonster monster) {
        //Retrieve widget
        TextView opponentName = (TextView) layout.findViewById(R.id.textViewOpponentName);
        TextView monsterLvl = (TextView) layout.findViewById(R.id.textview_monster_lvl);
        TextView monsterHp = (TextView) layout.findViewById(R.id.textview_monster_hp);
        ImageView monsterPicture = (ImageView) layout.findViewById(R.id.imageView_monster);

        //Populate widget
        opponentName.setText(monster.name);
        monsterLvl.setText(String.format(getResources().getString(R.string.combat_msg_monster_lvl), monster.level));
        monsterHp.setText(String.format(getResources().getString(R.string.combat_msg_monster_hp), monster.hp));

        //TODO retrieve sprite and populate picture
//        Glide.with(mContext)
//                .load(monster.sprite)
//                .placeholder(R.drawable.default_monster)
//                .centerCrop()
//                .into(monsterPicture);
    }

    private void sendAttack() {
        //check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        ServiceHelper.bindToSocketService(mContext, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                NestedWorldSocketAPI nestedWorldSocketAPI = ((SocketService.LocalBinder) service).getService().getApiInstance();
                if (nestedWorldSocketAPI != null) {
                    SendAttackRequest data = new SendAttackRequest(mStartMessage.opponent.monster.id, 10);
                    nestedWorldSocketAPI.sendRequest(data, SocketMessageType.MessageKind.TYPE_COMBAT_SEND_ATTACK);
                    mPositions.clear();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(mContext, R.string.combat_msg_send_atk_failed, Toast.LENGTH_LONG).show();
            }
        });
    }
}
