package com.nestedworld.nestedworld.helpers.database.updater.entity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nestedworld.nestedworld.event.http.OnAttacksUpdatedEvent;
import com.nestedworld.nestedworld.helpers.database.updater.callback.OnEntityUpdated;
import com.nestedworld.nestedworld.models.Attack;
import com.nestedworld.nestedworld.network.http.implementation.NestedWorldHttpApi;
import com.nestedworld.nestedworld.network.http.models.response.attack.AttacksResponse;

import org.greenrobot.eventbus.EventBus;

import retrofit2.Call;
import retrofit2.Response;

public class AttacksUpdater extends EntityUpdater<AttacksResponse> {

    public AttacksUpdater(@NonNull Context context, @Nullable OnEntityUpdated callback) {
        super(context, callback);
    }

    @NonNull
    @Override
    public Call<AttacksResponse> getRequest() {
        return NestedWorldHttpApi.getInstance(getContext()).getAttacks();
    }

    @Override
    public void updateEntity(@NonNull Response<AttacksResponse> response) {
        //Delete old entity
        Attack.deleteAll(Attack.class);

        //Save entity
        Attack.saveInTx(response.body().attacks);

        //Send event
        EventBus.getDefault().post(new OnAttacksUpdatedEvent());
    }
}
