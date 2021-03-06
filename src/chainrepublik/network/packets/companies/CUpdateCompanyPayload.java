
package chainrepublik.network.packets.companies;

import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CPayload;
import chainrepublik.network.packets.blocks.CBlockPayload;
import java.sql.ResultSet;

public class CUpdateCompanyPayload extends CPayload
{
    // Company ID
    long comID;
    
    // Name
    String name;
    
    // Description
    String desc; 
    
    // Pic
    String pic;
    
    // Serial
    private static final long serialVersionUID = 100L;
    
    public CUpdateCompanyPayload(String adr,
                                 long comID, 
                                 String name, 
                                 String desc, 
                                 String pic) throws Exception
    {
        // Superclass
	super(adr);
        
        // Company ID
        this.comID=comID;
        
        // Name
        this.name=name;
    
        // Description
        this.desc=desc; 
    
        // Pic
        this.pic=pic;
        
         // Hash
 	hash=UTILS.BASIC.hash(this.getHash()+
                              this.comID+
 			      this.name+
                              this.desc+
 			      this.pic);
       
    }
    
    
    public void check(CBlockPayload block) throws Exception
   {
   	// Super class
   	super.check(block);
        
        // Check for null
        if (this.name==null ||
            this.desc==null ||
            this.pic==null)
        throw new Exception("Null assertion failed - CRentLicencePayload.java, 68");
        
        // Energy
        this.checkEnergy();
        
         // Company address
        String com_adr=UTILS.BASIC.getComAdr(this.comID);
        
        // Citizen address ?
        if (!UTILS.BASIC.isCitAdr(this.target_adr, this.block))
           throw new Exception("Only citizens can do this action - CWorkPayload.java, 68");
        
        // Company owner ?
        if (!UTILS.BASIC.isComOwner(this.target_adr, comID))
             throw new Exception("Address is not company owner, CUpdateCompanyPayload.java, 74");
        
        // Name
        if (!UTILS.BASIC.isTitle(this.name))
            throw new Exception("Invalid name, CUpdateCompanyPayload.java, 65");
        
        // Description
        if (!UTILS.BASIC.isDesc(this.desc))
            throw new Exception("Invalid description, CUpdateCompanyPayload.java, 69");
        
        // Pic
        if (!this.pic.equals(""))
            if (!UTILS.BASIC.isPic(this.pic))
                throw new Exception("Invalid pic, CUpdateCompanyPayload.java, 74");
        
        // Hash
 	String h=UTILS.BASIC.hash(this.getHash()+
                                  this.comID+
 			          this.name+
                                  this.desc+
 			          this.pic);
        
        // Hash match ?
        if (!h.equals(this.hash))
            throw new Exception("Invalid hash, CUpdateCompanyPayload.java, 113");
   }
    
    public void commit(CBlockPayload block) throws Exception
    {
       // Superclass
       super.commit(block);
       
       // Update
       UTILS.DB.executeUpdate("UPDATE companies "
                               + "SET name='"+UTILS.BASIC.base64_encode(this.name)+"', "
                                   + "description='"+UTILS.BASIC.base64_encode(this.desc)+"' "
                             + "WHERE comID='"+this.comID+"'");
       
       // Update address
       UTILS.DB.executeUpdate("UPDATE adr "
                               + "SET pic='"+UTILS.BASIC.base64_encode(this.pic)+"' "
                             + "WHERE adr='"+UTILS.BASIC.getComAdr(this.comID)+"'");
       
       // Clear transations
       UTILS.ACC.clearTrans(this.hash, "ID_ALL", this.block);
    }
    
}
