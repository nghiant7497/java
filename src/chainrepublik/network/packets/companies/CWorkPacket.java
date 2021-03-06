package chainrepublik.network.packets.companies;

import chainrepublik.kernel.CPackets;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CBroadcastPacket;
import chainrepublik.network.packets.blocks.CBlockPayload;

public class CWorkPacket extends CBroadcastPacket
{
    // Serial
    //private static final long serialVersionUID = 100L;
    
    public CWorkPacket(String fee_adr,
                       String adr,
                       long workplaceID,
                       long minutes) throws Exception
    {
        // Constructor
        super(fee_adr, "ID_WORK_PACKET");
        
        // Builds the payload class
	CWorkPayload dec_payload=new CWorkPayload(adr,
                                                  workplaceID,
                                                  minutes);
                              
	// Build the payload
	this.payload=UTILS.SERIAL.serialize(dec_payload);
					
        // Network fee
	this.setFee(0.0001, "Work network fee");
			   
	// Sign packet
	this.sign();
    }
    
    // Check 
    public void check(CBlockPayload block) throws Exception
    {
	  // Super class
   	  super.check(block);
   	  
   	  // Check type
   	  if (!this.tip.equals("ID_WORK_PACKET")) 
             throw new Exception("Invalid packet type - CWorkPacket.java");
   	  
          // Deserialize transaction data
   	  CWorkPayload dec_payload=(CWorkPayload) UTILS.SERIAL.deserialize(payload);
          
          // Check fee
	  if (this.fee<0.0001)
	      throw new Exception("Invalid fee - CWorkPacket.java");
          
          // Check payload
          dec_payload.check(block);
          
          // Footprint
          CPackets foot=new CPackets(this);
          foot.add("Address", dec_payload.target_adr);
          foot.add("Workplace ID", dec_payload.workplaceID);
          foot.add("Minutes", dec_payload.minutes);
          foot.write();
    }
}   
