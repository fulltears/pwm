/*
 * Password Management Servlets (PWM)
 * http://code.google.com/p/pwm/
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2014 The PWM Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package password.pwm.ldap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.novell.ldapchai.provider.ChaiProvider;
import password.pwm.PwmApplication;
import password.pwm.PwmService;
import password.pwm.config.LdapProfile;
import password.pwm.config.option.DataStorageMethod;
import password.pwm.error.ErrorInformation;
import password.pwm.error.PwmError;
import password.pwm.error.PwmException;
import password.pwm.error.PwmUnrecoverableException;
import password.pwm.health.HealthRecord;
import password.pwm.util.Helper;
import password.pwm.util.PwmLogger;

import java.util.*;

public class LdapConnectionService implements PwmService {
    final private static PwmLogger LOGGER = PwmLogger.getLogger(LdapConnectionService.class);

    private final Map<String,ChaiProvider> proxyChaiProviders = new HashMap<String, ChaiProvider>();
    private final Map<LdapProfile,ErrorInformation> lastLdapErrors = new HashMap<LdapProfile, ErrorInformation>();
    private PwmApplication pwmApplication;
    private STATUS status = STATUS.NEW;

    public STATUS status()
    {
        return status;
    }

    public void init(PwmApplication pwmApplication)
            throws PwmException
    {
        this.pwmApplication = pwmApplication;

        // read the lastLoginTime
        this.lastLdapErrors.putAll(readLastLdapFailure());

        status = STATUS.OPEN;
    }

    public void close()
    {
        status = STATUS.CLOSED;
        LOGGER.trace("closing ldap proxy connections");
        for (final String id : proxyChaiProviders.keySet()) {
            final ChaiProvider existingProvider = proxyChaiProviders.get(id);

            try {
                existingProvider.close();
            } catch (Exception e) {
                LOGGER.error("error closing ldap proxy connection: " + e.getMessage(), e);
            }
        }
        proxyChaiProviders.clear();
    }

    public List<HealthRecord> healthCheck()
    {
        return null;
    }

    public ServiceInfo serviceInfo()
    {
        return new ServiceInfo(Collections.singletonList(DataStorageMethod.LDAP));
    }


    public ChaiProvider getProxyChaiProvider(final LdapProfile ldapProfile)
            throws PwmUnrecoverableException
    {
        return getProxyChaiProvider(ldapProfile.getIdentifier());
    }

    public ChaiProvider getProxyChaiProvider(final String identifier)
            throws PwmUnrecoverableException
    {
        final ChaiProvider proxyChaiProvider = proxyChaiProviders.get(identifier == null ? "" : identifier);
        if (proxyChaiProvider != null) {
            return proxyChaiProvider;
        }

        final LdapProfile ldapProfile = pwmApplication.getConfig().getLdapProfiles().get(identifier == null ? "" : identifier);
        if (ldapProfile == null) {
            final String errorMsg = "unknown ldap profile requested connection: " + identifier;
            throw new PwmUnrecoverableException(new ErrorInformation(PwmError.ERROR_NO_LDAP_CONNECTION,errorMsg));
        }

        try {
            final ChaiProvider newProvider = LdapOperationsHelper.openProxyChaiProvider(ldapProfile, pwmApplication.getConfig(), pwmApplication.getStatisticsManager());
            proxyChaiProviders.put(identifier, newProvider);
            return newProvider;
        } catch (PwmUnrecoverableException e) {
            setLastLdapFailure(ldapProfile,e.getErrorInformation());
            throw e;
        }
    }

    public void setLastLdapFailure(final LdapProfile ldapProfile, final ErrorInformation errorInformation) {
        lastLdapErrors.put(ldapProfile, errorInformation);
        final Gson gson = Helper.getGson();
        final String jsonString = gson.toJson(lastLdapErrors);
        pwmApplication.writeAppAttribute(PwmApplication.AppAttribute.LAST_LDAP_ERROR, jsonString);
    }

    public Map<LdapProfile,ErrorInformation> getLastLdapFailure() {
        return Collections.unmodifiableMap(lastLdapErrors);
    }

    public Date getLastLdapFailureTime(final LdapProfile ldapProfile) {
        final ErrorInformation errorInformation = lastLdapErrors.get(ldapProfile);
        if (errorInformation != null) {
            return errorInformation.getDate();
        }
        return null;
    }

    private Map<LdapProfile,ErrorInformation> readLastLdapFailure() {
        try {
            final String lastLdapFailureStr = pwmApplication.readAppAttribute(PwmApplication.AppAttribute.LAST_LDAP_ERROR);
            if (lastLdapFailureStr != null && lastLdapFailureStr.length() > 0) {

                return Helper.getGson().fromJson(lastLdapFailureStr,new TypeToken<Map<LdapProfile, ErrorInformation>>() {}.getType());
            }
        } catch (Exception e) {
            LOGGER.error("unexpected error loading cached lastLdapFailure statuses: " + e.getMessage());
        }
        return Collections.emptyMap();
    }
}