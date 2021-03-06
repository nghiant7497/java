package chainrepublik.kernel;
import java.sql.ResultSet;
import chainrepublik.network.packets.blocks.CBlockPayload;
import java.util.Random;

public class CAcc 
{
    public CAcc() throws Exception
    {
        
    }
    
    public void clearTrans(String hash, String tip, long block) throws Exception
    {
        // Check hash
        if (!UTILS.BASIC.isHash(hash))
            throw new Exception("Invalid hash - CAcc.java, 19");
        
        // Load trans data
        ResultSet rs=UTILS.DB.executeQuery("SELECT * "
                                           + "FROM trans "
                                           + "WHERE hash='"+hash+"' "
                                             + "AND status='ID_PENDING'");
                
        // Clear
        while (rs.next())
        {
            if ((rs.getDouble("amount")<0 && (tip.equals("ID_ALL") || tip.equals("ID_SEND"))) ||
                (rs.getDouble("amount")>0 && (tip.equals("ID_ALL") || tip.equals("ID_RECEIVE"))))
            {
                // Asset
                if (!rs.getString("cur").equals("CRC") && 
                    rs.getString("cur").indexOf("_")==-1)
                   this.doAssetTrans(rs.getString("src"), 
                                     rs.getDouble("amount"), 
                                     rs.getString("cur"),
                                     block);
               
                // Product
                if (!rs.getString("cur").equals("CRC") && 
                    rs.getString("cur").indexOf("_")>=0)
                   this.doProdTrans(rs.getString("src"), 
                                     rs.getDouble("amount"), 
                                     rs.getString("cur"),
                                     rs.getDouble("invested"),
                                     block,
                                     hash);
                
                // CRC
                if (rs.getString("cur").equals("CRC"))
                        this.doTrans(rs.getString("src"), 
                                     rs.getDouble("amount"), 
                                     block);
                      
                // Update trans
                UTILS.DB.executeUpdate("UPDATE trans "
                                        + "SET status='ID_CLEARED' "
                                      + "WHERE ID='"+rs.getLong("ID")+"'");
            }
        }
    }
        
        
    public double newProdTrans(String adr, 
                                   double amount, 
                                   String prod, 
                                   String expl, 
                                   String hash,
                                   double cost,
                                   long block) throws Exception
    {
        // ResultSet
        ResultSet rs;
            
            // Address valid
            if (!UTILS.BASIC.isAdr(adr) && 
                !adr.equals("none")) 
                throw new Exception("Invalid address - CAcc.java, 107");
            
            // Valid currency ?
            if (!UTILS.BASIC.isProd(prod) 
                && !UTILS.BASIC.isLic(prod)) 
                throw new Exception("Invalid product - CAcc.java, 85");
            
            // Valid hash ?
            if (!UTILS.BASIC.isHash(hash)) 
                throw new Exception("Invalid hash - CAcc.java, 124");
            
            // Amount
            if (amount==0)
                throw new Exception("Invalid amount - CAcc.java, 124");
            
            // Owner type
            String owner_type=UTILS.BASIC.getAdrOwnerType(adr);
            
            // Owner citizen ?
            if (owner_type.equals("ID_CIT"))
            {
                if (amount<0)
                {
                    if (!UTILS.BASIC.canSell(adr, prod) && 
                        !UTILS.BASIC.canDonate(adr, prod))
                       throw new Exception("Address can't sell this product - CAcc.java, 124");
                }
                else 
                {
                    if (!UTILS.BASIC.canBuy(adr, prod, amount, null))
                       throw new Exception("Address can't buy this product - CAcc.java, 124");
                }
            }
            
            // Government  ?
            else if (owner_type.equals("ID_GUV"))
                throw new Exception("Invalid product - CAcc.java, 115");
            
            // Company  ?
            else
            {
               rs=UTILS.DB.executeQuery("SELECT * "
                                        + "FROM com_prods "
                                       + "WHERE com_type='"+owner_type+"' "
                                         + "AND prod='"+prod+"'");
               
               // Has data
               if (!UTILS.DB.hasData(rs))
                   throw new Exception("Invalid product - CAcc.java, 127");
            }
            
            // Encode expl
            expl=UTILS.BASIC.base64_encode(expl);
           
            // Trans ID
            long tID=UTILS.BASIC.getID();
                
            // Add trans to trans pool
            if (amount<0)
            {
                // Balance
                double balance=this.getBalance(adr, prod);
                   
                // Funds
                if (balance<Math.abs(amount))
                   throw new Exception("Insufficient funds");
                    
                // Add to pool
	        UTILS.NETWORK.TRANS_POOL.addTrans(adr, 
		    		                  amount, 
		    		                  prod, 
		    		                  hash, 
		    		                  block);
            }
            
            // Insert into transactions
            UTILS.DB.executeUpdate("INSERT INTO trans "
                                             + "SET src='"+adr+"', "
                                                 + "amount='"+UTILS.FORMAT_8.format(amount)+"', "
                                                 + "cur='"+prod+"', "
                                                 + "hash='"+hash+"', "
                                                 + "expl='"+expl+"', "
                                                 + "invested='"+cost+"', "
                                                 + "block='"+block+"', "
                                                 + "block_hash='"+UTILS.NET_STAT.actual_block_hash+"', "
                                                 + "tstamp='"+UTILS.BASIC.tstamp()+"', "
                                                 + "status='ID_PENDING'");
            if (amount>0)
                return 0;
            
            // Amount
            double stoc=this.getBalance(adr, prod);
            
            // Percent
            double p=Math.abs(amount*100/stoc);
            
            // Load stocuri
            rs=UTILS.DB.executeQuery("SELECT * "
                                     + "FROM stocuri "
                                    + "WHERE adr='"+adr+"' "
                                      + "AND tip='"+prod+"'");
            
            if (UTILS.DB.hasData(rs))
            {
               // Next
               rs.next();
            
               // Cost
               return p*rs.getDouble("invested")/100;
            }
            else return 0;
        }
        
        
        public void newAssetTrans(String adr, 
                             String adr_assoc, 
                             double amount, 
                             String cur, 
                             String expl, 
                             String escrower, 
                             double cost,
                             String hash, 
                             long block) throws Exception
        {
            // ResultSet
            ResultSet rs;
            
            // Asset fee
            double fee=0;
            
            // Asset fee address
            String fee_adr="";
            
            // Market fee
            double mkt_fee=0;
            
            // Address valid
            if (!UTILS.BASIC.isAdr(adr)) 
                throw new Exception("Invalid address - CAcc.java, 107");
            
            // Address associated address ?
            if (!adr_assoc.equals("")
                && !adr_assoc.equals("none"))
              if (!UTILS.BASIC.isAdr(adr_assoc)) 
                throw new Exception("Invalid associated address - CAcc.java, 111");
            
            // Valid currency ?
            if (!UTILS.BASIC.isCur(cur)) 
                throw new Exception("Invalid currency - CAcc.java, 115");
            
            // Valid escrower ?
            if (!escrower.equals(""))
              if (!UTILS.BASIC.isAdr(escrower)) 
                throw new Exception("Invalid escrower - CAcc.java, 120");
            
            // Valid hash ?
            if (!UTILS.BASIC.isHash(hash)) 
                throw new Exception("Invalid hash - CAcc.java, 124");
            
            // Encode expl
            expl=UTILS.BASIC.base64_encode(expl);
           
            // Trans ID
            long tID=UTILS.BASIC.getID();
                
            // Add trans to trans pool
            if (amount<0)
            {
                // Balance
                double balance=this.getBalance(adr, cur);
                   
                // Funds
                if (balance<Math.abs(amount))
                   throw new Exception("Insufficient funds");
                    
                // Add to pool
	        UTILS.NETWORK.TRANS_POOL.addTrans(adr, 
		    		                  amount, 
		    		                  cur, 
		    		                  hash, 
		    		                  block);
            }
                
            // Credit transaction ?
            if (amount>0)
            {
                // Loads asset data
                    rs=UTILS.DB.executeQuery("SELECT * "
                                             + "FROM assets "
                                            + "WHERE symbol='"+cur+"'");
                  
                    // Next
                    rs.next();
                  
                    if (rs.getDouble("trans_fee")>0)
                    {
                        // Fee
                        fee=Math.abs(rs.getDouble("trans_fee")*amount/100);
                  
                        // Fee address
                        fee_adr=rs.getString("trans_fee_adr");
                    }
                
            }
            
            // Fee
            if (fee<0) 
                fee=0;
            
            // Insert into transactions
            UTILS.DB.executeUpdate("INSERT INTO trans "
                                             + "SET src='"+adr+"', "
                                                 + "amount='"+UTILS.FORMAT_8.format(amount)+"', "
                                                 + "cur='"+cur+"', "
                                                 + "hash='"+hash+"', "
                                                 + "expl='"+expl+"', "
                                                 + "escrower='"+escrower+"', "
                                                 + "block='"+block+"', "
                                                 + "block_hash='"+UTILS.NET_STAT.actual_block_hash+"', "
                                                 + "tstamp='"+UTILS.BASIC.tstamp()+"', "
                                                 + "status='ID_PENDING'");
            
            if (amount>0 && !adr.equals("default") && fee>0)
            {
                // Insert fee into transactions
                UTILS.DB.executeUpdate("INSERT INTO trans "
                                             + "SET src='"+adr+"', "
                                                 + "amount='"+UTILS.FORMAT_8.format(-fee)+"', "
                                                 + "cur='"+cur+"', "
                                                 + "hash='"+hash+"', "
                                                 + "expl='"+expl+"', "
                                                 + "block='"+block+"', "
                                                 + "block_hash='"+UTILS.NET_STAT.actual_block_hash+"', "
                                                 + "tstamp='"+UTILS.BASIC.tstamp()+"', "
                                                 + "status='ID_PENDING'");
           
            
                // Insert fee into transactions
                UTILS.DB.executeUpdate("INSERT INTO trans "
                                             + "SET src='"+fee_adr+"', "
                                                 + "amount='"+UTILS.FORMAT_8.format(fee)+"', "
                                                 + "cur='"+cur+"', "
                                                 + "hash='"+hash+"', "
                                                 + "expl='"+expl+"', "
                                                 + "block='"+block+"', "
                                                 + "block_hash='"+UTILS.NET_STAT.actual_block_hash+"', "
                                                 + "tstamp='"+UTILS.BASIC.tstamp()+"', "
                                                 + "status='ID_PENDING'");
            }
            
       
            
        }
        
        public void newCRCTrans(String adr, 
                                String adr_assoc, 
                                double amount, 
                                String cur, 
                                String expl, 
                                String escrower, 
                                double cost,
                                String hash, 
                                long block,
                                boolean taxed,
                                String tax,
                                String prod) throws Exception
        {
            // ResultSet
            ResultSet rs;
            
            // Address valid
            if (!UTILS.BASIC.isAdr(adr)) 
                throw new Exception("Invalid address - CAcc.java, 107");
            
            // Address associated address ?
            if (!adr_assoc.equals("")
               && !adr_assoc.equals("none"))
              if (!UTILS.BASIC.isAdr(adr_assoc)) 
                throw new Exception("Invalid associated address - CAcc.java, 111");
            
            // Valid currency ?
            if (!UTILS.BASIC.isCur(cur)) 
                throw new Exception("Invalid currency - CAcc.java, 115");
            
            // Valid escrower ?
            if (!escrower.equals(""))
              if (!UTILS.BASIC.isAdr(escrower)) 
                throw new Exception("Invalid escrower - CAcc.java, 120");
            
            // Valid hash ?
            if (!UTILS.BASIC.isHash(hash)) 
                throw new Exception("Invalid hash - CAcc.java, 124");
            
            // Encode expl
            expl=UTILS.BASIC.base64_encode(expl);
           
            // Trans ID
            long tID=UTILS.BASIC.getID();
                
            // Add trans to trans pool
            if (amount<0)
            {
                // Balance
                double balance=this.getBalance(adr, cur);
                   
                // Funds
                if (balance<Math.abs(amount))
                   throw new Exception("Insufficient funds");
                    
                // Add to pool
	        UTILS.NETWORK.TRANS_POOL.addTrans(adr, 
		    		                  amount, 
		    		                  cur, 
		    		                  hash, 
		    		                  block);
            }
            
            
              
            // Insert into transactions
            UTILS.DB.executeUpdate("INSERT INTO trans "
                                             + "SET src='"+adr+"', "
                                                 + "amount='"+UTILS.FORMAT_8.format(amount)+"', "
                                                 + "cur='"+cur+"', "
                                                 + "hash='"+hash+"', "
                                                 + "expl='"+expl+"', "
                                                 + "escrower='"+escrower+"', "
                                                 + "block='"+block+"', "
                                                 + "block_hash='"+UTILS.NET_STAT.actual_block_hash+"', "
                                                 + "tstamp='"+UTILS.BASIC.tstamp()+"', "
                                                 + "status='ID_PENDING'");
            
             if (taxed && amount>0)
                    this.bugTax(adr, 
                                tax, 
                                amount, 
                                prod, 
                                hash, 
                                block);
        }
        
        
        public void newTrans(String adr, 
                             String adr_assoc, 
                             double amount, 
                             String cur, 
                             String expl, 
                             String escrower, 
                             double cost,
                             String hash, 
                             long block,
                             boolean taxed,
                             String tax,
                             String prod) throws Exception
        {
            // CRC ?
            if (cur.equals("CRC"))
                this.newCRCTrans(adr, 
                                 adr_assoc, 
                                 amount, 
                                 cur, 
                                 expl, 
                                 escrower, 
                                 cost,
                                 hash, 
                                 block,
                                 taxed,
                                 tax,
                                 prod);
            
            // Asset ?
            else if (UTILS.BASIC.isAsset(cur))
                this.newAssetTrans(adr, 
                                   adr_assoc, 
                                   amount, 
                                   cur, 
                                   expl, 
                                   escrower, 
                                   cost,
                                   hash, 
                                   block);
            
            // Product ?
            else if (UTILS.BASIC.isProd(cur))
                this.newProdTrans(adr, 
                                  amount, 
                                  cur, 
                                  expl, 
                                  hash,
                                  cost,
                                  block);
                
        }
        
        public void newTransfer(String adr, 
                                String dest, 
                                double amount, 
                                String cur, 
                                String expl, 
                                String escrower, 
                                double cost,
                                String hash, 
                                long block,
                                boolean taxed,
                                String tax,
                                String prod) throws Exception
        {
            // CRC ?
            if (cur.equals("CRC"))
            {
                // Substract
                this.newCRCTrans(adr, 
                                 dest, 
                                 -amount, 
                                 cur, 
                                 expl, 
                                 escrower, 
                                 cost,
                                 hash, 
                                 block,
                                 false,
                                 "",
                                 "");
                
                // Deposit
                this.newCRCTrans(dest, 
                                 adr, 
                                 amount, 
                                 cur, 
                                 expl, 
                                 escrower, 
                                 cost,
                                 hash, 
                                 block,
                                 taxed,
                                 tax,
                                 prod);
            }
            
            // Asset ?
            else if (UTILS.BASIC.isAsset(cur))
            {
                // Substract
                this.newAssetTrans(adr, 
                                   dest, 
                                   -amount, 
                                   cur, 
                                   expl, 
                                   escrower, 
                                   cost,
                                   hash, 
                                   block);
                
                // Deposit
                this.newAssetTrans(dest, 
                                   adr, 
                                   amount, 
                                   cur, 
                                   expl, 
                                   escrower, 
                                   cost,
                                   hash, 
                                   block);
            }
            
            // Product ?
            else if (UTILS.BASIC.isProd(cur))
            {
                // Substract
                this.newProdTrans(adr, 
                                  -amount, 
                                  cur, 
                                  expl, 
                                  hash,
                                  cost,
                                  block);
                
                // Deposit
                this.newProdTrans(dest, 
                                  amount, 
                                  cur, 
                                  expl, 
                                  hash,
                                  cost,
                                  block);
            }   
        }
        
        // Get address owner
        public long getAdrUserID(String adr) throws Exception
        {
            // Address valid
            if (!UTILS.BASIC.isAdr(adr))
                throw new Exception ("Invalid address - CAcc.java, 278");
            
            // Default address ?
            if (adr.equals("default")) 
                return 0;
            
            // Load source
               ResultSet rs=UTILS.DB.executeQuery("SELECT * "
                                                  + "FROM my_adr "
                                                 + "WHERE adr='"+adr+"'"); 
               
               // None ?
               if (!UTILS.DB.hasData(rs)) return 0;
               
               // Next
               rs.next();
            
               // Return
               return rs.getLong("userID");
            
        }
        
        // Transfer assets
        public void doAssetTrans(String adr, 
                                 double amount, 
                                 String cur,
                                 long block) throws Exception
        {
            double balance;
            double new_balance;
            double invested=0;
            
            // Address valid
            if (!UTILS.BASIC.isAdr(adr))
                throw new Exception ("Invalid address - CAcc.java, 278");
            
            // Currency valid
            if (!UTILS.BASIC.isCur(cur))
                throw new Exception ("Invalid address - CAcc.java, 278");
                    
            // Load asset data
            ResultSet rs=UTILS.DB.executeQuery("SELECT * "
                                               + "FROM assets "
                                              + "WHERE symbol='"+cur+"'");
                     
            // Next
            rs.next();
                     
            // Load source
            rs=UTILS.DB.executeQuery("SELECT * "
                                     + "FROM assets_owners "
                                    + "WHERE owner='"+adr+"' "
                                     + " AND symbol='"+cur+"'");
		         
            // Address exist ?
            if (!UTILS.DB.hasData(rs))
            {
                // Insert address
                UTILS.DB.executeUpdate("INSERT INTO assets_owners "
                                             + "SET owner='"+adr+"', "
                                                 + "symbol='"+cur+"', "
                                                 + "qty='0'");
                         
                // New balance
                new_balance=amount;
            }
            else
            {
                // Next
                rs.next();
                     
                // Balance
		balance=rs.getDouble("qty");
		     
                // New balance
                new_balance=balance+amount;
            }
                      
            // Source balance update
            UTILS.DB.executeUpdate("UPDATE assets_owners "
		                    + "SET qty="+new_balance
                                  + " WHERE owner='"+adr+"' "
                                    + "AND symbol='"+cur+"'");
            
             UTILS.DB.executeUpdate("UPDATE web_users "
                                        + "SET unread_trans=unread_trans+1 "
                                      + "WHERE ID='"+getAdrUserID(adr)+"' ");
                 
        }
        
        // Transfer assets
        public void doProdTrans(String adr, 
                                 double amount, 
                                 String prod,
                                 double invested,
                                 long block,
                                 String hash) throws Exception
        {
            double balance;
            double new_balance;
            
            // New stoc ID
            long stocID=UTILS.BASIC.getFreeID(UTILS.BASIC.hashToLong(hash));
            
            // Already exist ?
            if (UTILS.BASIC.isID(stocID))
                throw new Exception("Invalid generated random number");
            
            // Address valid
            if (!UTILS.BASIC.isAdr(adr))
                throw new Exception ("Invalid address - CAcc.java, 278");
            
            // Currency valid
            if (!UTILS.BASIC.isProd(prod) && 
                !UTILS.BASIC.isLic(prod))
                throw new Exception ("Invalid product - CAcc.java, 278");
            
            // Energy ?
            if (prod.equals("ID_ENERGY"))
            {
                // Update energy
                UTILS.DB.executeUpdate("UPDATE adr "
                                        + "SET energy=energy+"+amount+" "
                                      + "WHERE adr='"+adr+"'");
                
                // Return 
                return;
            }
                     
            // Expire
            long expires=0;
            
            if (amount>=1)
            {
                   // Load product data
                   ResultSet rs=UTILS.DB.executeQuery("SELECT * "
                                                      + "FROM tipuri_produse "
                                                     + "WHERE prod='"+prod+"'");
                   
                   // Next
                   rs.next();
                   
                   // Expire
                   if (rs.getLong("expires")>0)
                       expires=block+rs.getLong("expires");
            }
                
            // Load stoc
            ResultSet rs=UTILS.DB.executeQuery("SELECT * "
                                               + "FROM stocuri "
                                              + "WHERE adr='"+adr+"' "
                                               + " AND tip='"+prod+"'");
              
            // Stoc exist ?
            if (!UTILS.DB.hasData(rs))
            {
                
                
                // Capacity
                long cap=0;
                
                // Insert item
                if (UTILS.BASIC.buySplit(adr, prod, amount))
                {
                    for (int a=1; a<=amount; a++)
                    {
                        long stID=UTILS.BASIC.getFreeID(stocID+a);
                        
                        UTILS.DB.executeUpdate("INSERT INTO stocuri "
                                                  + "SET adr='"+adr+"', "
                                                      + "tip='"+prod+"', "
                                                      + "qty='1', "
                                                      + "invested="+(invested/amount)+", "
                                                      + "expires='"+expires+"', "
                                                      + "stocID='"+stID+"', "
                                                      + "block='"+block+"'");
                    }
                }
                else
                     UTILS.DB.executeUpdate("INSERT INTO stocuri "
                                                  + "SET adr='"+adr+"', "
                                                      + "tip='"+prod+"', "
                                                      + "qty='0', "
                                                      + "invested="+invested+", "
                                                      + "expires='"+expires+"', "
                                                      + "cap='"+cap+"', "
                                                      + "stocID='"+stocID+"', "
                                                      + "block='"+block+"'");
                         
                   // New balance
                   new_balance=amount;
            }
            else
            {
                // Next
                rs.next();
                     
                // Balance
		balance=rs.getDouble("qty");
		     
                // New balance
                new_balance=balance+amount;
                
                
                // Buy split ?
                if (UTILS.BASIC.buySplit(adr, prod, amount))
                {
                   for (int a=1; a<=amount; a++)
                   {
                       // Unique ID
                       long stID=UTILS.BASIC.getFreeID(stocID+a);
                       
                       // Insert
                       UTILS.DB.executeUpdate("INSERT INTO stocuri "
                                                  + "SET adr='"+adr+"', "
                                                      + "tip='"+prod+"', "
                                                      + "qty='1', "
                                                      + "invested="+(invested/amount)+", "
                                                      + "expires='"+expires+"', "
                                                      + "stocID='"+stID+"', "
                                                      + "block='"+block+"'");
                    }
                }
            }
                      
            // Source balance update
            if (!UTILS.BASIC.buySplit(adr, prod, amount))
            {
                if (amount<0)
                {
                    // Invested
                    invested=rs.getDouble("invested");
                
                    // Stoc
                    double stoc=this.getBalance(adr, prod);
                    
                    // Percent
                    double p=amount*100/stoc;
                    
                    // Invested
                    invested=p*invested/100;
                }
                 
                // Update 
                UTILS.DB.executeUpdate("UPDATE stocuri "
		                              + "SET qty="+new_balance+", "
                                                  + "block='"+block+"', "
                                                  + "invested=invested+"+invested
                                           + " WHERE adr='"+adr+"' "
                                              + "AND tip='"+prod+"'");
            }
             
             if (UTILS.STATUS.status.equals("ID_ONLINE"))
                 UTILS.DB.executeUpdate("UPDATE web_users "
                                         + "SET unread_trans=unread_trans+1 "
                                       + "WHERE ID='"+getAdrUserID(adr)+"' ");
                 
            }
        
        public void doTrans(String adr, 
                            double amount, 
                            long block) throws Exception
        {
            double balance;
            double new_balance;
            
            // Adr
            if (!UTILS.BASIC.isAdr(adr)) 
                throw new Exception("Invalid address");
            
            // Load source
            ResultSet rs=UTILS.DB.executeQuery("SELECT * FROM adr WHERE adr='"+adr+"'");
		         
                     // Address exist ?
                     if (!UTILS.DB.hasData(rs))
                     {
                        UTILS.DB.executeUpdate("INSERT INTO adr  "
                                                     + "SET adr='"+adr+"', "
                                                         + "balance='0', "
                                                         + "block='"+block+"', "
                                                         + "created='"+block+"'");
                        
                        // New balance
                        new_balance=Double.parseDouble(UTILS.FORMAT_8.format(amount));
                     }
                     else
                     {
                        // Next
                        rs.next();
                     
                        // Balance
		        balance=rs.getDouble("balance");
		     
                        // New balance
                        new_balance=balance+amount;
                     
                        // Format
                        new_balance=Double.parseDouble(UTILS.FORMAT_8.format(new_balance));
                     }
                     
		     // Source balance update
		     UTILS.DB.executeUpdate("UPDATE adr "
		   		             + "SET balance="+UTILS.FORMAT_8.format(new_balance)+", "
		   		                 + "block='"+block+
                                           "' WHERE adr='"+adr+"'");
                     
                      UTILS.DB.executeUpdate("UPDATE web_users "
                                              + "SET unread_trans=unread_trans+1 "
                                            + "WHERE ID='"+getAdrUserID(adr)+"' ");
         }
        
        public long getEnergyProdBalance(String adr, String prod) throws Exception
        {
            // Valid address ?
            if (!UTILS.BASIC.isAdr(adr))
                throw new Exception("Invalid address");
            
            // Energy prod ?
            if (!UTILS.BASIC.isEnergyProd(prod))
                throw new Exception("Invalid product");
            
            // Load data
            ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(qty) AS total "
                                               + "FROM stocuri "
                                              + "WHERE adr='"+adr+"' "
                                                + "AND tip='"+prod+"'");
            
            // Next
            rs.next();
            
            // Return
            return rs.getLong("total");
        }
        
        public double getBalance(String adr, String cur) throws Exception
	{
           // Result Set
	   ResultSet rs=null;
           
           // Adr
            if (!UTILS.BASIC.isAdr(adr)) 
                throw new Exception("Invalid address");
            
            // Currency
            if (!UTILS.BASIC.isCur(cur) && 
                !UTILS.BASIC.isProd(cur)) 
                throw new Exception("Invalid currency");
            
            // CRC ?
           if (cur.equals("CRC"))
              rs=UTILS.DB.executeQuery("SELECT * "
                                       + "FROM adr "
                                      + "WHERE adr='"+adr+"'");
            
            // Asset ? 
           if (!cur.equals("CRC") && 
               !cur.contains("_") &&
                (cur.length()==5 || cur.length()==6))
                rs=UTILS.DB.executeQuery("SELECT * "
                                         + "FROM assets_owners "
                                        + "WHERE owner='"+adr+"' "
                                          + "AND symbol='"+cur+"'");
           
           // Prod ? 
           if (!cur.equals("CRC") && cur.indexOf("_")>0)
           {
              // Energy ?
              if (cur.equals("ID_ENERGY"))
              {
                  // Energy
                  rs=UTILS.DB.executeQuery("SELECT * "
                                           + "FROM adr "
                                          + "WHERE adr='"+adr+"'");
                  
                  // Next
                  rs.next();
                  
                  // Return
                  return rs.getDouble("energy");
              }
                  
              rs=UTILS.DB.executeQuery("SELECT * "
                                         + "FROM stocuri "
                                        + "WHERE adr='"+adr+"' "
                                          + "AND tip='"+cur+"'");
           
           }
           
            // Has data ?
            if (UTILS.DB.hasData(rs)==true)
            {
                // Next
                rs.next();
                   
                // Balance
                double balance;
                
                // Currency
                if (cur.equals("CRC"))
                   balance=rs.getDouble("balance");
                else
                   balance=rs.getDouble("qty");
                   
                  
                // Return
                return balance;
            } 
            else
            {
                // Return
                return 0;
            }
	}
        
    public double getBalance(String adr, String cur, CBlockPayload block) throws Exception
    {
        if (block==null)
            return UTILS.NETWORK.TRANS_POOL.getBalance(adr, cur);
        else
            return UTILS.ACC.getBalance(adr, cur);
    }
    
    
    public void bugTax(String adr, 
                       String tax, 
                       double income, 
                       String prod,
                       String hash, 
                       long block) throws Exception
    {
        // Product ?
        if (!prod.equals(""))
            if (!UTILS.BASIC.isProd(prod) || 
                adr.equals("default"))
                   return;
        
        // Country
        String cou=UTILS.BASIC.getAdrData(adr, "cou");
                             
        // Tax 
        double t=UTILS.BASIC.getTax(cou, tax, prod);
        
        // Value
        t=UTILS.BASIC.round(income*t/100, 4);
        
        // Pay
        if (t>0.0001)
        {
            // Insert into transactions
            UTILS.DB.executeUpdate("INSERT INTO trans "
                                             + "SET src='"+adr+"', "
                                                 + "amount='"+UTILS.FORMAT_8.format(-t)+"', "
                                                 + "cur='CRC', "
                                                 + "hash='"+hash+"', "
                                                 + "expl='WW91IHBhaWQgYSB0YXggdG8gc3RhdGUgYnVkZ2V0', "
                                                 + "invested='0', "
                                                 + "block='"+block+"', "
                                                 + "block_hash='"+UTILS.NET_STAT.actual_block_hash+"', "
                                                 + "tstamp='"+UTILS.BASIC.tstamp()+"', "
                                                 + "status='ID_PENDING'");
            
            // Insert into transactions
            UTILS.DB.executeUpdate("INSERT INTO trans "
                                             + "SET src='"+UTILS.BASIC.getCouAdr(cou)+"', "
                                                 + "amount='"+UTILS.FORMAT_8.format(t)+"', "
                                                 + "cur='CRC', "
                                                 + "hash='"+hash+"', "
                                                 + "expl='WW91IHJlY2VpdmVkIGEgdGF4', "
                                                 + "invested='0', "
                                                 + "block='"+block+"', "
                                                 + "block_hash='"+UTILS.NET_STAT.actual_block_hash+"', "
                                                 + "tstamp='"+UTILS.BASIC.tstamp()+"', "
                                                 + "status='ID_PENDING'");
        }
    }
    
    public void payBuyBonus(String adr, 
                            String prod, 
                            double qty,
                            long block,
                            String hash,
                            CBlockPayload block_payload) throws Exception
    {
        // Adr
        if (!UTILS.BASIC.isAdr(adr)) 
           throw new Exception("Invalid address, CAcc.java, 1138");
            
        // Product
        if (!UTILS.BASIC.isProd(prod))
            throw new Exception("Invalid product, CAcc.java, 1142");
        
        // Qty ?
        if (qty<0)
            throw new Exception("Invalid qty, CAcc.java, 1146");
        
        // Hash
        if (!UTILS.BASIC.isHash(hash))
            throw new Exception("Invalid hash, CAcc.java, 1150");
        
        // Address country
        String cou=UTILS.BASIC.getAdrData(adr, "cou");
        
        // Countru valid ?
        if (!UTILS.BASIC.isCountry(cou))
            throw new Exception("Invalid country, CAcc.java, 1157");
        
        // Load bous amount
        double bonus=UTILS.BASIC.getBonus(cou, "ID_BUY_BONUS", prod);
        
        // Bonus not zero ?
        if (bonus>0)
        {
            // Bonus amount
            bonus=bonus*qty;
        
            // Premium citizen or company ?
            if (UTILS.BASIC.getBudget(cou, "CRC", block_payload)>bonus)
                if (UTILS.BASIC.isCompanyAdr(adr) ||
                Long.parseLong(UTILS.BASIC.getAdrData(adr, "premium"))>0)
                UTILS.ACC.newTransfer(UTILS.BASIC.getCouAdr(cou), 
                                      adr,
                                      bonus, 
                                      "CRC", 
                                      "State budget bonus", 
                                      "", 
                                      0,
                                      UTILS.BASIC.hash(String.valueOf(block)), 
                                      block,
                                      false,
                                      "",
                                      "");  
        }
        
    }
}
