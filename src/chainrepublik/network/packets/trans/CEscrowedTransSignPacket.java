// Author : Vlad Cristian
// Contact : vcris@gmx.com

package chainrepublik.network.packets.trans;

import java.sql.ResultSet;
import java.sql.Statement;
import chainrepublik.kernel.CPackets;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CBroadcastPacket;
import chainrepublik.network.packets.blocks.CBlockPayload;
import chainrepublik.network.packets.mes.CMesPayload;

public class CEscrowedTransSignPacket  extends CBroadcastPacket 
{
    // Serial
    private static final long serialVersionUID = 100L;
   
    public CEscrowedTransSignPacket(String fee_adr,
                                    String adr, 
                                    String trans_hash, 
                                    String type) throws Exception
    {
	  super(fee_adr, "ID_ESCROWED_TRANS_SIGN");
	  
          // Builds the payload class
	  CEscrowedTransSignPayload dec_payload=new CEscrowedTransSignPayload(adr, trans_hash, type);
			
	   // Build the payload
	   this.payload=UTILS.SERIAL.serialize(dec_payload);
			
	   // Network fee
           this.setFee(0.0001, "Escrowed transaction network fee");
	   
	   // Sign packet
	   this.sign();
  }
  
  // Check 
  public void check(CBlockPayload block) throws Exception
  {
     // Super class
     super.check(block);
     	
     // Check type
     if (!this.tip.equals("ID_ESCROWED_TRANS_SIGN")) 
         throw new Exception("Invalid packet type - CEscrowedTransSignPacket.java");
  	  
     // Check
     CEscrowedTransSignPayload dec_payload=(CEscrowedTransSignPayload) UTILS.SERIAL.deserialize(payload);
     dec_payload.check(block);
     
     // Check fee
     if (this.fee<0.0001)
	throw new Exception("Invalid fee - CTweetMesPacket.java"); 
     
     // Footprint
     CPackets foot=new CPackets(this);
     foot.add("Transaction hash", dec_payload.trans_hash);
     foot.add("Signer", dec_payload.target_adr);
     foot.add("type", dec_payload.type);
     foot.write();
  }
  
 
}
