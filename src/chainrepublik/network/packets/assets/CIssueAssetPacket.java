// Author : Vlad Cristian
// Contact : vcris@gmx.com

package chainrepublik.network.packets.assets;

import chainrepublik.kernel.CPackets;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CBroadcastPacket;
import chainrepublik.network.packets.blocks.CBlockPayload;

public class CIssueAssetPacket extends CBroadcastPacket 
{
   // Serial
   private static final long serialVersionUID = 100L;
    
   public CIssueAssetPacket(String fee_adr,
		            String adr,
                            String name,
                            String description,
                            String how_buy,
                            String how_sell,
                            String website,
                            String pic,
                            String symbol,
                            long qty,
                            double trans_fee,
                            String trans_fee_adr,
                            long days) throws Exception
   {
	   super(fee_adr, "ID_NEW_ASSET_PACKET");

	   // Builds the payload class
	   CIssueAssetPayload dec_payload=new CIssueAssetPayload(adr,
                                                                 name,
                                                                 description,
                                                                 how_buy,
                                                                 how_sell,
                                                                 website,
                                                                 pic,
                                                                 symbol,
                                                                 qty,
                                                                 trans_fee,
                                                                 trans_fee_adr,
                                                                 days); 
           
	   // Build the payload
	   this.payload=UTILS.SERIAL.serialize(dec_payload);
           
           // Network fee
	   this.setFee(0.0001, "Issue asset network fee");
	   
           // Sign packet
	   this.sign();
   }
   
   // Check 
   public void check(CBlockPayload block) throws Exception
   {
        // Super class
   	super.check(block);
   	  
   	// Check type
   	if (!this.tip.equals("ID_NEW_ASSET_PACKET")) 
             throw new Exception("Invalid packet type - CIssueAssetPacket.java");
   	  
        // Deserialize transaction data
   	CIssueAssetPayload dec_payload=(CIssueAssetPayload) UTILS.SERIAL.deserialize(payload);
          
        // Check payload
        dec_payload.check(block);
          
        // Check fee
	if (this.fee<0.0001)
	      throw new Exception("Invalid fee - CIssueAssetPacket.java");
          
        // Footprint
        CPackets foot=new CPackets(this);
        foot.add("Address", dec_payload.target_adr);
        foot.add("Symbol", dec_payload.symbol);
        foot.add("Title", dec_payload.title);
        foot.add("Description", dec_payload.description);
        foot.add("How Buy", dec_payload.how_buy);
        foot.add("How Sell", dec_payload.how_sell);
        foot.add("Web Page", dec_payload.web_page);
        foot.add("Pic", dec_payload.pic);
        foot.add("Days", String.valueOf(dec_payload.days));
        foot.add("Qty", String.valueOf(dec_payload.qty));
        foot.add("Transaction Fee Address", dec_payload.trans_fee_adr);
        foot.add("Transaction Fee", String.valueOf(dec_payload.trans_fee));
        foot.write();
   }
}