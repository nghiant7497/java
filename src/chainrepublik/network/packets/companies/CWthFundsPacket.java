package chainrepublik.network.packets.companies;

import chainrepublik.kernel.CPackets;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CBroadcastPacket;
import chainrepublik.network.packets.blocks.CBlockPayload;

public class CWthFundsPacket extends CBroadcastPacket
{
    // Serial
    private static final long serialVersionUID = 100L;
    
    public CWthFundsPacket(String fee_adr,
                         String adr,
                         long comID,
                         double amount) throws Exception
    {
        // Constructor
        super(fee_adr, "ID_WTH_FUNDS_PACKET");
        
        // Builds the payload class
	CWthFundsPayload dec_payload=new CWthFundsPayload(adr,
                                                          comID,
                                                          amount);
                              
	// Build the payload
	this.payload=UTILS.SERIAL.serialize(dec_payload);
					
        // Network fee
	this.setFee(0.0001, "Dividends withdraw network fee");
			   
	// Sign packet
	this.sign();
    }
    
    // Check 
    public void check(CBlockPayload block) throws Exception
    {
	  // Super class
   	  super.check(block);
   	  
   	  // Check type
   	  if (!this.tip.equals("ID_WTH_FUNDS_PACKET")) 
             throw new Exception("Invalid packet type - CWthFundsPacket.java");
   	  
          // Deserialize transaction data
   	  CWthFundsPayload dec_payload=(CWthFundsPayload) UTILS.SERIAL.deserialize(payload);
          
          // Check fee
	  if (this.fee<0.0001)
	      throw new Exception("Invalid fee - CWthFundsPacket.java");
          
          // Check payload
          dec_payload.check(block);
          
          // Footprint
          CPackets foot=new CPackets(this);
          foot.add("Address", dec_payload.target_adr);
          foot.add("Company ID", dec_payload.comID);
          foot.add("Amount", dec_payload.amount);
          foot.write();
    }
}   
