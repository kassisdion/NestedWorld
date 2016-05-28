package com.nestedworld.nestedworld.fragments.profil;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

import com.nestedworld.nestedworld.R;
import com.nestedworld.nestedworld.activities.registration.RegistrationActivity;
import com.nestedworld.nestedworld.fragments.base.BaseFragment;
import com.nestedworld.nestedworld.helpers.log.LogHelper;
import com.nestedworld.nestedworld.helpers.session.SessionManager;
import com.nestedworld.nestedworld.models.Session;
import com.nestedworld.nestedworld.models.User;
import com.nestedworld.nestedworld.network.http.implementation.NestedWorldHttpApi;
import com.nestedworld.nestedworld.network.http.models.response.users.auth.LogoutResponse;
import com.nestedworld.nestedworld.network.socket.implementation.NestedWorldSocketAPI;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit2.Response;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProfileFragment extends BaseFragment {

    public final static String FRAGMENT_NAME = ProfileFragment.class.getSimpleName();

    @Bind(R.id.textView_gender)
    TextView textViewGender;
    @Bind(R.id.textView_pseudo)
    TextView textViewPseudo;
    @Bind(R.id.textView_birthDate)
    TextView textViewBirthDate;
    @Bind(R.id.textView_city)
    TextView textViewCity;
    @Bind(R.id.textView_registeredAt)
    TextView textViewRegisteredAt;
    @Bind(R.id.textView_email)
    TextView textViewEmail;

    /*
    ** Public method
     */
    public static void load(@NonNull final FragmentManager fragmentManager, final boolean toBackStack) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, new ProfileFragment());
        if (toBackStack) {
            fragmentTransaction.addToBackStack(FRAGMENT_NAME);
        }
        fragmentTransaction.commit();
    }

    /*
    ** Life cycle
     */
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_action_profil;
    }

    @Override
    protected void init(View rootView, Bundle savedInstanceState) {
        //Retrieve the session
        Session session = SessionManager.get().getSession();
        if (session == null) {
            LogHelper.d(TAG, "No Session");
            onFatalError();
            return;
        }

        //Retrieve the user
        User user = session.getUser();
        if (user == null) {
            LogHelper.d(TAG, "No User");
            onFatalError();
            return;
        }

        /*We display some information*/
        textViewGender.setText(user.gender);
        textViewPseudo.setText(user.pseudo);
        textViewBirthDate.setText(user.birth_date);
        textViewCity.setText(user.city);
        textViewRegisteredAt.setText(user.registered_at);
        textViewEmail.setText(user.email);
    }


    /*
    ** Butterknife callback
     */
    @OnClick(R.id.button_logout)
    public void logout() {
        if (mContext == null)
            return;
        NestedWorldHttpApi.getInstance(mContext).logout(
                new com.nestedworld.nestedworld.network.http.callback.Callback<LogoutResponse>() {
                    @Override
                    public void onSuccess(Response<LogoutResponse> response) {
                        //Server has accept our logout
                    }

                    @Override
                    public void onError(@NonNull KIND errorKind, @Nullable Response<LogoutResponse> response) {
                        //Server refuse our logout
                    }
                });

        //remove user
        SessionManager.get().deleteSession();

        //avoid leek with the static instance
        NestedWorldHttpApi.reset();
        NestedWorldSocketAPI.reset();

        //go to launch screen & kill the current context
        Intent intent = new Intent(mContext, RegistrationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
