/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *      Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.account.AccountDetailBasic;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.account.AccountDetailTls;
import org.sflphone.client.AccountEditionActivity;
import org.sflphone.client.AccountWizard;
import org.sflphone.client.SFLPhonePreferenceActivity;
import org.sflphone.client.SFLphoneApplication;
import org.sflphone.model.Account;
import org.sflphone.service.ConfigurationManagerCallback;
import org.sflphone.service.ISipService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class AccountManagementFragment extends PreferenceFragment {
    static final String TAG = "AccountManagementFragment";
    static final String DEFAULT_ACCOUNT_ID = "IP2IP";
    static final int ACCOUNT_CREATE_REQUEST = 1;
    static final int ACCOUNT_EDIT_REQUEST = 2;
    private SFLPhonePreferenceActivity sflphonePreferenceActivity;
    private ISipService service = null;

    // ArrayList<AccountDetail.PreferenceEntry> basicDetailKeys = null;
    // ArrayList<AccountDetail.PreferenceEntry> advancedDetailKeys = null;
    // ArrayList<AccountDetail.PreferenceEntry> srtpDetailKeys = null;
    // ArrayList<AccountDetail.PreferenceEntry> tlsDetailKeys = null;
    HashMap<String, Preference> accountPreferenceHashMap = null;
    PreferenceScreen mRoot = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        sflphonePreferenceActivity = (SFLPhonePreferenceActivity) activity;
        service = sflphonePreferenceActivity.getSipService();
        Log.w(TAG, "onAttach() service=" + service);
    }

    public AccountManagementFragment() {
        // basicDetailKeys = AccountDetailBasic.getPreferenceEntries();
        // advancedDetailKeys = AccountDetailAdvanced.getPreferenceEntries();
        // srtpDetailKeys = AccountDetailSrtp.getPreferenceEntries();
        // tlsDetailKeys = AccountDetailTls.getPreferenceEntries();

        accountPreferenceHashMap = new HashMap<String, Preference>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Create Account Management Fragment");

        this.setHasOptionsMenu(true);

        /*
         * FIXME if service cannot be obtained from SFLPhonePreferenceActivity, then get it from Application
         */
        service = sflphonePreferenceActivity.getSipService();
        if (service == null) {
            service = ((SFLphoneApplication) sflphonePreferenceActivity.getApplication()).getSipService();
            if (service == null) {
                Log.e(TAG, "onCreate() service=" + service);
            }
        }

        setPreferenceScreen(getAccountListPreferenceScreen());

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(ConfigurationManagerCallback.ACCOUNTS_CHANGED));
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @SuppressWarnings("unchecked") // No proper solution with HashMap runtime cast
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ACCOUNT_CREATE_REQUEST:
            if (resultCode == AccountWizard.ACCOUNT_CREATED) {
                Bundle bundle = data.getExtras();
                Log.i(TAG, "Create account settings");
                HashMap<String, String> accountDetails = new HashMap<String, String>();
                accountDetails = (HashMap<String, String>) bundle.getSerializable(AccountDetail.TAG);
                // if(accountDetails == null){
                // Toast.makeText(getActivity(), "NUUUUL", Toast.LENGTH_SHORT).show();
                // } else
                // Toast.makeText(getActivity(), "OKKKK", Toast.LENGTH_SHORT).show();
                createNewAccount(accountDetails);
            }
            break;
        case ACCOUNT_EDIT_REQUEST:
            if (resultCode == AccountEditionActivity.result.ACCOUNT_MODIFIED) {
                Bundle bundle = data.getExtras();
                String accountID = bundle.getString("AccountID");
                Log.i(TAG, "Update account settings for " + accountID);

                HashMap<String, String> accountDetails = new HashMap<String, String>();
                accountDetails = (HashMap<String, String>) bundle.getSerializable(AccountDetail.TAG);

                Preference accountScreen = accountPreferenceHashMap.get(accountID);
                mRoot.removePreference(accountScreen);
                accountPreferenceHashMap.remove(accountID);
                setAccountDetails(accountID, accountDetails);

            } else if (resultCode == AccountEditionActivity.result.ACCOUNT_DELETED) {
                Bundle bundle = data.getExtras();
                String accountID = bundle.getString("AccountID");

                Log.i(TAG, "Remove account " + accountID);
                deleteSelectedAccount(accountID);
                Preference accountScreen = accountPreferenceHashMap.get(accountID);
                mRoot.removePreference(accountScreen);
                accountPreferenceHashMap.remove(accountID);
            } else {
                Log.i(TAG, "Edition canceled");
            }
            break;
        default:
            break;
        }
    }

    private void createNewAccount(HashMap<String, String> accountDetails) {
        try {
            Log.i(TAG, "ADD ACCOUNT");
            service.addAccount(accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    private void setAccountDetails(String accountID, HashMap<String, String> accountDetails) {
        try {
            service.setAccountDetails(accountID, accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    private void deleteSelectedAccount(String accountID) {
        Log.i(TAG, "DeleteSelectedAccount");
        try {
            service.removeAccount(accountID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> newList = (ArrayList<String>) getAccountList();
            Set<String> currentList = (Set<String>) accountPreferenceHashMap.keySet();
            if (newList.size() > currentList.size()) {
                for (String s : newList) {
                    if (!currentList.contains(s)) {
                        Preference accountScreen = createAccountPreferenceScreen(s);
                        mRoot.addPreference(accountScreen);
                        accountPreferenceHashMap.put(s, accountScreen);
                    }
                }
            }
        }
    };

    Preference.OnPreferenceClickListener launchAccountCreationOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            launchAccountCreationActivity(preference);
            return true;
        }
    };

    Preference.OnPreferenceClickListener launchAccountEditOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            launchAccountEditActivity(preference);
            return true;
        }
    };

    Preference.OnPreferenceClickListener removeSelectedAccountOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getTitle() == "Delete Account") {
                deleteSelectedAccount(preference.getKey());
            }
            return true;
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.account_creation, m);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case R.id.menuitem_create:
            Intent intent = new Intent().setClass(getActivity(), AccountWizard.class);
            startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
            break;
        }

        return true;
    }

    private void launchAccountCreationActivity(Preference preference) {
        Log.i(TAG, "Launch account creation activity");
        Intent intent = preference.getIntent();
        startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
    }

    private void launchAccountEditActivity(Preference preference) {
        Log.i(TAG, "Launch account edit activity");
        Intent intent = preference.getIntent();
        Bundle bundle = intent.getExtras();
        String accountID = bundle.getString("AccountID");

        HashMap<String, String> preferenceMap = getAccountDetails(accountID);

        Account d = new Account(accountID, preferenceMap);

        bundle.putStringArrayList(AccountDetailBasic.BUNDLE_TAG, d.getBasicDetails().getValuesOnly());
        bundle.putStringArrayList(AccountDetailAdvanced.BUNDLE_TAG, d.getAdvancedDetails().getValuesOnly());
        bundle.putStringArrayList(AccountDetailSrtp.BUNDLE_TAG, d.getSrtpDetails().getValuesOnly());
        bundle.putStringArrayList(AccountDetailTls.BUNDLE_TAG, d.getTlsDetails().getValuesOnly());

        intent.putExtras(bundle);

        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    @SuppressWarnings("unchecked") // No proper solution with HashMap runtime cast
    private ArrayList<String> getAccountList() {
        ArrayList<String> accountList = null;
        try {
            accountList = (ArrayList<String>) service.getAccountList();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        // Remove the default account from list
        accountList.remove(DEFAULT_ACCOUNT_ID);

        return accountList;
    }

    @SuppressWarnings("unchecked") // No proper solution with HashMap runtime cast
    private HashMap<String, String> getAccountDetails(String accountID) {
        HashMap<String, String> accountDetails = null;
        try {
            accountDetails = (HashMap<String, String>) service.getAccountDetails(accountID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return accountDetails;
    }

    public PreferenceScreen getAccountListPreferenceScreen() {
        Activity currentContext = getActivity();

        mRoot = getPreferenceManager().createPreferenceScreen(currentContext);

        // Default account category
        // PreferenceCategory defaultAccountCat = new PreferenceCategory(currentContext);
        // defaultAccountCat.setTitle(R.string.default_account_category);
        // mRoot.addPreference(defaultAccountCat);
        //
        // mRoot.addPreference(createAccountPreferenceScreen(DEFAULT_ACCOUNT_ID));

        // Account list category
        // PreferenceCategory accountListCat = new PreferenceCategory(currentContext);
        // accountListCat.setTitle(R.string.default_account_category);
        // mRoot.addPreference(accountListCat);

        ArrayList<String> accountList = getAccountList();

        for (String s : accountList) {
            Preference accountScreen = createAccountPreferenceScreen(s);
            mRoot.addPreference(accountScreen);
            accountPreferenceHashMap.put(s, accountScreen);
        }

        return mRoot;
    }

    Preference createAccountPreferenceScreen(String accountID) {

        HashMap<String, String> details = getAccountDetails(accountID);
        // Set<String> keys = details.keySet();
        // Iterator<String> ite = keys.iterator();
        // while(ite.hasNext()){
        // Log.i(TAG,"key : "+ ite.next());
        // }
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);

        Intent intent = new Intent().setClass(getActivity(), AccountEditionActivity.class);
        intent.putExtras(bundle);

        Preference editAccount = new Preference(getActivity());
        editAccount.setTitle(details.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS));
        editAccount.setSummary(details.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME));
        editAccount.setOnPreferenceClickListener(launchAccountEditOnClick);
        editAccount.setIntent(intent);

        return editAccount;
    }
}
