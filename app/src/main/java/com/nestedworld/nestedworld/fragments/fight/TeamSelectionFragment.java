package com.nestedworld.nestedworld.fragments.fight;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.nestedworld.nestedworld.R;
import com.nestedworld.nestedworld.customView.viewpager.ViewPagerWithIndicator;
import com.nestedworld.nestedworld.fragments.base.BaseFragment;
import com.nestedworld.nestedworld.helpers.log.LogHelper;
import com.nestedworld.nestedworld.models.Combat;
import com.nestedworld.nestedworld.models.Monster;
import com.nestedworld.nestedworld.models.UserMonster;
import com.nestedworld.nestedworld.network.socket.implementation.NestedWorldSocketAPI;
import com.nestedworld.nestedworld.network.socket.implementation.SocketMessageType;
import com.nestedworld.nestedworld.network.socket.listener.ConnectionListener;
import com.nestedworld.nestedworld.network.socket.models.request.result.ResultRequest;
import com.orm.query.Condition;
import com.orm.query.Select;

import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.Bind;

public class TeamSelectionFragment extends BaseFragment implements ViewPager.OnPageChangeListener {

    @Bind(R.id.viewpager)
    ViewPager viewPager;
    @Bind(R.id.ViewPagerArrowIndicator)
    ViewPagerWithIndicator viewPagerArrowIndicator;
    @Bind(R.id.button_select_monster)
    Button button_select_monster;
    @Bind(R.id.tablerow_selected_monster)
    TableRow tableRow_selected_monster;
    @Bind(R.id.button_go_fight)
    Button button_go_fight;

    private List<UserMonster> mUserMonsters;
    private List<UserMonster> mSelectedMonster;
    private UserMonsterPagerAdapter mUserMonsterPagerAdapter;
    private Combat currentCombat;

    /*
    ** Public method
     */
    public static void load(@NonNull final FragmentManager fragmentManager, Combat currentCombat) {

        //Instantiate new fragment
        Fragment newFragment = new TeamSelectionFragment();

        //Add fragment param
        Bundle args = new Bundle();
        args.putLong("combatId", currentCombat.getId());
        newFragment.setArguments(args);

        //Display the fragment
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, newFragment);
        fragmentTransaction.commit();
    }

    /*
    ** Life cycle
     */
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_team_selection;
    }

    @Override
    protected void init(View rootView, Bundle savedInstanceState) {
        //Change action bar title
        changeActionBarName();

        //Get selectedCombat from arg
        parseArgs();

        //Retrieve monster and init selectedMonster list
        mUserMonsters = Select.from(UserMonster.class).list();
        mSelectedMonster = new ArrayList<>();

        //Init the viewPager (will display userMonster)
        setUpViewPager();

        //Init button 'start_fight' text (will display the state until we've selected enough monster)
        button_go_fight.setText(String.format(getResources().getString(R.string.teamSelection_msg_progress), 0));

        //Init button 'select_monster'
        button_select_monster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMonsterSelected();
                updateArrowState();
            }
        });
    }

    /*
    ** Private method
     */
    private void parseArgs() {
        //Check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        //Parse combatId
        Long combatId = getArguments().getLong("combatId", 0);

        //Retrieve Combat from Orm
        Combat combat = Select.from(Combat.class).where(Condition.prop("id").eq(combatId)).first();

        //Check if we successfully got the combat
        if (combat == null) {
            //Display an error message
            Toast.makeText(mContext, "An error occur, please try again", Toast.LENGTH_LONG).show();

            //Display some log
            LogHelper.d(TAG, "cannot parse combatId args");

            //Finish the current activity
            ((AppCompatActivity)mContext).finish();
        } else {
            this.currentCombat = combat;

            //Display some log
            LogHelper.d(TAG, "Combat= " + this.currentCombat.toString());
        }
    }

    private void setUpViewPager() {
        //check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        //Set up viewPager
        mUserMonsterPagerAdapter = new UserMonsterPagerAdapter(mContext, mUserMonsters);
        viewPager.setAdapter(mUserMonsterPagerAdapter);
        viewPagerArrowIndicator.setViewPager(viewPager);
        viewPager.addOnPageChangeListener(this);
    }

    private void onMonsterSelected() {
        //Get the selected monster
        UserMonster selectedMonster = mUserMonsters.get(viewPager.getCurrentItem());

        //Display some log
        LogHelper.d(TAG, "Monster selected: " + selectedMonster.toString());

        /*
        ** /!\ keep order : show monster and then add it in the list /!\
        ** (because show is based on mSelectedMonster.size())
         */
        //Show the selected monster
        tableRow_selected_monster.getChildAt(mSelectedMonster.size()).setBackgroundResource(R.drawable.default_monster);

        //Add the monster in the list (of selected monster)
        mSelectedMonster.add(selectedMonster);

        //Update 'start_fight' button with the current state
        button_go_fight.setText(String.format(getResources().getString(R.string.teamSelection_msg_progress), mSelectedMonster.size()));

        //If we've selected enough monster, we enable the button
        if (mSelectedMonster.size() == 4) {
            onEnoughMonsterSelected();
        }
    }

    private void onEnoughMonsterSelected() {
        //Change text on the button
        button_go_fight.setText(getResources().getString(R.string.teamSelection_msg_startFight));

        //Set a listener for starting the fight
        button_go_fight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NestedWorldSocketAPI.getInstance(new ConnectionListener() {
                    @Override
                    public void onConnectionReady(@NonNull NestedWorldSocketAPI nestedWorldSocketAPI) {
                        ValueFactory.MapBuilder map = ValueFactory.newMapBuilder();

                        List<Value> selectedMonsterIdList = new ArrayList<>();
                        for (UserMonster userMonster : mSelectedMonster) {
                            selectedMonsterIdList.add(ValueFactory.newInteger(userMonster.user_monster_id));
                        }

                        map.put(ValueFactory.newString("accept"), ValueFactory.newBoolean(true));
                        map.put(ValueFactory.newString("monsters"), ValueFactory.newArray(selectedMonsterIdList));

                        ResultRequest resultRequest = new ResultRequest(map.build().map(), true);
                        nestedWorldSocketAPI.sendRequest(resultRequest, SocketMessageType.MessageKind.TYPE_RESULT, currentCombat.message_id);
                    }

                    @Override
                    public void onConnectionLost() {

                    }

                    @Override
                    public void onMessageReceived(@NonNull SocketMessageType.MessageKind kind, @NonNull Map<Value, Value> content) {

                    }
                });
                FightFragment.load(getFragmentManager());
            }
        });
    }

    private void updateArrowState() {
        if (mSelectedMonster.contains(mUserMonsters.get(viewPager.getCurrentItem()))) {
            button_select_monster.setBackgroundResource(R.drawable.ic_action_arrow_bottom_red);
            button_select_monster.setClickable(false);
        } else {
            button_select_monster.setBackgroundResource(R.drawable.ic_action_arrow_bottom_blue);
            button_select_monster.setClickable(true);
        }
    }

    private void changeActionBarName() {
        //check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        if (mContext instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) mContext).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getResources().getString(R.string.teamSelection_title));
            }
        }
    }

    /*
    ** ViewPager.OnPageChangeListener implementation
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //DO What you want
    }

    @Override
    public void onPageSelected(int position) {
        updateArrowState();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //DO What you want
    }

    /**
     * Simple pager Adapter for displaying our monster
     */
    private static class UserMonsterPagerAdapter extends PagerAdapter {

        private final List<UserMonster> mUserMonsters;
        private final Context mContext;

        public UserMonsterPagerAdapter(@NonNull Context context, @NonNull List<UserMonster> userMonsters) {
            mUserMonsters = userMonsters;
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            //Create the view
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_monster, container, false);

            //Retrieve the monster we'll display
            UserMonster monster = mUserMonsters.get(position);
            Monster monsterInfo = monster.info();

            if (monsterInfo == null) {
                return null;
            }

            //Populate monster information
            Resources res = mContext.getResources();
            ((TextView) view.findViewById(R.id.textview_monster_name)).setText(String.format(res.getString(R.string.teamSelection_msg_monsterName), monsterInfo.name));
            ((TextView) view.findViewById(R.id.textview_monster_lvl)).setText(String.format(res.getString(R.string.teamSelection_msg_monsterLvl), monster.level));
            ((TextView) view.findViewById(R.id.textview_monster_hp)).setText(String.format(res.getString(R.string.teamSelection_msg_monsterHp), monsterInfo.hp));
            ((TextView) view.findViewById(R.id.textview_monster_attack)).setText(String.format(res.getString(R.string.teamSelection_msg_monsterAttack), monsterInfo.attack));
            ((TextView) view.findViewById(R.id.textview_monster_defense)).setText(String.format(res.getString(R.string.teamSelection_msg_monsterDefence), monsterInfo.defense));

            //Display monster picture
            final ImageView imageViewMonster = (ImageView) view.findViewById(R.id.imageView_monster);
            Glide.with(mContext)
                    .load(monsterInfo.sprite)
                    .placeholder(R.drawable.default_monster)
                    .centerCrop()
                    .into(imageViewMonster);

            //Add color shape around monster picture
            view.findViewById(R.id.imageView_monster_shape).setBackgroundColor(ContextCompat.getColor(mContext, monster.getColorResource()));

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return mUserMonsters.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            UserMonster monster = mUserMonsters.get(position);
            Monster monsterInfo = monster.info();
            if (monsterInfo == null) {
                return "";
            }
            return monsterInfo.name;
        }
    }
}
