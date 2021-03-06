package com.uncmorfi.menu;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.uncmorfi.R;
import com.uncmorfi.helpers.SnackbarHelper;
import com.uncmorfi.helpers.ConnectionHelper;
import com.uncmorfi.helpers.MemoryHelper;

import java.io.File;
import java.util.Calendar;
import java.util.Date;


public class MenuFragment extends Fragment implements RefreshMenuTask.RefreshMenuListener {
    public static final String MENU_FILE = "menu.txt";
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WebView mWebView;
    private Snackbar lastSnackBar;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        mWebView = (WebView) view.findViewById(R.id.menu_content);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.menu_swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshMenu();
                    }
                }
        );

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(
                R.color.accent);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.white,
                R.color.primary_light
        );

        String menuSaved = MemoryHelper.readFileFromStorage(getContext(), MENU_FILE);
        if (menuSaved != null) {
            mWebView.loadData(menuSaved, "text/html", "UTF-8");
        }

        if (needRefreshMenu()) {
            refreshMenu();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.navigation_menu);
    }

    @Override
    public void onStop() {
        super.onStop();
        mSwipeRefreshLayout.setRefreshing(false);
        if (lastSnackBar != null && lastSnackBar.isShown())
            lastSnackBar.dismiss();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() ==  R.id.menu_update) {
            refreshMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshMenu() {
        if (ConnectionHelper.isOnline(getContext())) {
            mSwipeRefreshLayout.setRefreshing(true);
            new RefreshMenuTask(this).execute();
        } else {
            showSnackBarMsg(R.string.no_connection, SnackbarHelper.SnackType.ERROR);
        }
    }

    @Override
    public void onRefreshMenuSuccess(String menu) {
        if (needSaveMenu(menu)) {
            MemoryHelper.saveToStorage(getContext(), MenuFragment.MENU_FILE, menu);
        }

        if (getActivity() != null && isAdded()) {
            mWebView.loadDataWithBaseURL(null, menu, "text/html", "UTF-8", null);

            showSnackBarMsg(R.string.update_success, SnackbarHelper.SnackType.FINISH);
        }
    }

    @Override
    public void onRefreshMenuFail() {
        showSnackBarMsg(R.string.update_fail, SnackbarHelper.SnackType.ERROR);
    }

    private void showSnackBarMsg(int resId, SnackbarHelper.SnackType type) {
        if (getActivity() != null && isAdded() && resId != 0) {
            lastSnackBar = Snackbar.make(mWebView, resId, SnackbarHelper.getLength(type));
            SnackbarHelper.getColored(getContext(), lastSnackBar, type).show();

            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private boolean needRefreshMenu() {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        int nowWeek = now.get(Calendar.WEEK_OF_YEAR);

        Calendar menu = Calendar.getInstance();
        menu.setTimeInMillis(getMenuLastModified());
        int menuWeek = menu.get(Calendar.WEEK_OF_YEAR);

        return (menuWeek < nowWeek);
    }

    private long getMenuLastModified() {
        File menuFile = new File(getContext().getFilesDir() + "/" + MENU_FILE);
        return menuFile.lastModified();
    }

    private boolean needSaveMenu(String menu) {
        String menuSaved = MemoryHelper.readHeadFromStorage(getContext(), MENU_FILE);
        return (menuSaved == null || !menu.startsWith(menuSaved));
    }

}