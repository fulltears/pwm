/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2018 The PWM Project
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

package password.pwm.util.cli.commands;

import password.pwm.PwmApplication;
import password.pwm.util.cli.CliParameters;

public class ResetInstanceIDCommand extends AbstractCliCommand
{
    public void doCommand( ) throws Exception
    {
        final PwmApplication pwmApplication = cliEnvironment.getPwmApplication();
        final String currentInstanceID = pwmApplication.getInstanceID();
        
        if ( !promptForContinue( "Proceeding will change the existing instanceID (\"" + currentInstanceID + "\") of this server and can not be undone." ) )
        {
            return;
        }

        pwmApplication.writeAppAttribute( PwmApplication.AppAttribute.INSTANCE_ID, null );
        out( "instanceID has been cleared" );
    }

    @Override
    public CliParameters getCliParameters( )
    {
        final CliParameters cliParameters = new CliParameters();
        cliParameters.commandName = "ResetInstanceID";
        cliParameters.description = "Reset the existing instanceID ";
        cliParameters.needsPwmApplication = true;
        cliParameters.needsLocalDB = true;
        cliParameters.readOnly = true;

        return cliParameters;
    }
}
