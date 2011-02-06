// ========================================================================
// Copyright 2007-2008 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//========================================================================

package org.mortbay.ijetty.chat;



import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Message;
import org.mortbay.cometd.BayeuxService;
import org.mortbay.cometd.ext.TimesyncExtension;
import org.mortbay.log.Log;

public class BayeuxServicesListener implements ServletContextAttributeListener
{
    public void initialize(Bayeux bayeux)
    {
        synchronized(bayeux)
        {
                new ChatService(bayeux);
		bayeux.addExtension(new TimesyncExtension());
        }
    }
    
    public void attributeAdded(ServletContextAttributeEvent scab)
    {
        Log.info("Would add Bayeux instance");
        //if (scab.getName().equals(Bayeux.DOJOX_COMETD_BAYEUX))
        //{
            Object o = scab.getValue();
            //if (o instanceof Bayeux)
            //{
            //  initialize((Bayeux)o);
            //}
        //}
    }

    public void attributeRemoved(ServletContextAttributeEvent scab)
    {

    }

    public void attributeReplaced(ServletContextAttributeEvent scab)
    {

    }
}
