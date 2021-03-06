/*
 * Copyright 2018 Vlad Cristian (vcris@gmx.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chainrepublik.network.packets.press;
import chainrepublik.kernel.CPackets;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CBroadcastPacket;
import chainrepublik.network.packets.blocks.CBlockPayload;


public class CNewArticlePacket extends CBroadcastPacket 
{
   // Serial
   private static final long serialVersionUID = 100L;
   
   public CNewArticlePacket(String fee_adr, 
		            String adr, 
		            String title,
                            String mes, 
                            String categ,
                            String cou, 
                            String pic,
                            long mil_unit,
                            long pol_party,
                            long days) throws Exception
   {
	   // Super class
	   super(fee_adr, "ID_NEW_TWEET_PACKET");
	   
	   // Builds the payload class
	   CNewArticlePayload dec_payload=new CNewArticlePayload(adr, 
                                                                 title,
		                                                 mes, 
                                                                 categ,
                                                                 cou,
                                                                 pic,
                                                                 mil_unit,
                                                                 pol_party,
                                                                 days);
			
	   // Build the payload
	   this.payload=UTILS.SERIAL.serialize(dec_payload);
			
	   // Network fee
           this.setFee( 0.0001*days, "New article network fee");
           
	   // Sign packet
           this.sign();
   }
   
   // Check 
   public void check(CBlockPayload block) throws Exception
   {
          // Super class
   	  super.check(block);
   	  
   	  // Check type
   	  if (!this.tip.equals("ID_NEW_TWEET_PACKET")) 
   		throw new Exception("Invalid packet type - CNewTweetPacket.java");
   	  
   	  // Deserialize transaction data
   	  CNewArticlePayload dec_payload=(CNewArticlePayload) UTILS.SERIAL.deserialize(payload);
          
          // Check payload
          dec_payload.check(block);
          
          // Check fee
	  if (this.fee<dec_payload.days*0.0001)
	     throw new Exception("Invalid fee - CNewTweetPacket.java");
 
          // Footprint
          CPackets foot=new CPackets(this);
          foot.add("Address", dec_payload.target_adr);
          foot.add("Title", String.valueOf(dec_payload.title));
          foot.add("Message", String.valueOf(dec_payload.mes));
          foot.add("Pic", String.valueOf(dec_payload.pic));
          foot.add("Military Unit", String.valueOf(dec_payload.pol_party));
          foot.add("Political Party", String.valueOf(dec_payload.mil_unit));
          foot.write();
   	
   }
   
  
}
