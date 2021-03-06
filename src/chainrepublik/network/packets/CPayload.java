// Author : Vlad Cristian
// Contact : vcris@gmx.com

package chainrepublik.network.packets;

import chainrepublik.kernel.CAddress;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.blocks.CBlockPayload;


public class CPayload  implements java.io.Serializable
{
	// Hash
	public String hash;
	
	// Block target address
        public String target_adr;
	
	// Signature
	public String sign;
	
	// Tstamp
	public long tstamp;
	
	// Tstamp
        public long block;
        
	// Constructor
        public CPayload() throws Exception
	{
           // Time
           this.tstamp=UTILS.BASIC.mtstamp();
           
           // Block
	   this.block=UTILS.NET_STAT.last_block+1;
           
           // Hash
           this.hash=this.getHash();
	}
    
        //  Hash
        public String getHash() throws Exception
        {	
            return UTILS.BASIC.hash(this.target_adr+
                                    this.tstamp+
                                    this.block);
        }
    
        // Constructor
        public CPayload(String adr) throws Exception
        {
    	    // Time
            this.tstamp=UTILS.BASIC.mtstamp();
    	
           // Target address
    	   this.target_adr=adr;
    	
	   // Block
	   this.block=UTILS.NET_STAT.last_block+1;
	   
	   // Hash
	   this.hash=this.getHash();
        }
    
        // Check
        public void check(CBlockPayload block) throws Exception
        {
           // Null hash ?
           if (this.hash==null)
               throw new Exception("Null assertion failed - CPayload");
           
           // Hash
           if (!UTILS.BASIC.isHash(this.hash)) 
                throw new Exception("Invalid hash - CPayload");
            
            // Target adr
            if (this.target_adr!=null)
               if (!this.target_adr.equals(""))
                 if (!UTILS.BASIC.isAdr(this.target_adr))
                  throw new Exception("Invalid target address - CPayload");
           
           // Block number
           if (block!=null)
             if (block.block!=this.block)
               throw new Exception("Invalid block number - CPayload");
           
           // Delete previous trans
           UTILS.DB.executeUpdate("DELETE FROM trans WHERE hash='"+this.hash+"'");
        }
        
       
        public void commit(CBlockPayload block) throws Exception
        {
           
        }
        
        public void footprint(String packet_hash) throws Exception
        {
        
        }
        
        public void checkEnergy() throws Exception
        {
           // Registered ?
           if (UTILS.BASIC.isRegistered(this.target_adr, this.block))
           {
               // Load address type
               String owner_type=UTILS.BASIC.getAdrOwnerType(this.target_adr);
              
               // Energy
               if (Double.parseDouble(UTILS.BASIC.getAdrData(this.target_adr, "energy"))<0.1)
                     throw new Exception("Not enought energy to execute this action - CPayload.java, Line 85");
               
               // Take energy
               UTILS.ACC.newTrans(this.target_adr, 
                                  "", 
                                  -0.1, 
                                  "ID_ENERGY", 
                                  "You have lost energy executing a game action", 
                                  "", 
                                  0, 
                                  this.hash, 
                                  this.block,
                                  false,
                                  "",
                                  "");
           }
        }
        
        public void checkEnergy(double required) throws Exception
        {
           // Registered ?
           if (UTILS.BASIC.isRegistered(this.target_adr, this.block))
           {
               // Load address type
               String owner_type=UTILS.BASIC.getAdrOwnerType(this.target_adr);
              
               // Energy
               if (Double.parseDouble(UTILS.BASIC.getAdrData(this.target_adr, "energy"))<0.1)
                     throw new Exception("Not enought energy to execute this action - CPayload.java, Line 85");
               
               // Take energy
               UTILS.ACC.newTrans(this.target_adr, 
                                  "", 
                                  -required, 
                                  "ID_ENERGY", 
                                  "You have lost energy executing a game action", 
                                  "", 
                                  0, 
                                  this.hash, 
                                  this.block,
                                  false,
                                  "",
                                  "");
           }
        }
        
}