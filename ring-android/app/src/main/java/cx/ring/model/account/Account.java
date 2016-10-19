/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.model.account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Account extends java.util.Observable {
    private static final String TAG = Account.class.getName();

    private final String accountID;
    private AccountConfig mVolatileDetails;
    private AccountConfig mDetails;
    private ArrayList<AccountCredentials> credentialsDetails = new ArrayList<>();

    private Map<String, String> devices = new HashMap<>();

    public OnDevicesChangedListener devicesListener = null;
    public OnExportEndedListener exportListener = null;
    public OnStateChangedListener stateListener = null;

    public Account(String bAccountID) {
        accountID = bAccountID;
        mDetails = new AccountConfig();
        mVolatileDetails = new AccountConfig();
    }

    public Account(String bAccountID, final Map<String, String> details,
                   final ArrayList<Map<String, String>> credentials,
                   final Map<String, String> volDetails) {
        accountID = bAccountID;
        mDetails = new AccountConfig(details);
        mVolatileDetails = new AccountConfig(volDetails);
        setCredentials(credentials);
    }

    public void update(Account account) {
        String old = getRegistrationState();
        mDetails = account.mDetails;
        mVolatileDetails = account.mVolatileDetails;
        credentialsDetails = account.credentialsDetails;
        devices = account.devices;
        String newRegState = getRegistrationState();
        if (old != null && !old.contentEquals(newRegState) && stateListener != null) {
            stateListener.stateChanged(newRegState, getRegistrationStateCode());
        }
    }

    public Map<String, String> getDevices() {
        return devices;
    }

    public void setCredentials(ArrayList<Map<String, String>> credentials) {
        credentialsDetails.clear();
        if (credentials != null) {
            credentialsDetails.ensureCapacity(credentials.size());
            for (int i = 0; i < credentials.size(); ++i) {
                credentialsDetails.add(new AccountCredentials(credentials.get(i)));
            }
        }
    }

    public void setDetails(Map<String, String> details) {
        this.mDetails = new AccountConfig(details);
    }

    public void setDetail(ConfigKey key, String val) {
        mDetails.put(key, val);
    }

    public void setDetail(ConfigKey key, boolean val) {
        mDetails.put(key, val);
    }

    public AccountConfig getConfig() {
        return mDetails;
    }

    public interface OnDevicesChangedListener {
        void devicesChanged(Map<String, String> devices);
    }

    public interface OnExportEndedListener {
        void exportEnded(int code, String pin);
    }

    public interface OnStateChangedListener {
        void stateChanged(String state, int code);
    }

    public void setDevices(Map<String, String> devs) {
        devices = devs;
        if (devicesListener != null)
            devicesListener.devicesChanged(devs);
    }

    public String getAccountID() {
        return accountID;
    }

    public String getUsername() {
        return mDetails.get(ConfigKey.ACCOUNT_USERNAME);
    }

    public String getHost() {
        return mDetails.get(ConfigKey.ACCOUNT_HOSTNAME);
    }

    public void setHost(String host) {
        mDetails.put(ConfigKey.ACCOUNT_HOSTNAME, host);
    }

    public String getProxy() {
        return mDetails.get(ConfigKey.ACCOUNT_ROUTESET);
    }

    public void setProxy(String proxy) {
        mDetails.put(ConfigKey.ACCOUNT_ROUTESET, proxy);
    }

    public String getRegistrationState() {
        return mVolatileDetails.get(ConfigKey.ACCOUNT_REGISTRATION_STATUS);
    }

    public int getRegistrationStateCode() {
        String codeStr = mVolatileDetails.get(ConfigKey.ACCOUNT_REGISTRATION_STATE_CODE);
        if (codeStr == null || codeStr.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(codeStr);
    }

    public void setRegistrationState(String registeredState, int code) {
        String old = getRegistrationState();
        mVolatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATUS, registeredState);
        mVolatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATE_CODE, Integer.toString(code));
        if (old != null && !old.contentEquals(registeredState) && stateListener != null) {
            stateListener.stateChanged(registeredState, code);
        }
    }

    public void setVolatileDetails(Map<String, String> volatileDetails) {
        String stateOld = getRegistrationState();
        this.mVolatileDetails = new AccountConfig(volatileDetails);
        String stateNew = getRegistrationState();
        if (!stateOld.contentEquals(stateNew) && stateListener != null) {
            stateListener.stateChanged(stateNew, getRegistrationStateCode());
        }
    }

    public String getRegisteredName() {
        return mVolatileDetails.get(ConfigKey.ACCOUNT_REGISTRED_NAME);
    }

    public String getAlias() {
        return mDetails.get(ConfigKey.ACCOUNT_ALIAS);
    }

    public Boolean isSip() {
        return mDetails.get(ConfigKey.ACCOUNT_TYPE).equals(AccountConfig.ACCOUNT_TYPE_SIP);
    }

    public Boolean isRing() {
        return mDetails.get(ConfigKey.ACCOUNT_TYPE).equals(AccountConfig.ACCOUNT_TYPE_RING);
    }

    public void setAlias(String alias) {
        mDetails.put(ConfigKey.ACCOUNT_ALIAS, alias);
    }

    public String getDetail(ConfigKey key) {
        return mDetails.get(key);
    }

    public boolean getDetailBoolean(ConfigKey key) {
        return mDetails.getBool(key);
    }

    public boolean isEnabled() {
        return mDetails.getBool(ConfigKey.ACCOUNT_ENABLE);
    }

    public void setEnabled(boolean isChecked) {
        mDetails.put(ConfigKey.ACCOUNT_ENABLE, isChecked);
    }

    public HashMap<String, String> getDetails() {
        return mDetails.getAll();
    }

    public boolean isTrying() {
        return getRegistrationState().contentEquals(AccountConfig.STATE_TRYING);
    }

    public boolean isRegistered() {
        return (getRegistrationState().contentEquals(AccountConfig.STATE_READY) || getRegistrationState().contentEquals(AccountConfig.STATE_REGISTERED));
    }

    public boolean isInError() {
        String state = getRegistrationState();
        return (state.contentEquals(AccountConfig.STATE_ERROR)
                || state.contentEquals(AccountConfig.STATE_ERROR_AUTH)
                || state.contentEquals(AccountConfig.STATE_ERROR_CONF_STUN)
                || state.contentEquals(AccountConfig.STATE_ERROR_EXIST_STUN)
                || state.contentEquals(AccountConfig.STATE_ERROR_GENERIC)
                || state.contentEquals(AccountConfig.STATE_ERROR_HOST)
                || state.contentEquals(AccountConfig.STATE_ERROR_NETWORK)
                || state.contentEquals(AccountConfig.STATE_ERROR_NOT_ACCEPTABLE)
                || state.contentEquals(AccountConfig.STATE_ERROR_SERVICE_UNAVAILABLE)
                || state.contentEquals(AccountConfig.STATE_REQUEST_TIMEOUT));
    }

    public boolean isIP2IP() {
        boolean emptyHost = getHost() == null || (getHost() != null && getHost().isEmpty());
        return isSip() && emptyHost;
    }

    public boolean isAutoanswerEnabled() {
        return mDetails.getBool(ConfigKey.ACCOUNT_AUTOANSWER);
    }

    public ArrayList<AccountCredentials> getCredentials() {
        return credentialsDetails;
    }

    public void addCredential(AccountCredentials newValue) {
        credentialsDetails.add(newValue);
    }

    public void removeCredential(AccountCredentials accountCredentials) {
        credentialsDetails.remove(accountCredentials);
    }

    @Override
    public boolean hasChanged() {
        return true;
    }

    public List getCredentialsHashMapList() {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();
        for (AccountCredentials cred : credentialsDetails) {
            result.add(cred.getDetails());
        }
        return result;
    }

    public boolean useSecureLayer() {
        return mDetails.getBool(ConfigKey.SRTP_ENABLE) || mDetails.getBool(ConfigKey.TLS_ENABLE);
    }

    public String getShareURI() {
        String shareUri;
        if (isRing()) {
            shareUri = getUsername();
        } else {
            shareUri = getUsername() + "@" + getHost();
        }

        return shareUri;
    }

    public boolean needsMigration() {
        return AccountConfig.STATE_NEED_MIGRATION.equals(getRegistrationState());
    }
}
