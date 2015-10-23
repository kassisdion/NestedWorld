package com.nestedworld.nestedworld.fragment.mainMenu.tabs;

import com.nestedworld.nestedworld.R;
import com.nestedworld.nestedworld.adapter.MonsterAdapter;
import com.nestedworld.nestedworld.api.implementation.NestedWorldApi;
import com.nestedworld.nestedworld.api.models.apiResponse.monsters.MonstersList;
import com.nestedworld.nestedworld.fragment.base.BaseFragment;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.ListView;

import butterknife.Bind;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A placeholder fragment containing a simple view.
 */
public class MonstersFragment extends BaseFragment {

    public final static String FRAGMENT_NAME = MonstersFragment.class.getSimpleName();

    @Bind(R.id.listview_monsters_list)
    ListView listViewMonstersList;

    public static void load(final FragmentManager fragmentManager) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, new MonstersFragment());
        fragmentTransaction.addToBackStack(FRAGMENT_NAME);
        fragmentTransaction.commit();
    }

    /*
    ** Life cycle
     */
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_tab_monsters;
    }

    @Override
    protected void initUI(Bundle savedInstanceState) {

    }

    @Override
    protected void initLogic(Bundle savedInstanceState) {
        //TODO display a loading animation
        NestedWorldApi.getInstance().getMonstersList(
                new Callback<MonstersList>() {
                    @Override
                    public void success(final MonstersList json, Response response) {
                        final MonsterAdapter adapter = new MonsterAdapter(mContext, json.monsters);
                        // listViewMonstersList = null if we've change view before the end of the request
                        if (listViewMonstersList != null) {
                            listViewMonstersList.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        //TODO no monster to display
                    }
                }
        );

    }
}
