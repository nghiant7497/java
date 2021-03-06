package chainrepublik.kernel;

import java.sql.ResultSet;
import java.util.ArrayList;

public class CRewards 
{
    // Block
    long block;
    
    // Total
    double total_reward=0;
    
    public CRewards()
    {

               
    }
    
    public void reward(long block) throws Exception
    {
       // Block ?
       if (block%1440!=0) 
           return; 
       
       // Total reward
       this.total_reward=0;
       
       // Block
       this.block=block;
           
       // Energy Reward 10%
       this.generalReward("ID_ENERGY");
       
       // Political Influence Reward 10%
       this.generalReward("ID_POL_INF");
       
       // Political Endorsment Reward 5%
       this.generalReward("ID_POL_END");
       
       // Political Endorsment Reward 10%
       this.generalReward("ID_WAR_POINTS");
       
       // Refferers Reward 10%
       this.refReward();
       
       // Node Operators Reward 10%
       this.nodesReward();
       
       // Country Size Rewards 5%
       this.couSizeReward();
       
       // Country Energy Rewards5%
       this.couEnergyReward();
       
       // Military Units Rewards 5%
       this.milUnitsReward();
       
       // Political Parties Rewards 5%
       this.polPartiesReward();
       
       // Press Rewards 10%
       this.pressReward("ID_PRESS");
       
       // Comments Rewards 5%
       this.pressReward("ID_COM");
       
       // Take reward from default address
       UTILS.ACC.newTrans("default", 
                          "default",
                          -UTILS.BASIC.round(this.total_reward, 4), 
                          "CRC", 
                          "You have paid rewards ", 
                          "", 
                          0,
                          UTILS.BASIC.hash(String.valueOf(this.block)), 
                          this.block,
                          false,
                          "",
                          "");
       
       // Clear
       UTILS.ACC.clearTrans(UTILS.BASIC.hash(String.valueOf(this.block)), "ID_ALL", this.block);
    }
    
    // Energy Reward 10%
    public void generalReward(String type) throws Exception
    {
        ResultSet rs=null;
        
        // Minimum
        long min=0;
        
        // Pool
        long pool=0;
        
        // Column
        String col="energy";
        
        switch (type)
        {
            // Energy
            case "ID_ENERGY" :   rs=UTILS.DB.executeQuery("SELECT SUM(energy) AS total "
                                                          + "FROM adr "
                                                         + "WHERE energy>=1");
            
                                // Min to activate
                                min=1;
                                
                                // Column
                                col="energy";
                                
                                // Break
                                break;
                                
            // Political influence
            case "ID_POL_INF" :   rs=UTILS.DB.executeQuery("SELECT SUM(pol_inf) AS total "
                                                           + "FROM adr "
                                                          + "WHERE pol_inf>=1");
            
                                  // Min to activate
                                  min=1;
                                  
                                  // Column
                                  col="pol_inf";
                                
                                  // Break
                                  break;
                                
            // Political endorsment
            case "ID_POL_END" :   rs=UTILS.DB.executeQuery("SELECT SUM(pol_endorsed) AS total "
                                                           + "FROM adr "
                                                          + "WHERE pol_endorsed>=1");
            
                                  // Min to activate
                                  min=1;
                                  
                                  // Column
                                  col="pol_endorsed";
                                
                                  // Break
                                  break;
                                  
            // Political endorsment
            case "ID_WAR_POINTS" :   rs=UTILS.DB.executeQuery("SELECT SUM(war_points) AS total "
                                                              + "FROM adr "
                                                             + "WHERE war_points>=1");
            
                                  // Min to activate
                                  min=1;
                                  
                                  // Column
                                  col="war_points";
                                
                                  // Break
                                  break;
        }
        
        // Total pool
        pool=UTILS.BASIC.getRewardPool(type);
        
        // Next
        rs.next();
        
        // Total
        long total=rs.getLong("total");
        
        if (total>min)
        {
           // Total
           rs.next();
        
           // Load addressess
           rs=UTILS.DB.executeQuery("SELECT * "
                                    + "FROM adr "
                                   + "WHERE "+col+">=1");
        
           // Loop
           while (rs.next())
           {
               // Percent
               double p=rs.getDouble(col)*100/total;
            
              // Reward
              double amount=UTILS.BASIC.round(p*pool/100, 4);
               
               // Pay
               if (amount>0.0001)
                    this.payReward(rs.getString("adr"), 
                                   "ID_ADR", 
                                   0, 
                                   type, 
                                   amount, 
                                   rs.getDouble(col));
             
            }
        }
        
    }
    
     // Refferers Reward 10%
    public void refReward() throws Exception
    {
        // Load all refs energy
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(energy) AS total "
                                           + "FROM adr "
                                          + "WHERE ref_adr<>''");
        
        // Next
        rs.next();
        
        // Total energy
        long t_energy=Math.round(rs.getDouble("total"));
        
        // Total ?
        if (t_energy<1)
            return;
        
        // Pool
        long pool=UTILS.BASIC.getRewardPool("ID_REFS");
        
        // Load all referers
        rs=UTILS.DB.executeQuery("SELECT DISTINCT(ref_adr) "
                                 + "FROM adr "
                                + "WHERE ref_adr<>''");
        
        // Loop
        while (rs.next())
        {
            // Address
            String adr=rs.getString("ref_adr");
            
            // Load refs energy
            ResultSet rse=UTILS.DB.executeQuery("SELECT SUM(energy) AS total "
                                                + "FROM adr "
                                               + "WHERE ref_adr='"+adr+"'");
            
            // Next
            rse.next();
            
            // Total bigger than 10 ?
            if (rse.getDouble("total")>10)
            {
                // Ref energy
                double ref_energy=rse.getDouble("total");
                
                // Percent
                double p=ref_energy*100/t_energy;
                
                // Reward
                double amount=UTILS.BASIC.round(p*pool/100, 4);
                
                // Pay reward
                this.payReward(adr, 
                               "", 
                               0, 
                               "ID_REFS", 
                               amount, 
                               ref_energy);
            }
        }
    }
    
    // Node Operators Reward 10%
    public void nodesReward() throws Exception
    {
        // Load all refs energy
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(energy) AS total "
                                           + "FROM adr "
                                          + "WHERE node_adr<>''");
        
        // Next
        rs.next();
        
        // Total energy
        long t_energy=Math.round(rs.getDouble("total"));
        
        // Min energy ?
        if (t_energy<100)
            return;
        
        // Pool
        long pool=UTILS.BASIC.getRewardPool("ID_NODES");
        
        // Load all referers
        rs=UTILS.DB.executeQuery("SELECT DISTINCT(node_adr) "
                                 + "FROM adr "
                                + "WHERE node_adr<>''");
        
        // Loop
        while (rs.next())
        {
            // Address
            String adr=rs.getString("node_adr");
            
            // Load refs energy
            ResultSet rse=UTILS.DB.executeQuery("SELECT SUM(energy) AS total "
                                                + "FROM adr "
                                               + "WHERE node_adr='"+adr+"'");
            
            // Next
            rse.next();
            
            // Total bigger than 10 ?
            if (rse.getDouble("total")>10)
            {
                // Ref energy
                double node_energy=rse.getDouble("total");
                
                // Percent
                double p=node_energy*100/t_energy;
                
                // Reward
                double amount=UTILS.BASIC.round(p*pool/100, 4);
                
                // Pay reward
                this.payReward(adr, 
                               "", 
                               0, 
                               "ID_NODES", 
                               amount, 
                               node_energy);
            }
            
        }
    }
    
    public double getVotes(String target_type, long targetID) throws Exception
    {
        // Load upvotes
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(power) AS total "
                                           + "FROM votes "
                                          + "WHERE target_type='"+target_type+"' "
                                            + "AND targetID='"+targetID+"' "
                                            + "AND type='ID_UP'");
        
        // Next
        rs.next();
        
        // Upvotes
        double upvotes=rs.getDouble("total");
        
        // Load upvotes
        rs=UTILS.DB.executeQuery("SELECT SUM(power) AS total "
                                 + "FROM votes "
                                + "WHERE target_type='"+target_type+"' "
                                  + "AND targetID='"+targetID+"' "
                                  + "AND type='ID_DOWN'");
        
        // Next
        rs.next();
        
        // Upvotes
        double downvotes=rs.getDouble("total");
        
        // Return
        return (upvotes-downvotes);
    }
    
    // Pay voters
    public void payVoters(String target_type, long targetID, double pool) throws Exception
    {
        // Total votes power
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(power) AS total "
                                           + "FROM votes "
                                          + "WHERE target_type='"+target_type+"' "
                                            + "AND targetID='"+targetID+"'");
        
        // Next
        rs.next();
        
        // Total
        double total=rs.getDouble("total");
        
        // Total
        if (total<1)
            return;
        
        // Loop voters
        rs=UTILS.DB.executeQuery("SELECT * "
                                 + "FROM votes "
                                + "WHERE target_type='"+target_type+"' "
                                  + "AND targetID='"+targetID+"'");
        
        // Next
        while (rs.next())
        {
            // Percent 
            double p=rs.getDouble("power")*100/total;
            
            // Reward
            double amount=UTILS.BASIC.round(p*pool/100, 4);
            
            // Pay reward
            this.payReward(rs.getString("adr"), 
                           "ID_VOTE", 
                           0, 
                           "ID_VOTERS", 
                           amount, 
                           rs.getDouble("power"));
            
        }
    }
    
    // Press Rewards 10%
    public void pressReward(String type) throws Exception
    {
        // Pool
        long pool=0;
        
        // Target type
        String target="";
        
        // Pool
        if (type.equals("ID_PRESS"))
        {
            pool=UTILS.BASIC.getRewardPool("ID_PRESS");
            target="ID_TWEET";
        }
        else
        {
            pool=UTILS.BASIC.getRewardPool("ID_COMM");
            target="ID_COM";
        }
        
        // Total votes
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(power) AS total "
                                           + "FROM votes "
                                          + "WHERE target_type='"+target+"'");
        
        // Next
        rs.next();
        
        // Total votes
        long total_power=rs.getLong("total");
        
        // Total power
        if (total_power<1)
            return;
        
        // Load distinct articles
        rs=UTILS.DB.executeQuery("SELECT DISTINCT(targetID) "
                                 + "FROM votes "
                                + "WHERE target_type='"+target+"'");
        
        // Loop
        while (rs.next())
        {
             ResultSet rst=null;
             
            // Load target details
            if (type.equals("ID_PRESS"))
                rst=UTILS.DB.executeQuery("SELECT * "
                                          + "FROM tweets "
                                         + "WHERE tweetID='"+rs.getLong("targetID")+"'");
            else
                 rst=UTILS.DB.executeQuery("SELECT * "
                                          + "FROM comments "
                                         + "WHERE comID='"+rs.getLong("targetID")+"'");
            
            // Next
            rst.next();
            
            // Address
            String adr=rst.getString("adr");
            
            // Target ID
            long targetID=rs.getLong("targetID");
            
            // Ref energy
            double press_points=this.getVotes(target, targetID);
                
            // Percent
            double p=press_points*100/total_power;
                
            // Reward
            double amount=UTILS.BASIC.round(p*pool/100, 4);
                
            // Pay reward
            this.payReward(adr, 
                           type, 
                           targetID, 
                           type, 
                           amount/2, 
                           press_points);
            
            // Pay voters
            this.payVoters(target, targetID, amount/2);
        }
            
    }
    
    
    // Country Size Rewards 5%
    public void couSizeReward() throws Exception
    {
        // Adr
        String adr="";
        
        // Total area
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(area) AS total "
                                           + "FROM countries");
        
        // Next
        rs.next();
        
        // Total area
        long total_area=rs.getLong("total");
        
        // Pool
        long pool=UTILS.BASIC.getRewardPool("ID_COU_AREA");
        
        // Parse countries
        rs=UTILS.DB.executeQuery("SELECT * "
                                 + "FROM countries "
                                + "WHERE area>0");
        
        // Loop
        while (rs.next())
        {
            // Country area
            long area=rs.getLong("area");
                    
            // Percent
            double p=UTILS.BASIC.round(area*100.0/total_area, 4);
            
            // Reward
            double amount=UTILS.BASIC.round(p*pool/100, 4);
            
            // Occupied ?
            if (!rs.getString("occupied").equals("") && 
                !rs.getString("occupied").equals(rs.getString("code")))
            {
                ResultSet rsc=UTILS.DB.executeQuery("SELECT * "
                                                    + "FROM countries "
                                                   + "WHERE code='"+rs.getString("occupied")+"'");
                
                // Next
                rsc.next();
                
                // Adr
                adr=rsc.getString("adr");
            }
            else adr=rs.getString("adr");
            
            // Pay reward
            this.payReward(adr, 
                           "", 
                           0, 
                           "ID_COU_AREA", 
                           amount, 
                           rs.getLong("area"));
                
        }
    }
    
    // Country Energy Rewards5%
    public void couEnergyReward() throws Exception
    {
        // Adr
        String adr; 
        
        // Pool
        long pool=UTILS.BASIC.getRewardPool("ID_COU_ENERGY");
        
        // Total energy
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(energy) AS total "
                                           + "FROM adr "
                                          + "WHERE cou<>''");
                                           
        // Next
        rs.next();
        
        // Total
        long total_energy=Math.round(rs.getDouble("total"));
        
        // Total energy
        if (total_energy<1)
            return;
        
        // Load countries
        rs=UTILS.DB.executeQuery("SELECT * "
                                 + "FROM countries");
        
        // Loop
        while (rs.next())
        {
            // Country energy
            ResultSet rsu=UTILS.DB.executeQuery("SELECT SUM(energy) AS total "
                                                + "FROM adr "
                                               + "WHERE cou='"+rs.getString("code")+"'");
            
            // Next
            rsu.next();
            
            // Country energy
            long ce=Math.round(rsu.getDouble("total"));
            
            if (ce>10)
            {
                // Percent
                double p=ce*100/total_energy;
                
                // Amount
                double amount=UTILS.BASIC.round(p*pool/100, 4);
                
                // Occupied ?
                if (!rs.getString("occupied").equals("") &&
                    !rs.getString("occupied").equals(rs.getString("code")))
                {
                    ResultSet rsc=UTILS.DB.executeQuery("SELECT * "
                                                    + "FROM countries "
                                                   + "WHERE code='"+rs.getString("occupied")+"'");
                
                    // Next
                    rsc.next();
                
                    // Adr
                   adr=rsc.getString("adr");
                }
                else adr=rs.getString("adr");
                
                // Pay reward
                this.payReward(adr, 
                               "", 
                               0, 
                               "ID_COU_ENERGY", 
                               amount, 
                               ce);
                
            }
        }
    }
    
    // Military Units Rewards 5%
    public void milUnitsReward() throws Exception
    {
        // Pool
        long pool=UTILS.BASIC.getRewardPool("ID_MIL_UNITS");
        
        // Load war points total
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(war_points) AS total "
                                           + "FROM adr "
                                          + "WHERE mil_unit>0");
        
        // Next
        rs.next();
        
        // Load
        long total_points=rs.getLong("total");
        
        // Minimum war points ?
        if (total_points<1)
            return;
        
        // Load military units
        rs=UTILS.DB.executeQuery("SELECT * "
                                 + "FROM orgs "
                                 + "WHERE type='ID_MIL_UNIT'");
        
        // Loop
        while (rs.next())
        {
            // Load unit points
            ResultSet rsu=UTILS.DB.executeQuery("SELECT SUM(war_points) AS total "
                                                + "FROM adr "
                                               + "WHERE mil_unit='"+rs.getLong("orgID")+"'");
            
            // Next
            rsu.next();
            
            // Unit points
            long points=rsu.getLong("total");
            
            // Percent
            double p=points*100/total_points;
            
            // Amount
            double amount=UTILS.BASIC.round(p*pool/100, 4);
            
            // Pay reward
            this.payReward(rs.getString("adr"), 
                           "", 
                           0, 
                           "ID_MIL_UNITS", 
                           amount, 
                           points);
        }
    }
    
    // Political Parties Rewards 5%
    public void polPartiesReward() throws Exception
    {
        // Pool
        long pool=UTILS.BASIC.getRewardPool("ID_POL_PARTY");
        
        // Load war points total
        ResultSet rs=UTILS.DB.executeQuery("SELECT SUM(pol_inf) AS total "
                                           + "FROM adr "
                                          + "WHERE pol_party>0");
        
        // Next
        rs.next();
        
        // Load
        long total_points=rs.getLong("total");
        
        
        // Minimum points ?
        if (total_points<1)
            return;
        
        // Load military units
        rs=UTILS.DB.executeQuery("SELECT * "
                                 + "FROM orgs "
                                 + "WHERE type='ID_POL_PARTY'");
        
        // Loop
        while (rs.next())
        {
            // Load unit points
            ResultSet rsu=UTILS.DB.executeQuery("SELECT SUM(pol_inf) AS total "
                                                + "FROM adr "
                                               + "WHERE pol_party='"+rs.getLong("orgID")+"'");
            
            // Next
            rsu.next();
            
            // Unit points
            long points=rsu.getLong("total");
            
            // Percent
            double p=points*100/total_points;
            
            // Amount
            double amount=UTILS.BASIC.round(p*pool/100, 4);
            
            // Pay reward
            this.payReward(rs.getString("adr"), 
                           "", 
                           0, 
                           "ID_POL_PARTY", 
                           amount, 
                           points);
        }
    }
    
    public void payReward(String adr, 
                          String target_type, 
                          long targetID, 
                          String reward, 
                          double amount, 
                          double par) throws Exception
    {
        // Name
        String name="";
        
        // Min reward ?
        if (amount<0.0001)
            return;
        
        // Reward name
        switch (reward)
        {
            // Energy reward
            case "ID_ENERGY" : name="energy reward"; break;
            
            // Pol nfluence
            case "ID_POL_INF" : name="political influence reward"; break;
            
            // Pol endorsment
            case "ID_POL_END" : name="political endorsment reward"; break;
            
            // Military reward
            case "ID_WAR_POINTS" : name="military rank reward"; break;
            
            // Referers
            case "ID_REF" : name="affiliates reward"; break;
            
            // Noodes reward
            case "ID_NODES" : name="nodes reward"; break;
            
            // Articles reward
            case "ID_TWEET" : name="articles reward"; break;
            
            // Commenters reward
            case "ID_COM" : name="commenters reward"; break;
            
            // Voters reward
            case "ID_VOTERS" : name="voters reward"; break;
            
            // Country area reward
            case "ID_COU_AREA" : name="country area reward"; break;
            
            // Country energy reward
            case "ID_COU_ENERGY" : name="country energy reward"; break;
            
            // Military Units reward
            case "ID_MIL_UNITS" : name="military units reward"; break;
            
            // Political parties reward
            case "ID_POL_PARTY" : name="political parties reward"; break;
        }
        
        // Taxed ?
        boolean taxed=true;
        
        // Tax ?
        if (reward.equals("ID_POL_PARTY") || 
            reward.equals("ID_MIL_UNITS") || 
            reward.equals("ID_COU_ENERGY") || 
            reward.equals("ID_COU_AREA") || 
            reward.equals("ID_NODES"))
        taxed=false;
           else 
        taxed=true;
        
        // Taxed ?
        if (taxed && UTILS.BASIC.getAdrData(adr, "cou").equals(""))
           return;
        
        // Payment
        UTILS.ACC.newTrans(adr, 
                           "default",
                           amount, 
                           "CRC", 
                           "You have received "+name, 
                           "", 
                           0,
                           UTILS.BASIC.hash(String.valueOf(this.block)), 
                           this.block,
                           taxed,
                           "ID_REWARDS_TAX",
                           "");
        
        // Log
        UTILS.DB.executeUpdate("INSERT INTO rewards "
                                + "SET adr='"+adr+"', "
                                    + "target_type='"+target_type+"', "
                                    + "targetID='"+targetID+"', "
                                    + "reward='"+reward+"', "
                                    + "amount='"+amount+"', "
                                    + "block='"+this.block+"', "
                                    + "par_f='"+par+"'");
        
        // Update totals
        this.total_reward=this.total_reward+amount;
    }
    
}