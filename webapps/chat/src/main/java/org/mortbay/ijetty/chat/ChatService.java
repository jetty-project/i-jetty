/**
 * 
 */
package org.mortbay.ijetty.chat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.cometd.Bayeux;
import org.cometd.Channel;
import org.cometd.Client;
import org.cometd.RemoveListener;
import org.mortbay.cometd.BayeuxService;
import org.mortbay.log.Log;

public class ChatService extends BayeuxService
{
    ConcurrentMap<String,Set<String>> _members = new ConcurrentHashMap<String,Set<String>>();
    
    public ChatService(Bayeux bayeux)
    {
        super(bayeux,"chat");
        subscribe("/chat/**","trackMembers");
    }
    
    public void trackMembers(Client joiner,String channel,Map<String,Object> data,String id)
    {
        if (Boolean.TRUE.equals(data.get("join")))
        {
            Set<String> m = _members.get(channel);
            if (m==null)
            {
                Set<String> new_list=new CopyOnWriteArraySet<String>();
                m=_members.putIfAbsent(channel,new_list);
                if (m==null)
                    m=new_list;
            }
            
            final Set<String> members=m;
            final String username=(String)data.get("user");
            
            members.add(username);
            final String channel_=channel;
            final String id_=id;
            joiner.addListener(new RemoveListener(){
                public void removed(String clientId, boolean timeout)
                {
                    members.remove(username);
                    Log.info("members: "+members);
                    Channel ch=getBayeux().getChannel(channel_,false);
                    if (ch!=null)
                        ch.publish(getClient(),members,id_);
                }
            });
            Log.info("Members: "+members);
            getBayeux().getChannel(channel,false).publish(getClient(),members,id);
        }
    }
}
