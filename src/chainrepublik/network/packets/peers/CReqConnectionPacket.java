// Author : Vlad Cristian
// Contact : vcris@gmx.com

package chainrepublik.network.packets.peers;

import chainrepublik.kernel.UTILS;
import chainrepublik.network.CPeer;
import chainrepublik.network.packets.CPacket;

public class CReqConnectionPacket extends CPacket 
{
	// Software version
	String ver;
	
	// Status
	String status;
        
        // Port
        int server_port;
        
        // Serial
   private static final long serialVersionUID = 100L;
	
   public CReqConnectionPacket() throws Exception
   {
	   super("ID_REQ_CON_PACKET");
	   
	   // Version
	   this.ver=UTILS.STATUS.version;
           
           // Port
           this.server_port=UTILS.SETTINGS.port;
           
           // Hash
           this.hash=UTILS.BASIC.hash(UTILS.BASIC.mtstamp()+this.ver+this.server_port);
   }
   
   public void check(CPeer peer) throws Exception
   {
	   boolean aproved=true;
	   
	   // Check if connection is possible
	   if (UTILS.NETWORK.peers.conectedTo(peer.adr)==true)
               throw new Exception("Already connected");
           else
               UTILS.NETWORK.peers.addPeer(peer, this.server_port);
           
           
	   // Create response
	   CReqConnectionResponsePacket response=new CReqConnectionResponsePacket(aproved, peer);
	   peer.writePacket(response);
   }
}