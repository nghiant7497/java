// Author : Vlad Cristian
// Contact : vcris@gmx.com

package chainrepublik.network.packets.press;

import chainrepublik.kernel.CPackets;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CBroadcastPacket;
import chainrepublik.network.packets.blocks.CBlockPayload;


public class CUnfollowPacket extends CBroadcastPacket 
{
   // Serial
   private static final long serialVersionUID = 100L;
   
   public CUnfollowPacket(String fee_adr,
                          String adr,
                          String unfollow_address) throws Exception
   {
	   // Super class
	   super(fee_adr, "ID_UNFOLLOW_PACKET");
	   
	   // Builds the payload class
	   CUnfollowPayload dec_payload=new CUnfollowPayload(adr, 
                                                             unfollow_address);
			
	   // Build the payload
	   this.payload=UTILS.SERIAL.serialize(dec_payload);
			
	   // Network fee
           this.setFee(0.0001, "Unfollow network fee");
	   
	   // Sign packet
           this.sign();
   }
   
   // Check 
   public void check(CBlockPayload block) throws Exception
   {
          // Super class
   	  super.check(block);
   	  
   	  // Check type
   	  if (!this.tip.equals("ID_UNFOLLOW_PACKET")) 
   		throw new Exception("Invalid packet type - CFollowPayload.java"); 
          
          // Check fee
	  if (this.fee<0.0001)
	      throw new Exception("Invalid fee - CFollowPayload.java"); 
          
          // Deserialize transaction data
   	  CUnfollowPayload dec_payload=(CUnfollowPayload) UTILS.SERIAL.deserialize(payload);
          
          // Check payload
          dec_payload.check(block);
           
          // Footprint
          CPackets foot=new CPackets(this);
          foot.add("Address", dec_payload.target_adr);
          foot.add("Unfollow Address", dec_payload.unfollow_adr);
          foot.write();
   	 
   }
   
  
}
