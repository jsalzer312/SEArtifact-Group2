package org.fossasia.phimpme.accounts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.RelativeLayout;

import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxSession;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.pinterest.android.pdk.PDKCallback;
import com.pinterest.android.pdk.PDKClient;
import com.pinterest.android.pdk.PDKException;
import com.pinterest.android.pdk.PDKResponse;
import com.tumblr.loglr.Interfaces.ExceptionHandler;
import com.tumblr.loglr.Interfaces.LoginListener;
import com.tumblr.loglr.Loglr;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import org.fossasia.phimpme.R;
import org.fossasia.phimpme.base.PhimpmeProgressBarHandler;
import org.fossasia.phimpme.base.RecyclerItemClickListner;
import org.fossasia.phimpme.base.ThemedActivity;
import org.fossasia.phimpme.data.local.AccountDatabase;
import org.fossasia.phimpme.data.local.DatabaseHelper;
import org.fossasia.phimpme.share.flickr.FlickrActivity;
import org.fossasia.phimpme.share.imgur.ImgurAuthActivity;
import org.fossasia.phimpme.share.nextcloud.NextCloudAuth;
import org.fossasia.phimpme.share.owncloud.OwnCloudActivity;
import org.fossasia.phimpme.share.tumblr.TumblrClient;
import org.fossasia.phimpme.share.twitter.LoginActivity;
import org.fossasia.phimpme.utilities.ActivitySwitchHelper;
import org.fossasia.phimpme.utilities.BasicCallBack;
import org.fossasia.phimpme.utilities.Constants;
import org.fossasia.phimpme.utilities.SnackBarHandler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmQuery;

import static com.pinterest.android.pdk.PDKClient.setDebugMode;
import static org.fossasia.phimpme.R.string.no_account_signed_in;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.BOX;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.DROPBOX;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.FACEBOOK;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.IMGUR;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.NEXTCLOUD;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.OWNCLOUD;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.PINTEREST;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.TUMBLR;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.TWITTER;
import static org.fossasia.phimpme.utilities.Constants.BOX_CLIENT_ID;
import static org.fossasia.phimpme.utilities.Constants.BOX_CLIENT_SECRET;
import static org.fossasia.phimpme.utilities.Constants.DROPBOX_APP_KEY;
import static org.fossasia.phimpme.utilities.Constants.DROPBOX_APP_SECRET;
import static org.fossasia.phimpme.utilities.Constants.PINTEREST_APP_ID;
import static org.fossasia.phimpme.utilities.Constants.SUCCESS;
import static org.fossasia.phimpme.utilities.Utils.checkNetwork;

/**
 * Created by pa1pal on 13/6/17.
 */

public class AccountActivity extends ThemedActivity implements AccountContract.View,
        RecyclerItemClickListner.OnItemClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int NEXTCLOUD_REQUEST_CODE = 3;
    private static final int OWNCLOUD_REQUEST_CODE = 9;
    private static final int RESULT_OK = 1;
    private static final int RC_SIGN_IN = 9001;
    @BindView(R.id.accounts_parent)
    RelativeLayout parentLayout;
    @BindView(R.id.accounts_recycler_view)
    RecyclerView accountsRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.accounts)
    CoordinatorLayout coordinatorLayout;
    private AccountAdapter accountAdapter;
    private AccountPresenter accountPresenter;
    private Realm realm = Realm.getDefaultInstance();
    private RealmQuery<AccountDatabase> realmResult;
    private PhimpmeProgressBarHandler phimpmeProgressBarHandler;
    private TwitterAuthClient client;
    private CallbackManager callbackManager;
    private LoginManager loginManager;
    private AccountDatabase account;
    private DatabaseHelper databaseHelper;
    private Context context;
    private PDKClient pdkClient;
    private GoogleApiClient mGoogleApiClient;
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private BoxSession sessionBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        ActivitySwitchHelper.setContext(this);
        parentLayout.setBackgroundColor(getBackgroundColor());
        accountAdapter = new AccountAdapter(getAccentColor(), getPrimaryColor());
        accountPresenter = new AccountPresenter(realm);
        phimpmeProgressBarHandler = new PhimpmeProgressBarHandler(this);
        accountPresenter.attachView(this);
        databaseHelper = new DatabaseHelper(realm);
        client = new TwitterAuthClient();
        callbackManager = CallbackManager.Factory.create();
        setSupportActionBar(toolbar);
        loginManager = LoginManager.getInstance();
        toolbar.setPopupTheme(getPopupToolbarStyle());
        toolbar.setBackgroundColor(getPrimaryColor());
        setUpRecyclerView();
        accountPresenter.loadFromDatabase();  // Calling presenter function to load data from database
        getSupportActionBar().setTitle(R.string.title_account);
        phimpmeProgressBarHandler.show();
        pdkClient = PDKClient.configureInstance(this, PINTEREST_APP_ID);
        pdkClient.onConnect(this);
        setDebugMode(true);
        setupDropBox();
        //  googleApiClient();
        configureBoxClient();
    }

    private void setupDropBox() {
        AppKeyPair appKeys = new AppKeyPair(DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    /*    private void googleApiClient(){
            // Configure sign-in to request the user's ID, email address, and basic
            // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            // Build a GoogleApiClient with access to the Google Sign-In API and the
            // options specified by gso.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, AccountActivity.this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
        }*/
    private void configureBoxClient() {
        BoxConfig.CLIENT_ID = BOX_CLIENT_ID;
        BoxConfig.CLIENT_SECRET = BOX_CLIENT_SECRET;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_accounts_activity, menu);
        return true;
    }

    @Override
    public void setUpRecyclerView() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        accountsRecyclerView.setLayoutManager(layoutManager);
        accountsRecyclerView.setAdapter(accountAdapter);
        accountsRecyclerView.addOnItemTouchListener(new RecyclerItemClickListner(this, this));
    }

    @Override
    public void setUpAdapter(@NotNull RealmQuery<AccountDatabase> accountDetails) {
        this.realmResult = accountDetails;
        accountAdapter.setResults(realmResult);
    }

    @Override
    public void showError() {
        SnackBarHandler.show(coordinatorLayout, getString(no_account_signed_in));
    }

    @Override
    public void showComplete() {
        phimpmeProgressBarHandler.hide();
    }

    @Override
    public int getContentViewId() {
        return R.layout.activity_accounts;
    }

    @Override
    public int getNavigationMenuItemId() {
        return R.id.navigation_accounts;
    }

    @Override
    public void onItemClick(final View childView, final int position) {
        final SwitchCompat signInSignOut = (SwitchCompat) childView.findViewById(R.id.sign_in_sign_out_switch);
        final String name = AccountDatabase.AccountName.values()[position].toString();

        if (!signInSignOut.isChecked()) {
            if (!checkNetwork(this, parentLayout)) return;
            switch (AccountDatabase.AccountName.values()[position]) {
                case FACEBOOK:
                    signInFacebook();
                    break;

                case TWITTER:
                    signInTwitter();
                    break;

                /*case DRUPAL:
                    Intent drupalShare = new Intent(getContext(), DrupalLogin.class);
                    startActivity(drupalShare);
                    break;*/

                case NEXTCLOUD:
                    Intent nextCloudShare = new Intent(getContext(), NextCloudAuth.class);
                    startActivityForResult(nextCloudShare, NEXTCLOUD_REQUEST_CODE);
                    break;

                /*case WORDPRESS:
                    Intent WordpressShare = new Intent(this, WordpressLoginActivity.class);
                    startActivity(WordpressShare);
                    break;*/

                case PINTEREST:
                    signInPinterest();
                    break;

                case FLICKR:
                    signInFlickr();
                    break;

                case IMGUR:
                    signInImgur();
                    break;

                case DROPBOX:
                    signInDropbox();
                    break;

                case OWNCLOUD:
                    Intent ownCloudShare = new Intent(getContext(), OwnCloudActivity.class);
                    startActivityForResult(ownCloudShare, OWNCLOUD_REQUEST_CODE);
                    break;

                case BOX:
                    sessionBox = new BoxSession(AccountActivity.this);
                    sessionBox.authenticate();
                    break;
                case TUMBLR:
                    signInTumblr();
                    break;

                default:
                    SnackBarHandler.show(coordinatorLayout, R.string.feature_not_present);
            }
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(name)
                    .setTitle(getString(R.string.sign_out_dialog_title))
                    .setPositiveButton(R.string.yes_action,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    databaseHelper
                                            .deleteSignedOutAccount(name);
                                    accountAdapter.notifyDataSetChanged();
                                    accountPresenter.loadFromDatabase();
                                    signInSignOut.setChecked(false);
                                    BoxAuthentication.getInstance().logoutAllUsers(AccountActivity.this);
                                }
                            })
                    .setNegativeButton(R.string.no_action,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO: Implement negative button action
                                }
                            })
                    .show();
        }
    }

    private void signInFlickr() {
        BasicCallBack basicCallBack = new BasicCallBack() {
            @Override
            public void callBack(int status, Object data) {
                if (status == SUCCESS)
                    SnackBarHandler.show(coordinatorLayout, getString(R.string.logged_in_flickr));
            }
        };
        Intent intent = new Intent(this, FlickrActivity.class);
        FlickrActivity.setBasicCallBack(basicCallBack);
        startActivity(intent);
    }

    private void signInTumblr() {
        LoginListener loginListener = new LoginListener() {
            @Override
            public void onLoginSuccessful(com.tumblr.loglr.LoginResult loginResult) {
                SnackBarHandler.show(coordinatorLayout, getString(R.string.logged_in_tumblr));
                realm.beginTransaction();
                account = realm.createObject(AccountDatabase.class,
                        TUMBLR.toString());
                account.setToken(loginResult.getOAuthToken());
                account.setSecret(loginResult.getOAuthTokenSecret());
                account.setUsername(TUMBLR.toString());
                realm.commitTransaction();
                TumblrClient tumblrClient = new TumblrClient();
                realm.beginTransaction();
                BasicCallBack basicCallBack = new BasicCallBack() {
                    @Override
                    public void callBack(int status, Object data) {
                        account.setUsername(data.toString());
                        realm.commitTransaction();
                    }
                };
                tumblrClient.getName(basicCallBack);
            }
        };
        ExceptionHandler exceptionHandler = new ExceptionHandler() {
            @Override
            public void onLoginFailed(RuntimeException e) {
                SnackBarHandler.show(coordinatorLayout, R.string.error_volly);
            }
        };

        Loglr.getInstance()
                .setConsumerKey(Constants.TUMBLR_CONSUMER_KEY)
                .setConsumerSecretKey(Constants.TUMBLR_CONSUMER_SECRET)
                .setLoginListener(loginListener)
                .setExceptionHandler(exceptionHandler)
                .enable2FA(true)
                .setUrlCallBack(Constants.CALL_BACK_TUMBLR)
                .initiateInActivity(AccountActivity.this);
    }

    private void signInGooglePlus() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signInDropbox() {
        if (accountPresenter.checkAlreadyExist(DROPBOX))
            SnackBarHandler.show(coordinatorLayout, R.string.already_signed_in);
        else
            mDBApi.getSession().startOAuth2Authentication(this);
    }

    private void signInImgur() {
        BasicCallBack basicCallBack = new BasicCallBack() {
            @Override
            public void callBack(int status, Object data) {
                if (status == SUCCESS) {
                    SnackBarHandler.show(coordinatorLayout, R.string.account_logged);
                    if (data instanceof Bundle) {
                        Bundle bundle = (Bundle) data;
                        realm.beginTransaction();
                        account = realm.createObject(AccountDatabase.class, IMGUR.toString());
                        account.setUsername(bundle.getString(getString(R.string.auth_username)));
                        account.setToken(bundle.getString(getString(R.string.auth_token)));
                        realm.commitTransaction();
                    }
                }
            }
        };
        Intent i = new Intent(AccountActivity.this, ImgurAuthActivity.class);
        ImgurAuthActivity.setBasicCallBack(basicCallBack);
        startActivity(i);
    }

    private void signInPinterest() {
        List scopes = new ArrayList<String>();
        scopes.add(PDKClient.PDKCLIENT_PERMISSION_READ_PUBLIC);
        scopes.add(PDKClient.PDKCLIENT_PERMISSION_WRITE_PUBLIC);
        scopes.add(PDKClient.PDKCLIENT_PERMISSION_READ_RELATIONSHIPS);
        scopes.add(PDKClient.PDKCLIENT_PERMISSION_WRITE_RELATIONSHIPS);

        pdkClient.login(this, scopes, new PDKCallback() {
            @Override
            public void onSuccess(PDKResponse response) {
                Log.d(getClass().getName(), response.getData().toString());
                realm.beginTransaction();
                account = realm.createObject(AccountDatabase.class, PINTEREST.toString());
                account.setAccountname(PINTEREST);
                account.setUsername(response.getUser().getFirstName() + " " + response.getUser().getLastName());
                realm.commitTransaction();
                finish();
                startActivity(getIntent());
                SnackBarHandler.show(coordinatorLayout, getString(R.string.account_logged_pinterest));
            }

            @Override
            public void onFailure(PDKException exception) {
                Log.e(getClass().getName(), exception.getDetailMessage());
                SnackBarHandler.show(coordinatorLayout, R.string.pinterest_signIn_fail);
            }
        });
    }

    @Override
    public void onItemLongPress(View childView, int position) {
        // TODO: long press to implemented
    }

    /**
     * Create twitter login and session
     */
    public void signInTwitter() {
        BasicCallBack basicCallBack = new BasicCallBack() {
            @Override
            public void callBack(int status, Object data) {
                if (status == SUCCESS) {
                    SnackBarHandler.show(coordinatorLayout, getString(R.string.account_logged_twitter));
                    if (data instanceof Bundle) {
                        Bundle bundle = (Bundle) data;
                        realm.beginTransaction();
                        account = realm.createObject(AccountDatabase.class, TWITTER.toString());
                        account.setAccountname(TWITTER);
                        account.setUsername(bundle.getString(getString(R.string.auth_username)));
                        account.setToken(bundle.getString(getString(R.string.auth_token)));
                        account.setSecret(bundle.getString(getString(R.string.auth_secret)));
                        realm.commitTransaction();
                    }
                }
            }
        };
        Intent i = new Intent(AccountActivity.this, LoginActivity.class);
        LoginActivity.setBasicCallBack(basicCallBack);
        startActivity(i);
    }


    /**
     * Create Facebook login and session
     */
    public void signInFacebook() {

        loginManager = LoginManager.getInstance();
        loginManager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        loginManager.registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        realm.beginTransaction();
                        account = realm.createObject(AccountDatabase.class, FACEBOOK.toString());
                        account.setUsername(loginResult.getAccessToken().getUserId());
                        GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                                        Log.v("LoginActivity", graphResponse.toString());
                                        try {
                                            account.setUsername(jsonObject.getString("email"));
                                            SnackBarHandler.show(coordinatorLayout, getString(R.string.logged_in_facebook));
                                        } catch (JSONException e) {
                                            Log.e("LoginAct", e.toString());
                                        }
                                    }
                                });
                        realm.commitTransaction();
                    }

                    @Override
                    public void onCancel() {
                        SnackBarHandler.show(coordinatorLayout, getString(R.string.facebook_login_cancel));
                    }

                    @Override
                    public void onError(FacebookException e) {
                        SnackBarHandler.show(coordinatorLayout, getString(R.string.facebook_login_error));
                        Log.d("error", e.toString());
                    }
                });
    }

    @Override
    public Context getContext() {
        this.context = this;
        return context;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivitySwitchHelper.setContext(this);
        dropboxAuthentication();
        boxAuthentication();
        setStatusBarColor();
        setNavBarColor();
        accountPresenter.loadFromDatabase();
    }

    private void boxAuthentication() {
        if (sessionBox != null && sessionBox.getUser() != null) {
            String accessToken = sessionBox.getAuthInfo().accessToken();

            realm.beginTransaction();

            // Creating Realm object for AccountDatabase Class
            account = realm.createObject(AccountDatabase.class,
                    BOX.toString());

            // Writing values in Realm database

            account.setUsername(sessionBox.getUser().getName());
            account.setToken(String.valueOf(accessToken));

            // Finally committing the whole data
            realm.commitTransaction();
            accountPresenter.loadFromDatabase();
        }
    }

    private void dropboxAuthentication() {
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();
                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                realm.beginTransaction();
                account = realm.createObject(AccountDatabase.class, DROPBOX.toString());
                account.setUsername(DROPBOX.toString());
                account.setToken(String.valueOf(accessToken));
                realm.commitTransaction();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
            accountPresenter.loadFromDatabase();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        client.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        pdkClient.onOauthResponse(requestCode, resultCode, data);

        if ((requestCode == OWNCLOUD_REQUEST_CODE && resultCode == RESULT_OK) || (requestCode == NEXTCLOUD_REQUEST_CODE && resultCode == RESULT_OK)) {
            realm.beginTransaction();
            if (requestCode == NEXTCLOUD_REQUEST_CODE) {
                account = realm.createObject(AccountDatabase.class, NEXTCLOUD.toString());
            } else {
                account = realm.createObject(AccountDatabase.class, OWNCLOUD.toString());
            }
            account.setServerUrl(data.getStringExtra(getString(R.string.server_url)));
            account.setUsername(data.getStringExtra(getString(R.string.auth_username)));
            account.setPassword(data.getStringExtra(getString(R.string.auth_password)));
            realm.commitTransaction();
        }
     /*   if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }*/
    }

    /*private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();//acct.getDisplayName()
            SnackBarHandler.show(parentLayout,R.string.success);
            realm.beginTransaction();
            account = realm.createObject(AccountDatabase.class, GOOGLEPLUS.name());account.setUsername(acct.getDisplayName());
            account.setUserId(acct.getId());
            realm.commitTransaction();
        } else {
            SnackBarHandler.show(parentLayout,R.string.google_auth_fail);
        }
    }*/

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        SnackBarHandler.show(coordinatorLayout, "Connection Failed");
    }


}
