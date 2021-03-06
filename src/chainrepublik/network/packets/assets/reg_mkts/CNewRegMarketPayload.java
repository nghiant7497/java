// Author : Vlad Cristian
// Contact : vcris@gmx.com

package chainrepublik.network.packets.assets.reg_mkts;
import chainrepublik.kernel.UTILS;
import chainrepublik.network.packets.CPayload;
import chainrepublik.network.packets.blocks.CBlockPayload;
import java.sql.ResultSet;

public class CNewRegMarketPayload extends CPayload
{   
    // Market address
    String adr;
    
    // Asset symbol
    String asset_symbol;
    
    // Currency symbol
    String cur_symbol; 
    
    // Title
    String title; 
    
    // Description
    String description;
    
    // Decimals
    long decimals;
    
    // Days
    long days;
    
    // Market ID
    long mktID;
                                        
    // Serial
    private static final long serialVersionUID = 100L;
    
    public CNewRegMarketPayload(String adr, 
                                String asset_symbol,
                                String cur_symbol,
                                String name,
                                String desc, 
                                long decimals,
                                long days)  throws Exception
    {
        super(adr);
        
        // Market ddress
        this.adr=adr;
        
        // Asset symbol
        this.asset_symbol=asset_symbol;
    
        // Currency symbol
        this.cur_symbol=cur_symbol;
    
        // Title
        this.title=name;
    
        // Description
        this.description=desc;
    
        // Decimals
        this.decimals=decimals;
        
        // Days
        this.days=days;
        
        // Market ID
        this.mktID=UTILS.BASIC.getID();
      
        // Hash
        hash=UTILS.BASIC.hash(this.getHash()+
                              this.asset_symbol+
                              this.cur_symbol+
                              this.title+
                              this.description+
                              this.decimals+
                              this.days+
                              this.mktID);
       
    }

    
    public void check(CBlockPayload block) throws Exception
    {
        // Parent
        super.check(block);
        
        // Check for null
        if (this.adr==null ||
            this.asset_symbol==null ||
            this.cur_symbol==null ||
            this.title==null ||
            this.description==null)
        throw new Exception("Null assertion failed - CWorkPayload.java, 68");
        
         // Check energy
       this.checkEnergy();
       
       // Citizen address ?
        if (!UTILS.BASIC.isRegistered(this.target_adr, this.block))
           throw new Exception("Only citizens can do this action - CWorkPayload.java, 68");
         
        // Asset symbol 
        if (!UTILS.BASIC.isAsset(asset_symbol))
           throw new Exception("Invalid asset symbol - CNewRegMarketPayload.java");
        
        // Currency symbol
        if (!this.cur_symbol.equals("CRC"))
           if (!UTILS.BASIC.isAsset(cur_symbol))
             throw new Exception("Invalid currency symbol - CNewRegMarketPayload.java");
        
        // Asset and currency symbol the same ?
        if (this.asset_symbol.equals(this.cur_symbol))
            throw new Exception("Asset and currency are the same - CNewRegMarketPayload.java");
         
        // Market ID
        if (UTILS.BASIC.isID(mktID))
            throw new Exception("Invalid market ID - CNewRegMarketPayload.java");
        
        // Title
        if (!UTILS.BASIC.isTitle(title))
           throw new Exception("Invalid title - CNewRegMarketPayload.java");
             
        // Description
        if (!UTILS.BASIC.isDesc(description))
           throw new Exception("Invalid description - CNewRegMarketPayload.java");
         
        // Market Days
        if (this.days<30)
           throw new Exception("Invalid days - CNewRegMarketPayload.java");
         
        // Decimals
        if (this.decimals<0 || this.decimals>8)
            throw new Exception("Invalid decimals - CNewRegMarketPayload.java");
        
        // Another market exist ?
        ResultSet rs=UTILS.DB.executeQuery("SELECT * "
                                           + "FROM assets_mkts "
                                          + "WHERE asset='"+this.asset_symbol+"' "
                                            + "AND cur='"+this.cur_symbol+"'");
        
        // Has data ?
        if (UTILS.DB.hasData(rs))
           throw new Exception("Market already exist - CNewRegMarketPayload.java");
        
        // Hash code
        String h=UTILS.BASIC.hash(this.getHash()+
                                  this.asset_symbol+
                                  this.cur_symbol+
                                  this.title+
                                  this.description+
                                  this.decimals+
                                  this.days+
                                  this.mktID);
             
        // Check hash
        if (!this.hash.equals(h))
             throw new Exception("Invalid hash - CNewRegMarketPayload.java");
        
        // Funds
        if (UTILS.ACC.getBalance(this.target_adr, "CRC", block)<this.days*0.0001)
           throw new Exception("Insuficient funds - CNewRegMarketPayload.java");
        
        // Transfer fee
        UTILS.ACC.newTransfer(this.target_adr, 
                              "default", 
                              days*0.0001, 
                              "CRC", 
                              "New market fee", 
                              "", 
                              0, 
                              this.hash, 
                              this.block, 
                              false, 
                              "", 
                              "");
    }
    
    public void commit(CBlockPayload block) throws Exception
    {
        // Super
        super.commit(block);
        
       // Insert market
        UTILS.DB.executeUpdate("INSERT INTO assets_mkts "
                                     + "SET adr='"+this.adr+"', "
                                         + "asset='"+this.asset_symbol+"', "
                                         + "cur='"+this.cur_symbol+"', "
                                         + "name='"+UTILS.BASIC.base64_encode(this.title)+"', "
                                         + "description='"+UTILS.BASIC.base64_encode(this.description)+"', "
                                         + "decimals='"+this.decimals+"', "
                                         + "expires='"+(this.block+(this.days*1440))+"', "
                                         + "mktID='"+this.mktID+"'");
        
        // Position type
        UTILS.ACC.clearTrans(hash, "ID_ALL", this.block);
    }        
}
