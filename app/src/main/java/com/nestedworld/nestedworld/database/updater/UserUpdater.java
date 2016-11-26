package com.nestedworld.nestedworld.database.updater;

import android.support.annotation.NonNull;

import com.nestedworld.nestedworld.analytics.NestedWorldAnalytics;
import com.nestedworld.nestedworld.database.models.Player;
import com.nestedworld.nestedworld.database.models.Session;
import com.nestedworld.nestedworld.database.updater.base.EntityUpdater;
import com.nestedworld.nestedworld.events.http.OnUserUpdatedEvent;
import com.nestedworld.nestedworld.helpers.session.SessionHelper;
import com.nestedworld.nestedworld.network.http.models.response.users.UserResponse;

import org.greenrobot.eventbus.EventBus;

import retrofit2.Call;
import retrofit2.Response;

public class UserUpdater extends EntityUpdater<UserResponse> {

    /*
    ** Life cycle
     */
    @NonNull
    @Override
    public Call<UserResponse> getRequest() {
        return getApi().getUserInfo();
    }

    @Override
    public void updateEntity(@NonNull final Response<UserResponse> response) {
        //Delete old entity
        Session session = SessionHelper.getSession();
        if (session != null) {
            Player oldUser = session.getPlayer();
            if (oldUser != null) {
                oldUser.delete();
            }
        }

        Player player = response.body().player;

        //Add player information in our crash repport
        NestedWorldAnalytics.setUserInfo(player);

        //Save entity
        getDatabase().getPlayerDao().insertOrReplace(response.body().player);

        //Send event
        EventBus.getDefault().post(new OnUserUpdatedEvent());
    }
}
