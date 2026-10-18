package com.nedaqalam.school;

import android.app.Application;

public class App extends Application {
    public static App self;
    public static User me; // current logged-in user

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        Store.init(this);
    }

    public static boolean isManager() {
        return me != null && User.ROLE_ADMIN.equals(me.role);
    }
}
