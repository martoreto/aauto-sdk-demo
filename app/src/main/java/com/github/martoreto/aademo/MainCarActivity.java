package com.github.martoreto.aademo;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarToast;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.MenuController;
import com.google.android.apps.auto.sdk.MenuItem;
import com.google.android.apps.auto.sdk.StatusBarController;
import com.google.android.apps.auto.sdk.notification.CarNotificationExtender;

class MainCarActivity extends CarActivity {
    private static final String TAG = "MainCarActivity";

    static final String MENU_HOME = "home";
    static final String MENU_DEBUG = "debug";
    static final String MENU_DEBUG_LOG = "log";
    static final String MENU_DEBUG_TEST_NOTIFICATION = "test_notification";

    private static final String FRAGMENT_DEMO = "demo";
    private static final String FRAGMENT_LOG = "log";

    private static final String CURRENT_FRAGMENT_KEY = "app_current_fragment";

    private static final int TEST_NOTIFICATION_ID = 1;

    private String mCurrentFragmentTag;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle bundle) {
        setTheme(R.style.AppTheme_Car);
        super.onCreate(bundle);
        setContentView(R.layout.activity_car_main);

        CarUiController carUiController = getCarUiController();
        carUiController.getStatusBarController().showTitle();

        FragmentManager fragmentManager = getSupportFragmentManager();
        DemoFragment demoFragment = new DemoFragment();
        LogFragment logFragment = new LogFragment();
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, demoFragment, FRAGMENT_DEMO)
                .detach(demoFragment)
                .add(R.id.fragment_container, logFragment, FRAGMENT_LOG)
                .detach(logFragment)
                .commitNow();

        String initialFragmentTag = FRAGMENT_DEMO;
        if (bundle != null && bundle.containsKey(CURRENT_FRAGMENT_KEY)) {
            initialFragmentTag = bundle.getString(CURRENT_FRAGMENT_KEY);
        }
        switchToFragment(initialFragmentTag);

        ListMenuAdapter mainMenu = new ListMenuAdapter();
        mainMenu.setCallbacks(mMenuCallbacks);
        mainMenu.addMenuItem(MENU_HOME, new MenuItem.Builder()
                .setTitle(getString(R.string.demo_title))
                .setType(MenuItem.Type.ITEM)
                .build());
        mainMenu.addMenuItem(MENU_DEBUG, new MenuItem.Builder()
                .setTitle(getString(R.string.menu_debug_title))
                .setType(MenuItem.Type.SUBMENU)
                .build());

        ListMenuAdapter debugMenu = new ListMenuAdapter();
        debugMenu.setCallbacks(mMenuCallbacks);
        debugMenu.addMenuItem(MENU_DEBUG_LOG, new MenuItem.Builder()
                .setTitle(getString(R.string.menu_exlap_stats_log_title))
                .setType(MenuItem.Type.ITEM)
                .build());
        debugMenu.addMenuItem(MENU_DEBUG_TEST_NOTIFICATION, new MenuItem.Builder()
                .setTitle(getString(R.string.menu_test_notification_title))
                .setType(MenuItem.Type.ITEM)
                .build());
        mainMenu.addSubmenu(MENU_DEBUG, debugMenu);

        MenuController menuController = carUiController.getMenuController();
        menuController.setRootMenuAdapter(mainMenu);
        menuController.showMenuButton();

        StatusBarController statusBarController = carUiController.getStatusBarController();
        statusBarController.setAppBarAlpha(0.5f);
        statusBarController.setAppBarBackgroundColor(0xffff0000);

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentLifecycleCallbacks,
                false);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString(CURRENT_FRAGMENT_KEY, mCurrentFragmentTag);
        super.onSaveInstanceState(bundle);
    }

    private final ListMenuAdapter.MenuCallbacks mMenuCallbacks = new ListMenuAdapter.MenuCallbacks() {
        @Override
        public void onMenuItemClicked(String name) {
            switch (name) {
                case MENU_HOME:
                    switchToFragment(FRAGMENT_DEMO);
                    break;
                case MENU_DEBUG_LOG:
                    switchToFragment(FRAGMENT_LOG);
                    break;
                case MENU_DEBUG_TEST_NOTIFICATION:
                    showTestNotification();
                    break;
            }
        }

        @Override
        public void onEnter() {
        }

        @Override
        public void onExit() {
            updateStatusBarTitle();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        switchToFragment(mCurrentFragmentTag);
    }

    private void switchToFragment(String tag) {
        if (tag.equals(mCurrentFragmentTag)) {
            return;
        }
        FragmentManager manager = getSupportFragmentManager();
        Fragment currentFragment = mCurrentFragmentTag == null ? null : manager.findFragmentByTag(mCurrentFragmentTag);
        Fragment newFragment = manager.findFragmentByTag(tag);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) {
            transaction.detach(currentFragment);
        }
        transaction.attach(newFragment);
        transaction.commit();
        mCurrentFragmentTag = tag;
    }

    private final FragmentManager.FragmentLifecycleCallbacks mFragmentLifecycleCallbacks
            = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentStarted(FragmentManager fm, Fragment f) {
            updateStatusBarTitle();
        }
    };

    private void updateStatusBarTitle() {
        CarFragment fragment = (CarFragment) getSupportFragmentManager().findFragmentByTag(mCurrentFragmentTag);
        getCarUiController().getStatusBarController().setTitle(fragment.getTitle());
    }

    private void showTestNotification() {
        CarToast.makeText(this, "Will show notification in 5 seconds", Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Notification notification = new NotificationCompat.Builder(MainCarActivity.this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Test notification")
                        .setContentText("This is a test notification")
                        .setAutoCancel(true)
                        .extend(new CarNotificationExtender.Builder()
                                .setTitle("Test")
                                .setSubtitle("This is a test notification")
                                .setActionIconResId(R.mipmap.ic_launcher)
                                .setThumbnail(CarUtils.getCarBitmap(MainCarActivity.this,
                                        R.mipmap.ic_launcher, R.color.car_primary, 128))
                                .setShouldShowAsHeadsUp(true)
                                .build())
                        .build();

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(TAG, TEST_NOTIFICATION_ID, notification);

                CarNotificationSoundPlayer soundPlayer = new CarNotificationSoundPlayer(
                        MainCarActivity.this, R.raw.bubble);
                soundPlayer.play();
            }
        }, 5000);
    }
}
